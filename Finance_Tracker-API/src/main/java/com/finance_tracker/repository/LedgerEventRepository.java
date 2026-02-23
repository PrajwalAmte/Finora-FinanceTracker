package com.finance_tracker.repository;

import com.finance_tracker.model.LedgerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEventRepository extends JpaRepository<LedgerEvent, UUID> {

    Optional<LedgerEvent> findTopByUserIdOrderByEventSequenceDesc(String userId);

    List<LedgerEvent> findByUserIdOrderByEventSequenceAsc(String userId);

    List<LedgerEvent> findByEntityTypeAndEntityIdOrderByEventSequenceAsc(String entityType, String entityId);
}
