package com.gym.management.controller;

import com.gym.management.model.Member;
import com.gym.management.repository.EquipmentRepository;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.PaymentRepository;
import com.gym.management.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final EquipmentRepository equipmentRepository;
    private final PaymentRepository paymentRepository;

    public AdminController(MemberRepository memberRepository, MemberService memberService,
                          EquipmentRepository equipmentRepository, PaymentRepository paymentRepository) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.equipmentRepository = equipmentRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/admin/login")
    public String login() {
        return "admin-login";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        long totalMembers = memberRepository.count();
        long activeMembers = memberRepository.countByActiveTrue();
        long expiredMembers = memberRepository.countByActiveFalse();
        long expiringSoon = memberService.countExpiringSoon(7);
        LocalDate today = LocalDate.now();
        
        // Equipment statistics
        long totalEquipment = equipmentRepository.findAll().stream()
                .mapToLong(e -> e.getQuantity() != null ? e.getQuantity() : 0)
                .sum();
        long equipmentCount = equipmentRepository.count();
        
        // Payment statistics
        long paidMembers = memberRepository.findAll().stream()
                .filter(m -> "Paid".equals(m.getPaymentStatus()))
                .count();
        long pendingPayments = memberRepository.findAll().stream()
                .filter(m -> "Pending".equals(m.getPaymentStatus()))
                .count();
        long totalPayments = paymentRepository.countByStatus("Paid");

        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("activeMembers", activeMembers);
        model.addAttribute("expiredMembers", expiredMembers);
        model.addAttribute("expiringSoon", expiringSoon);
        model.addAttribute("today", today);
        model.addAttribute("totalEquipment", totalEquipment);
        model.addAttribute("equipmentCount", equipmentCount);
        model.addAttribute("paidMembers", paidMembers);
        model.addAttribute("pendingPayments", pendingPayments);
        model.addAttribute("totalPayments", totalPayments);

        return "admin-dashboard";
    }

    @GetMapping("/admin/members")
    public String members(Model model, 
                         @RequestParam(required = false) String search,
                         @RequestParam(required = false) String batch) {
        List<Member> members;
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            members = memberRepository.findAll().stream()
                    .filter(m -> m.getName().toLowerCase().contains(searchLower) ||
                               m.getEmail().toLowerCase().contains(searchLower) ||
                               (m.getPhone() != null && m.getPhone().contains(search)) ||
                               (m.getBatch() != null && m.getBatch().toLowerCase().contains(searchLower)))
                    .toList();
            model.addAttribute("search", search);
        } else if (batch != null && !batch.trim().isEmpty()) {
            members = memberRepository.findAll().stream()
                    .filter(m -> m.getBatch() != null && m.getBatch().equals(batch))
                    .toList();
            model.addAttribute("batch", batch);
        } else {
            members = memberRepository.findAll();
        }
        LocalDate today = LocalDate.now();
        model.addAttribute("members", members);
        model.addAttribute("today", today);
        return "admin-members";
    }

    @GetMapping("/admin/members/edit/{id}")
    public String editMemberForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member ID");
            return "redirect:/admin/members";
        }
        Optional<Member> member = memberRepository.findById(id);
        if (member.isPresent()) {
            model.addAttribute("member", member.get());
            return "edit-member";
        } else {
            redirectAttributes.addFlashAttribute("error", "Member not found");
            return "redirect:/admin/members";
        }
    }

    @PostMapping("/admin/members/update")
    public String updateMember(@ModelAttribute Member member, RedirectAttributes redirectAttributes) {
        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member data");
            return "redirect:/admin/members";
        }
        Long memberId = member.getId();
        if (memberId == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member ID");
            return "redirect:/admin/members";
        }
        Optional<Member> existingMember = memberRepository.findById(memberId);
        if (existingMember.isPresent()) {
            Member updatedMember = existingMember.get();
            updatedMember.setName(member.getName());
            updatedMember.setPhone(member.getPhone());
            updatedMember.setEmail(member.getEmail());
            updatedMember.setPlan(member.getPlan());
            updatedMember.setBatch(member.getBatch());
            updatedMember.setPaymentStatus(member.getPaymentStatus());
            updatedMember.setActive(member.isActive());
            
            // If payment status is Paid, activate membership
            if ("Paid".equals(member.getPaymentStatus())) {
                updatedMember.setActive(true);
            }
            
            // If plan changed, update expiry date
            String oldPlan = updatedMember.getPlan();
            String newPlan = member.getPlan();
            if (oldPlan != null && newPlan != null && !oldPlan.equals(newPlan) && updatedMember.getJoinDate() != null) {
                LocalDate newExpiryDate = calculateExpiryDate(updatedMember.getJoinDate(), newPlan);
                updatedMember.setExpiryDate(newExpiryDate);
            }
            
            memberRepository.save(updatedMember);
            redirectAttributes.addFlashAttribute("success", "Member updated successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", "Member not found");
        }
        return "redirect:/admin/members";
    }

    @PostMapping("/admin/members/delete/{id}")
    public String deleteMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member ID");
            return "redirect:/admin/members";
        }
        if (memberRepository.existsById(id)) {
            memberRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Member deleted successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", "Member not found");
        }
        return "redirect:/admin/members";
    }

    @PostMapping("/admin/members/toggle-status/{id}")
    public String toggleMemberStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member ID");
            return "redirect:/admin/members";
        }
        Optional<Member> member = memberRepository.findById(id);
        if (member.isPresent()) {
            Member m = member.get();
            m.setActive(!m.isActive());
            memberRepository.save(m);
            redirectAttributes.addFlashAttribute("success", 
                "Member status changed to " + (m.isActive() ? "Active" : "Inactive"));
        } else {
            redirectAttributes.addFlashAttribute("error", "Member not found");
        }
        return "redirect:/admin/members";
    }

    @PostMapping("/admin/members/renew/{id}")
    public String renewMembership(@PathVariable Long id, 
                                  @RequestParam String plan,
                                  RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member ID");
            return "redirect:/admin/members";
        }
        if (plan == null || plan.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Plan selection is required");
            return "redirect:/admin/members";
        }
        Optional<Member> member = memberRepository.findById(id);
        if (member.isPresent()) {
            Member m = member.get();
            LocalDate newJoinDate = LocalDate.now();
            LocalDate newExpiryDate = calculateExpiryDate(newJoinDate, plan);
            
            m.setJoinDate(newJoinDate);
            m.setExpiryDate(newExpiryDate);
            m.setPlan(plan);
            m.setActive(true);
            
            memberRepository.save(m);
            redirectAttributes.addFlashAttribute("success", 
                "Membership renewed successfully. Expires on " + newExpiryDate);
        } else {
            redirectAttributes.addFlashAttribute("error", "Member not found");
        }
        return "redirect:/admin/members";
    }

    private LocalDate calculateExpiryDate(LocalDate startDate, String plan) {
        if (plan == null) {
            return startDate.plusMonths(1);
        }
        switch (plan) {
            case "Monthly":
                return startDate.plusMonths(1);
            case "Quarterly":
                return startDate.plusMonths(3);
            case "Yearly":
                return startDate.plusYears(1);
            default:
                return startDate.plusMonths(1);
        }
    }
}
