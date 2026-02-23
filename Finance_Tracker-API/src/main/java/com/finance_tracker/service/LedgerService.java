package com.finance_tracker.service;

import com.finance_tracker.dto.LedgerIntegrityResultDTO;
import com.finance_tracker.model.LedgerEvent;
import com.finance_tracker.repository.LedgerEventRepository;
import com.finance_tracker.utils.HashingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEventRepository ledgerEventRepository;
    private final HashingUtils hashingUtils;

    @Transactional
    public LedgerEvent recordEvent(
            String entityType,
            String entityId,
            String actionType,
            Object before,
            Object after,
            String userId
    ) {
        String beforeJson = hashingUtils.toCanonicalJson(before);
        String afterJson = hashingUtils.toCanonicalJson(after);

        String prevHash = ledgerEventRepository
                .findTopByUserIdOrderByEventSequenceDesc(userId)
                .map(LedgerEvent::getHash)
                .orElse(null);

        OffsetDateTime eventTimestamp = OffsetDateTime.now();

        int eventVersion = 1;

        String hash = hashingUtils.computeHash(
                entityType,
                entityId,
                actionType,
                beforeJson,
                afterJson,
                eventTimestamp.toString(),
                prevHash,
                userId,
                eventVersion
        );

        LedgerEvent event = new LedgerEvent();
        event.setId(UUID.randomUUID());
        event.setEventUuid(UUID.randomUUID());
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setActionType(actionType);
        event.setBeforeState(before == null ? null : beforeJson);
        event.setAfterState(after == null ? null : afterJson);
        event.setEventTimestamp(eventTimestamp);
        event.setPrevHash(prevHash);
        event.setHash(hash);
        event.setUserId(userId);
        event.setEventVersion(eventVersion);

        return ledgerEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public LedgerIntegrityResultDTO verifyIntegrity(String userId) {
        List<LedgerEvent> events = ledgerEventRepository.findByUserIdOrderByEventSequenceAsc(userId);

        String prevHash = null;

        for (LedgerEvent event : events) {
            String recomputed = hashingUtils.computeHash(
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getActionType(),
                    event.getBeforeState() == null ? "null" : event.getBeforeState(),
                    event.getAfterState() == null ? "null" : event.getAfterState(),
                    event.getEventTimestamp().toString(),
                    prevHash,
                    event.getUserId(),
                    event.getEventVersion()
            );

            if (!recomputed.equals(event.getHash())) {
                return LedgerIntegrityResultDTO.builder()
                        .valid(false)
                        .eventCount(events.size())
                        .brokenAtEventUuid(event.getEventUuid().toString())
                        .build();
            }

            prevHash = event.getHash();
        }

        return LedgerIntegrityResultDTO.builder()
                .valid(true)
                .eventCount(events.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LedgerEvent> getTimeline(String entityType, String entityId) {
        return ledgerEventRepository.findByEntityTypeAndEntityIdOrderByEventSequenceAsc(entityType, entityId);
    }
}
