package com.finance_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "ledger_events")
public class LedgerEvent {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Generated(event = EventType.INSERT)
    @Column(name = "event_sequence", insertable = false, updatable = false)
    private Long eventSequence;

    @Column(name = "event_uuid", nullable = false, unique = true, updatable = false)
    private UUID eventUuid;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "before_state", columnDefinition = "text")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "text")
    private String afterState;

    @Column(name = "event_timestamp", nullable = false)
    private OffsetDateTime eventTimestamp;

    @Column(name = "prev_hash")
    private String prevHash;

    @Column(name = "hash", nullable = false)
    private String hash;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;
}
