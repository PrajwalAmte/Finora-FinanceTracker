package com.finance_tracker.repository;

import com.finance_tracker.model.Sip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SipRepository extends JpaRepository<Sip, Long> {
    List<Sip> findByUserId(Long userId);
    List<Sip> findBySchemeCode(String schemeCode);
}

