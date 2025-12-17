package com.gym.management.repository;

import com.gym.management.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    long countByActiveTrue();

    long countByActiveFalse();
}
