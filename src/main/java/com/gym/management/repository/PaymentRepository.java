package com.gym.management.repository;

import com.gym.management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByMemberId(Long memberId);
    long countByStatus(String status);
    Optional<Payment> findTopByMemberIdOrderByPaymentDateDesc(Long memberId);
}


