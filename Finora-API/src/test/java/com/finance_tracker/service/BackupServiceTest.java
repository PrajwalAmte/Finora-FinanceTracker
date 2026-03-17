package com.finance_tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finance_tracker.dto.BackupMetadataDTO;
import com.finance_tracker.dto.BackupPayloadDTO;
import com.finance_tracker.exception.BackupException;
import com.finance_tracker.exception.BusinessLogicException;
import com.finance_tracker.model.LedgerEvent;
import com.finance_tracker.model.User;
import com.finance_tracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackupServiceTest {

    @Mock private CryptoService cryptoService;
    @Mock private LedgerService ledgerService;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private InvestmentRepository investmentRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private SipRepository sipRepository;
    @Mock private LedgerEventRepository ledgerEventRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private BackupService backupService;

    private static final Long USER_ID = 1L;
    private static final String PASSWORD = "test-password";

    // Mirror the exact mapper configuration from BackupService.createBackupMapper()
    private static final ObjectMapper TEST_MAPPER = buildTestMapper();

    private static ObjectMapper buildTestMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return mapper;
    }

    private User makeUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("testuser");
        return user;
    }

    private LedgerEvent makeLedgerEvent(String hash, String prevHash) {
        LedgerEvent event = new LedgerEvent();
        event.setId(UUID.randomUUID());
        event.setEventUuid(UUID.randomUUID());
        event.setHash(hash);
        event.setPrevHash(prevHash);
        event.setUserId(String.valueOf(USER_ID));
        event.setEntityType("EXPENSE");
        event.setEntityId("1");
        event.setActionType("CREATE");
        event.setEventTimestamp(OffsetDateTime.now());
        event.setEventVersion(1);
        return event;
    }

    // -------------------------------------------------------------------------
    // exportBackup
    // -------------------------------------------------------------------------

    @Test
    void exportBackup_success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));
        when(expenseRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(investmentRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(loanRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(sipRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(ledgerEventRepository.findByUserIdOrderByEventSequenceAsc(String.valueOf(USER_ID)))
                .thenReturn(List.of());
        byte[] fakeEncrypted = new byte[]{10, 20, 30};
        when(cryptoService.encrypt(any(), eq(PASSWORD))).thenReturn(fakeEncrypted);

        byte[] result = backupService.exportBackup(USER_ID, PASSWORD);

        assertThat(result).isNotNull().isEqualTo(fakeEncrypted);
    }

    @Test
    void exportBackup_userNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> backupService.exportBackup(USER_ID, PASSWORD))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void exportBackup_serializationFailure() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));
        when(expenseRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(investmentRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(loanRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(sipRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(ledgerEventRepository.findByUserIdOrderByEventSequenceAsc(String.valueOf(USER_ID)))
                .thenReturn(List.of());
        when(cryptoService.encrypt(any(), eq(PASSWORD)))
                .thenThrow(new RuntimeException("Crypto failure"));

        assertThatThrownBy(() -> backupService.exportBackup(USER_ID, PASSWORD))
                .isInstanceOf(BackupException.class);
    }

    // -------------------------------------------------------------------------
    // importBackup
    // -------------------------------------------------------------------------

    @Test
    void importBackup_success() throws Exception {
        BackupMetadataDTO metadata = BackupMetadataDTO.builder()
                .version("1.0")
                .exportTimestamp(OffsetDateTime.now())
                .userId(USER_ID)
                .username("testuser")
                .ledgerRootHash(null)
                .ledgerEventCount(0)
                .expenseCount(0)
                .investmentCount(0)
                .loanCount(0)
                .sipCount(0)
                .build();

        BackupPayloadDTO payload = BackupPayloadDTO.builder()
                .metadata(metadata)
                .expenses(List.of())
                .investments(List.of())
                .loans(List.of())
                .sips(List.of())
                .ledgerEvents(List.of())
                .build();

        byte[] jsonBytes = TEST_MAPPER.writeValueAsBytes(payload);
        when(cryptoService.decrypt(any(), eq(PASSWORD))).thenReturn(jsonBytes);

        when(ledgerEventRepository.findByUserIdOrderByEventSequenceAsc(String.valueOf(USER_ID)))
                .thenReturn(List.of());
        when(expenseRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(investmentRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(loanRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(sipRepository.findByUserId(USER_ID)).thenReturn(List.of());

        BackupMetadataDTO result = backupService.importBackup(USER_ID, new byte[0], PASSWORD);

        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo("1.0");
        assertThat(result.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void importBackup_corruptedJson() {
        when(cryptoService.decrypt(any(), eq(PASSWORD))).thenReturn("INVALID".getBytes());

        assertThatThrownBy(() -> backupService.importBackup(USER_ID, new byte[0], PASSWORD))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("corrupted");
    }

    @Test
    void importBackup_missingMetadata() throws Exception {
        BackupPayloadDTO payload = BackupPayloadDTO.builder()
                .metadata(null)
                .expenses(List.of())
                .investments(List.of())
                .loans(List.of())
                .sips(List.of())
                .ledgerEvents(List.of())
                .build();

        byte[] jsonBytes = TEST_MAPPER.writeValueAsBytes(payload);
        when(cryptoService.decrypt(any(), eq(PASSWORD))).thenReturn(jsonBytes);

        assertThatThrownBy(() -> backupService.importBackup(USER_ID, new byte[0], PASSWORD))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("metadata");
    }

    @Test
    void importBackup_wrongVersion() throws Exception {
        BackupMetadataDTO metadata = BackupMetadataDTO.builder()
                .version("2.0")
                .exportTimestamp(OffsetDateTime.now())
                .userId(USER_ID)
                .username("testuser")
                .build();

        BackupPayloadDTO payload = BackupPayloadDTO.builder()
                .metadata(metadata)
                .expenses(List.of())
                .investments(List.of())
                .loans(List.of())
                .sips(List.of())
                .ledgerEvents(List.of())
                .build();

        byte[] jsonBytes = TEST_MAPPER.writeValueAsBytes(payload);
        when(cryptoService.decrypt(any(), eq(PASSWORD))).thenReturn(jsonBytes);

        assertThatThrownBy(() -> backupService.importBackup(USER_ID, new byte[0], PASSWORD))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("2.0");
    }

    @Test
    void importBackup_wrongUser() throws Exception {
        BackupMetadataDTO metadata = BackupMetadataDTO.builder()
                .version("1.0")
                .exportTimestamp(OffsetDateTime.now())
                .userId(999L)
                .username("otheruser")
                .build();

        BackupPayloadDTO payload = BackupPayloadDTO.builder()
                .metadata(metadata)
                .expenses(List.of())
                .investments(List.of())
                .loans(List.of())
                .sips(List.of())
                .ledgerEvents(List.of())
                .build();

        byte[] jsonBytes = TEST_MAPPER.writeValueAsBytes(payload);
        when(cryptoService.decrypt(any(), eq(PASSWORD))).thenReturn(jsonBytes);

        assertThatThrownBy(() -> backupService.importBackup(USER_ID, new byte[0], PASSWORD))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("different user");
    }

    @Test
    void importBackup_ledgerHashMismatch() throws Exception {
        // event.hash = "actual-hash", but metadata.ledgerRootHash = "expected-different-hash"
        LedgerEvent event = makeLedgerEvent("actual-hash", null);

        BackupMetadataDTO metadata = BackupMetadataDTO.builder()
                .version("1.0")
                .exportTimestamp(OffsetDateTime.now())
                .userId(USER_ID)
                .username("testuser")
                .ledgerRootHash("expected-different-hash")
                .ledgerEventCount(1)
                .build();

        BackupPayloadDTO payload = BackupPayloadDTO.builder()
                .metadata(metadata)
                .expenses(List.of())
                .investments(List.of())
                .loans(List.of())
                .sips(List.of())
                .ledgerEvents(List.of(event))
                .build();

        byte[] jsonBytes = TEST_MAPPER.writeValueAsBytes(payload);
        when(cryptoService.decrypt(any(), eq(PASSWORD))).thenReturn(jsonBytes);

        assertThatThrownBy(() -> backupService.importBackup(USER_ID, new byte[0], PASSWORD))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("tampered");
    }

    @Test
    void verifyBackupLedgerIntegrity_chainLinkage() throws Exception {
        // event1: prevHash=null, hash="hash1"
        // event2: prevHash="wrong-prev-hash" (should be "hash1"), hash="hash2"
        LedgerEvent event1 = makeLedgerEvent("hash1", null);
        LedgerEvent event2 = makeLedgerEvent("hash2", "wrong-prev-hash");

        // metadata.ledgerRootHash matches the last event so the root-hash check passes
        BackupMetadataDTO metadata = BackupMetadataDTO.builder()
                .version("1.0")
                .exportTimestamp(OffsetDateTime.now())
                .userId(USER_ID)
                .username("testuser")
                .ledgerRootHash("hash2")
                .ledgerEventCount(2)
                .build();

        BackupPayloadDTO payload = BackupPayloadDTO.builder()
                .metadata(metadata)
                .expenses(List.of())
                .investments(List.of())
                .loans(List.of())
                .sips(List.of())
                .ledgerEvents(List.of(event1, event2))
                .build();

        byte[] jsonBytes = TEST_MAPPER.writeValueAsBytes(payload);
        when(cryptoService.decrypt(any(), eq(PASSWORD))).thenReturn(jsonBytes);

        assertThatThrownBy(() -> backupService.importBackup(USER_ID, new byte[0], PASSWORD))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("broken");
    }
}
