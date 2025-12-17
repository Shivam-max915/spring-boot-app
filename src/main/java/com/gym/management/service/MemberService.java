package com.gym.management.service;

import com.gym.management.model.Member;
import com.gym.management.repository.MemberRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MemberService {

    private final MemberRepository repo;

    public MemberService(MemberRepository repo) {
        this.repo = repo;
    }

    public long totalMembers() {
        return repo.count();
    }

    public long activeMembers() {
        return repo.countByActiveTrue();
    }

    public long expiredMembers() {
        return repo.countByActiveFalse();
    }

    public List<Member> allMembers() {
        return repo.findAll();
    }

    /**
     * Automatically check and update expired memberships
     * Runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkAndUpdateExpiredMemberships() {
        LocalDate today = LocalDate.now();
        List<Member> allMembers = repo.findAll();
        
        for (Member member : allMembers) {
            if (member.getExpiryDate() != null && 
                member.getExpiryDate().isBefore(today) && 
                member.isActive()) {
                member.setActive(false);
                repo.save(member);
            }
        }
    }

    public long countExpiringSoon(int days) {
        LocalDate futureDate = LocalDate.now().plusDays(days);
        return repo.findAll().stream()
                .filter(m -> m.getExpiryDate() != null 
                        && m.getExpiryDate().isAfter(LocalDate.now())
                        && m.getExpiryDate().isBefore(futureDate)
                        && m.isActive())
                .count();
    }
}
