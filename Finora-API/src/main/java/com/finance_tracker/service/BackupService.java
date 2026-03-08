package com.finance_tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finance_tracker.dto.BackupMetadataDTO;
import com.finance_tracker.dto.BackupPayloadDTO;
import com.finance_tracker.exception.BackupException;
import com.finance_tracker.exception.BusinessLogicException;
import com.finance_tracker.model.*;
import com.finance_tracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Orchestrates backup export and import operations.
 *
 * <h3>Export flow:</h3>
 * <ol>
 *   <li>Snapshot all user-owned tables (expenses, investments, loans, SIPs)</li>
 *   <li>Snapshot all user's ledger events (ordered by sequence)</li>
 *   <li>Compute the ledger root hash (hash of the last event in the chain)</li>
 *   <li>Build metadata with counts and timestamp</li>
 *   <li>Serialize to JSON and encrypt with user-supplied password</li>
 * </ol>
 *
 * <h3>Import flow:</h3>
 * <ol>
 *   <li>Decrypt the uploaded file with the user-supplied password</li>
 *   <li>Deserialize the JSON payload</li>
 *   <li>Validate the backup belongs to the requesting user</li>
 *   <li>Verify the embedded ledger chain integrity</li>
 *   <li>Clear existing user data and replace with backup data</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final String BACKUP_FORMAT_VERSION = "1.0";

    private final CryptoService cryptoService;
    private final LedgerService ledgerService;
    private final ExpenseRepository expenseRepository;
    private final InvestmentRepository investmentRepository;
    private final LoanRepository loanRepository;
    private final SipRepository sipRepository;
    private final LedgerEventRepository ledgerEventRepository;
    private final UserRepository userRepository;

    private final ObjectMapper backupMapper = createBackupMapper();

    /**
     * Exports all data for the authenticated user as an encrypted byte array.
     *
     * @param userId   the authenticated user's ID
     * @param password the encryption password chosen by the user
     * @return encrypted backup bytes (ready to be sent as a file download)
     */
    @Transactional(readOnly = true)
    public byte[] exportBackup(Long userId, String password) {
        logger.info("Starting backup export for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("User not found"));

        // Snapshot all user data
        List<Expense> expenses = expenseRepository.findByUserId(userId);
        List<Investment> investments = investmentRepository.findByUserId(userId);
        List<Loan> loans = loanRepository.findByUserId(userId);
        List<Sip> sips = sipRepository.findByUserId(userId);
        List<LedgerEvent> ledgerEvents = ledgerEventRepository
                .findByUserIdOrderByEventSequenceAsc(String.valueOf(userId));

        // Compute ledger root hash (hash of the last event in the chain)
        String ledgerRootHash = ledgerEvents.isEmpty()
                ? null
                : ledgerEvents.get(ledgerEvents.size() - 1).getHash();

        // Build metadata
        BackupMetadataDTO metadata = BackupMetadataDTO.builder()
                .version(BACKUP_FORMAT_VERSION)
                .exportTimestamp(OffsetDateTime.now())
                .userId(userId)
                .username(user.getUsername())
                .ledgerRootHash(ledgerRootHash)
                .ledgerEventCount(ledgerEvents.size())
                .expenseCount(expenses.size())
                .investmentCount(investments.size())
                .loanCount(loans.size())
                .sipCount(sips.size())
                .build();

        // Assemble payload
        BackupPayloadDTO payload = BackupPayloadDTO.builder()
                .metadata(metadata)
                .expenses(expenses)
                .investments(investments)
                .loans(loans)
                .sips(sips)
                .ledgerEvents(ledgerEvents)
                .build();

        // Serialize and encrypt
        try {
            byte[] jsonBytes = backupMapper.writeValueAsBytes(payload);
            byte[] encrypted = cryptoService.encrypt(jsonBytes, password);

            logger.info("Backup export completed for user {} — {} expenses, {} investments, {} loans, {} SIPs, {} ledger events",
                    userId, expenses.size(), investments.size(), loans.size(), sips.size(), ledgerEvents.size());

            return encrypted;
        } catch (BackupException e) {
            throw e;
        } catch (Exception e) {
            throw new BackupException("Failed to serialize backup payload", e);
        }
    }

    /**
     * Imports an encrypted backup file, replacing all existing user data.
     *
     * @param userId        the authenticated user's ID
     * @param encryptedData the encrypted backup bytes uploaded by the user
     * @param password      the decryption password
     * @return metadata of the imported backup
     */
    @Transactional
    public BackupMetadataDTO importBackup(Long userId, byte[] encryptedData, String password) {
        logger.info("Starting backup import for user {}", userId);

        // Decrypt
        byte[] jsonBytes = cryptoService.decrypt(encryptedData, password);

        // Deserialize
        BackupPayloadDTO payload;
        try {
            payload = backupMapper.readValue(jsonBytes, BackupPayloadDTO.class);
        } catch (Exception e) {
            throw new BackupException("Failed to parse backup file — it may be corrupted", e);
        }

        // Validate metadata
        BackupMetadataDTO metadata = payload.getMetadata();
        if (metadata == null) {
            throw new BackupException("Backup file is missing metadata");
        }

        if (!BACKUP_FORMAT_VERSION.equals(metadata.getVersion())) {
            throw new BackupException(
                    String.format("Unsupported backup version: %s (expected %s)", metadata.getVersion(), BACKUP_FORMAT_VERSION));
        }

        // Ownership check: backup must belong to the same user
        if (!userId.equals(metadata.getUserId())) {
            throw new BackupException("This backup belongs to a different user and cannot be imported");
        }

        // Verify embedded ledger chain integrity before importing
        verifyBackupLedgerIntegrity(payload);

        // Clear existing user data (order matters for FK constraints — ledger first since it has no FKs)
        ledgerEventRepository.deleteAll(
                ledgerEventRepository.findByUserIdOrderByEventSequenceAsc(String.valueOf(userId)));

        List<Expense> existingExpenses = expenseRepository.findByUserId(userId);
        if (!existingExpenses.isEmpty()) expenseRepository.deleteAll(existingExpenses);

        List<Investment> existingInvestments = investmentRepository.findByUserId(userId);
        if (!existingInvestments.isEmpty()) investmentRepository.deleteAll(existingInvestments);

        List<Loan> existingLoans = loanRepository.findByUserId(userId);
        if (!existingLoans.isEmpty()) loanRepository.deleteAll(existingLoans);

        List<Sip> existingSips = sipRepository.findByUserId(userId);
        if (!existingSips.isEmpty()) sipRepository.deleteAll(existingSips);

        // Flush deletes before inserting
        expenseRepository.flush();
        investmentRepository.flush();
        loanRepository.flush();
        sipRepository.flush();
        ledgerEventRepository.flush();

        // Import backup data — reset auto-generated IDs so JPA treats them as new inserts
        importExpenses(payload.getExpenses(), userId);
        importInvestments(payload.getInvestments(), userId);
        importLoans(payload.getLoans(), userId);
        importSips(payload.getSips(), userId);
        importLedgerEvents(payload.getLedgerEvents(), String.valueOf(userId));

        logger.info("Backup import completed for user {} — {} expenses, {} investments, {} loans, {} SIPs, {} ledger events",
                userId,
                payload.getExpenses().size(),
                payload.getInvestments().size(),
                payload.getLoans().size(),
                payload.getSips().size(),
                payload.getLedgerEvents().size());

        return metadata;
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Verifies the hash chain in the backup's ledger events matches the metadata root hash.
     */
    private void verifyBackupLedgerIntegrity(BackupPayloadDTO payload) {
        List<LedgerEvent> events = payload.getLedgerEvents();
        if (events == null || events.isEmpty()) {
            return; // No ledger events to verify
        }

        // Verify the chain's last hash matches the root hash in metadata
        String lastHash = events.get(events.size() - 1).getHash();
        String expectedRootHash = payload.getMetadata().getLedgerRootHash();

        if (expectedRootHash != null && !expectedRootHash.equals(lastHash)) {
            throw new BackupException("Ledger root hash mismatch — backup may have been tampered with");
        }

        // Verify prev_hash linkage
        String prevHash = null;
        for (LedgerEvent event : events) {
            String eventPrevHash = event.getPrevHash();
            // First event's prevHash should be null, subsequent events should link to previous
            if (prevHash != null && !prevHash.equals(eventPrevHash)) {
                throw new BackupException(
                        String.format("Ledger chain broken at event %s — backup data integrity compromised",
                                event.getEventUuid()));
            }
            prevHash = event.getHash();
        }
    }

    private void importExpenses(List<Expense> expenses, Long userId) {
        if (expenses == null) return;
        for (Expense expense : expenses) {
            expense.setId(null); // Let DB generate new ID
            expense.setUserId(userId);
        }
        expenseRepository.saveAll(expenses);
    }

    private void importInvestments(List<Investment> investments, Long userId) {
        if (investments == null) return;
        for (Investment investment : investments) {
            investment.setId(null);
            investment.setUserId(userId);
        }
        investmentRepository.saveAll(investments);
    }

    private void importLoans(List<Loan> loans, Long userId) {
        if (loans == null) return;
        for (Loan loan : loans) {
            loan.setId(null);
            loan.setUserId(userId);
        }
        loanRepository.saveAll(loans);
    }

    private void importSips(List<Sip> sips, Long userId) {
        if (sips == null) return;
        for (Sip sip : sips) {
            sip.setId(null);
            sip.setUserId(userId);
        }
        sipRepository.saveAll(sips);
    }

    private void importLedgerEvents(List<LedgerEvent> events, String userId) {
        if (events == null) return;
        for (LedgerEvent event : events) {
            // LedgerEvent uses UUID as PK (not auto-generated), so we keep the original IDs
            // to preserve hash chain integrity. But we ensure userId matches.
            event.setUserId(userId);
        }
        ledgerEventRepository.saveAll(events);
    }

    private static ObjectMapper createBackupMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return mapper;
    }
}
