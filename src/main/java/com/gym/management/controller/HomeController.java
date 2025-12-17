package com.gym.management.controller;

import com.gym.management.model.Member;
import com.gym.management.repository.MemberRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
public class HomeController {

    private final MemberRepository memberRepository;

    public HomeController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/plans")
    public String plans() {
        return "plans";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("member", new Member());
        return "join";
    }

    @PostMapping("/join")
    public String joinSubmit(@ModelAttribute Member member) {
        member.setActive(false); // Set to false until payment is made
        member.setPaymentStatus("Pending"); // Default payment status
        LocalDate joinDate = LocalDate.now();
        member.setJoinDate(joinDate);
        
        // Calculate expiry date based on plan
        LocalDate expiryDate = calculateExpiryDate(joinDate, member.getPlan());
        member.setExpiryDate(expiryDate);
        
        // Check if membership is expired
        if (expiryDate.isBefore(LocalDate.now())) {
            member.setActive(false);
        }
        
        Member savedMember = memberRepository.save(member);
        // Redirect to payment page
        return "redirect:/payment/" + savedMember.getId();
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

    @GetMapping("/success")
    public String success() {
        return "success";
    }

    @PostMapping("/contact")
    public String contactSubmit(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam String subject,
                                @RequestParam String message,
                                RedirectAttributes redirectAttributes) {
        // In a real application, you would send an email or save to database
        // For now, we'll just redirect with a success message
        redirectAttributes.addFlashAttribute("success", 
            "Thank you for contacting us, " + name + "! We'll get back to you soon.");
        return "redirect:/contact";
    }
}
