package com.finance_tracker.service;

import com.finance_tracker.dto.LedgerIntegrityResultDTO;
import com.finance_tracker.model.LedgerEvent;
import com.finance_tracker.repository.LedgerEventRepository;
import com.finance_tracker.utils.HashingUtils;
import com.finance_tracker.utils.security.VaultKeyContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerEventRepository ledgerEventRepository;

    @Mock
    private HashingUtils hashingUtils;

    @Mock
    private FieldEncryptionService encryptionService;

    @InjectMocks
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        VaultKeyContext.clear();
    }

    @AfterEach
    void tearDown() {
        VaultKeyContext.clear();
    }

    private LedgerEvent buildEvent(String userId, Long sequence, String hash, String prevHash) {
        LedgerEvent e = new LedgerEvent();
        e.setId(UUID.randomUUID());
        e.setEventUuid(UUID.randomUUID());
        e.setEntityType("EXPENSE");
        e.setEntityId("1");
        e.setActionType("CREATE");
        e.setBeforeState(null);
        e.setAfterState("encrypted-after");
        e.setEventTimestamp(OffsetDateTime.now());
        e.setPrevHash(prevHash);
        e.setHash(hash);
        e.setUserId(userId);
        e.setEventVersion(1);
        return e;
    }

    // ── recordEvent ───────────────────────────────────────────────────────────

    @Test
    void recordEvent_firstEvent_noPrevHash() {
        when(hashingUtils.toCanonicalJson(any())).thenReturn("null");
        when(hashingUtils.toCanonicalJson(isNull())).thenReturn("null");
        when(encryptionService.encrypt(anyString(), any())).thenReturn("encrypted");
        when(ledgerEventRepository.findTopByUserIdOrderByEventSequenceDesc("1"))
                .thenReturn(Optional.empty());
        when(hashingUtils.computeHash(any(), any(), any(), any(), any(), any(), isNull(), any(), anyInt()))
                .thenReturn("hash-abc");

        LedgerEvent saved = buildEvent("1", 1L, "hash-abc", null);
        when(ledgerEventRepository.save(any())).thenReturn(saved);

        LedgerEvent result = ledgerService.recordEvent("EXPENSE", "1", "CREATE", null, new Object(), "1");

        assertThat(result.getHash()).isEqualTo("hash-abc");
        ArgumentCaptor<LedgerEvent> cap = ArgumentCaptor.forClass(LedgerEvent.class);
        verify(ledgerEventRepository).save(cap.capture());
        assertThat(cap.getValue().getPrevHash()).isNull();
    }

    @Test
    void recordEvent_subsequentEvent_chainsPrevHash() {
        LedgerEvent prev = buildEvent("1", 1L, "prev-hash", null);
        when(hashingUtils.toCanonicalJson(any())).thenReturn("json");
        when(encryptionService.encrypt(anyString(), any())).thenReturn("encrypted");
        when(ledgerEventRepository.findTopByUserIdOrderByEventSequenceDesc("1"))
                .thenReturn(Optional.of(prev));
        when(hashingUtils.computeHash(any(), any(), any(), any(), any(), any(), eq("prev-hash"), any(), anyInt()))
                .thenReturn("hash-xyz");

        LedgerEvent saved = buildEvent("1", 2L, "hash-xyz", "prev-hash");
        when(ledgerEventRepository.save(any())).thenReturn(saved);

        LedgerEvent result = ledgerService.recordEvent("EXPENSE", "2", "UPDATE", new Object(), new Object(), "1");

        ArgumentCaptor<LedgerEvent> cap = ArgumentCaptor.forClass(LedgerEvent.class);
        verify(ledgerEventRepository).save(cap.capture());
        assertThat(cap.getValue().getPrevHash()).isEqualTo("prev-hash");
    }

    @Test
    void recordEvent_withVaultKey_encryptsWithVaultKey() {
        VaultKeyContext.set("my-vault-key");

        when(hashingUtils.toCanonicalJson(any())).thenReturn("json");
        when(encryptionService.encrypt(anyString(), eq("my-vault-key"))).thenReturn("vault-encrypted");
        when(ledgerEventRepository.findTopByUserIdOrderByEventSequenceDesc("1"))
                .thenReturn(Optional.empty());
        when(hashingUtils.computeHash(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn("hash");
        LedgerEvent saved = buildEvent("1", 1L, "hash", null);
        when(ledgerEventRepository.save(any())).thenReturn(saved);

        ledgerService.recordEvent("EXPENSE", "1", "CREATE", null, new Object(), "1");

        verify(encryptionService).encrypt(anyString(), eq("my-vault-key"));
    }

    @Test
    void recordEvent_nullBeforeAndAfter_storesNullStates() {
        when(hashingUtils.toCanonicalJson(isNull())).thenReturn("null");
        when(ledgerEventRepository.findTopByUserIdOrderByEventSequenceDesc("1"))
                .thenReturn(Optional.empty());
        when(hashingUtils.computeHash(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn("hash");
        LedgerEvent saved = buildEvent("1", 1L, "hash", null);
        when(ledgerEventRepository.save(any())).thenReturn(saved);

        ledgerService.recordEvent("EXPENSE", "1", "DELETE", null, null, "1");

        ArgumentCaptor<LedgerEvent> cap = ArgumentCaptor.forClass(LedgerEvent.class);
        verify(ledgerEventRepository).save(cap.capture());
        assertThat(cap.getValue().getBeforeState()).isNull();
        assertThat(cap.getValue().getAfterState()).isNull();
    }

    // ── verifyIntegrity ───────────────────────────────────────────────────────

    @Test
    void verifyIntegrity_emptyChain_returnsValidWithZeroCount() {
        when(ledgerEventRepository.findByUserIdOrderByEventSequenceAsc("1")).thenReturn(List.of());

        LedgerIntegrityResultDTO result = ledgerService.verifyIntegrity("1");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getEventCount()).isEqualTo(0);
    }

    @Test
    void verifyIntegrity_validChain_returnsTrue() {
        LedgerEvent e = buildEvent("1", 1L, "computed-hash", null);
        e.setBeforeState(null);
        e.setAfterState("encrypted-after");

        when(ledgerEventRepository.findByUserIdOrderByEventSequenceAsc("1")).thenReturn(List.of(e));
        when(hashingUtils.computeHash(
                eq(e.getEntityType()), eq(e.getEntityId()), eq(e.getActionType()),
                eq("null"), eq("encrypted-after"),
                eq(e.getEventTimestamp().toString()),
                isNull(), eq(e.getUserId()), eq(e.getEventVersion())))
                .thenReturn("computed-hash");

        LedgerIntegrityResultDTO result = ledgerService.verifyIntegrity("1");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getEventCount()).isEqualTo(1);
    }

    @Test
    void verifyIntegrity_tamperedHash_returnsFalseWithBrokenUuid() {
        LedgerEvent e = buildEvent("1", 1L, "stored-hash", null);
        e.setBeforeState("before");
        e.setAfterState("after");

        when(ledgerEventRepository.findByUserIdOrderByEventSequenceAsc("1")).thenReturn(List.of(e));
        when(hashingUtils.computeHash(any(), any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn("different-hash");

        LedgerIntegrityResultDTO result = ledgerService.verifyIntegrity("1");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getBrokenAtEventUuid()).isEqualTo(e.getEventUuid().toString());
    }

    @Test
    void verifyIntegrity_multipleEvents_chainsPrevHash() {
        LedgerEvent e1 = buildEvent("1", 1L, "hash-1", null);
        e1.setBeforeState(null);
        e1.setAfterState("after-1");

        LedgerEvent e2 = buildEvent("1", 2L, "hash-2", "hash-1");
        e2.setBeforeState("before-2");
        e2.setAfterState("after-2");

        when(ledgerEventRepository.findByUserIdOrderByEventSequenceAsc("1"))
                .thenReturn(List.of(e1, e2));

        when(hashingUtils.computeHash(eq("EXPENSE"), eq("1"), eq("CREATE"),
                eq("null"), eq("after-1"), any(), isNull(), eq("1"), eq(1)))
                .thenReturn("hash-1");
        when(hashingUtils.computeHash(eq("EXPENSE"), eq("1"), eq("CREATE"),
                eq("before-2"), eq("after-2"), any(), eq("hash-1"), eq("1"), eq(1)))
                .thenReturn("hash-2");

        LedgerIntegrityResultDTO result = ledgerService.verifyIntegrity("1");

        assertThat(result.isValid()).isTrue();
        assertThat(result.getEventCount()).isEqualTo(2);
    }

    // ── getTimeline ───────────────────────────────────────────────────────────

    @Test
    void getTimeline_delegatesToRepo() {
        LedgerEvent e = buildEvent("1", 1L, "hash", null);
        when(ledgerEventRepository.findByEntityTypeAndEntityIdOrderByEventSequenceAsc("EXPENSE", "1"))
                .thenReturn(List.of(e));

        List<LedgerEvent> result = ledgerService.getTimeline("EXPENSE", "1");

        assertThat(result).hasSize(1);
    }

    @Test
    void getTimeline_noEvents_returnsEmptyList() {
        when(ledgerEventRepository.findByEntityTypeAndEntityIdOrderByEventSequenceAsc("EXPENSE", "99"))
                .thenReturn(List.of());

        assertThat(ledgerService.getTimeline("EXPENSE", "99")).isEmpty();
    }
}
