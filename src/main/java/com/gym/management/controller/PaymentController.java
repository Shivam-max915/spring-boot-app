package com.gym.management.controller;

import com.gym.management.model.Member;
import com.gym.management.model.Payment;
import com.gym.management.repository.MemberRepository;
import com.gym.management.repository.PaymentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    public PaymentController(PaymentRepository paymentRepository, MemberRepository memberRepository) {
        this.paymentRepository = paymentRepository;
        this.memberRepository = memberRepository;
    }

    @GetMapping("/payment/{memberId}")
    public String paymentPage(@PathVariable Long memberId, Model model, RedirectAttributes redirectAttributes) {
        if (memberId == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member ID");
            return "redirect:/join";
        }
        Optional<Member> member = memberRepository.findById(memberId);
        if (member.isPresent()) {
            model.addAttribute("member", member.get());
            // Calculate amount based on plan
            double amount = calculateAmount(member.get().getPlan());
            model.addAttribute("amount", amount);
            return "payment";
        } else {
            redirectAttributes.addFlashAttribute("error", "Member not found");
            return "redirect:/join";
        }
    }

    @PostMapping("/payment/process")
    public String processPayment(@RequestParam Long memberId,
                                 @RequestParam Double amount,
                                 @RequestParam String paymentMethod,
                                 @RequestParam(required = false) String transactionId,
                                 RedirectAttributes redirectAttributes) {
        if (memberId == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid member ID");
            return "redirect:/join";
        }
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            
            // Create payment record
            Payment payment = new Payment();
            payment.setMember(member);
            payment.setAmount(amount);
            payment.setPaymentDate(LocalDate.now());
            payment.setPaymentMethod(paymentMethod);
            payment.setStatus("Paid");
            payment.setPlan(member.getPlan());
            if (transactionId != null && !transactionId.trim().isEmpty()) {
                payment.setTransactionId(transactionId);
            } else {
                payment.setTransactionId("TXN" + System.currentTimeMillis());
            }
            paymentRepository.save(payment);
            
            // Update member payment status
            member.setPaymentStatus("Paid");
            member.setActive(true);
            memberRepository.save(member);
            
            redirectAttributes.addFlashAttribute("success", "Payment successful! Your membership is now active.");
            return "redirect:/success";
        } else {
            redirectAttributes.addFlashAttribute("error", "Member not found");
            return "redirect:/join";
        }
    }

    private double calculateAmount(String plan) {
        if (plan == null) return 50.0;
        switch (plan) {
            case "Monthly":
                return 50.0;
            case "Quarterly":
                return 135.0;
            case "Yearly":
                return 500.0;
            default:
                return 50.0;
        }
    }
}


