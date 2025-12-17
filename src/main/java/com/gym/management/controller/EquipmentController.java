package com.gym.management.controller;

import com.gym.management.model.Equipment;
import com.gym.management.repository.EquipmentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/equipment")
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;

    public EquipmentController(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @GetMapping
    public String listEquipment(Model model) {
        List<Equipment> equipmentList = equipmentRepository.findAll();
        long totalEquipment = equipmentList.stream().mapToLong(e -> e.getQuantity() != null ? e.getQuantity() : 0).sum();
        model.addAttribute("equipmentList", equipmentList);
        model.addAttribute("totalEquipment", totalEquipment);
        return "admin-equipment";
    }

    @PostMapping("/add")
    public String addEquipment(@ModelAttribute Equipment equipment, RedirectAttributes redirectAttributes) {
        if (equipment.getName() == null || equipment.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Equipment name is required");
            return "redirect:/admin/equipment";
        }
        if (equipment.getQuantity() == null || equipment.getQuantity() < 0) {
            equipment.setQuantity(0);
        }
        if (equipment.getStatus() == null || equipment.getStatus().trim().isEmpty()) {
            equipment.setStatus("Available");
        }
        equipmentRepository.save(equipment);
        redirectAttributes.addFlashAttribute("success", "Equipment added successfully");
        return "redirect:/admin/equipment";
    }

    @PostMapping("/update/{id}")
    public String updateEquipment(@PathVariable Long id, @ModelAttribute Equipment equipment, RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid equipment ID");
            return "redirect:/admin/equipment";
        }
        Optional<Equipment> existing = equipmentRepository.findById(id);
        if (existing.isPresent()) {
            Equipment eq = existing.get();
            eq.setName(equipment.getName());
            eq.setCategory(equipment.getCategory());
            eq.setQuantity(equipment.getQuantity());
            eq.setStatus(equipment.getStatus());
            eq.setDescription(equipment.getDescription());
            equipmentRepository.save(eq);
            redirectAttributes.addFlashAttribute("success", "Equipment updated successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", "Equipment not found");
        }
        return "redirect:/admin/equipment";
    }

    @PostMapping("/delete/{id}")
    public String deleteEquipment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid equipment ID");
            return "redirect:/admin/equipment";
        }
        if (equipmentRepository.existsById(id)) {
            equipmentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Equipment deleted successfully");
        } else {
            redirectAttributes.addFlashAttribute("error", "Equipment not found");
        }
        return "redirect:/admin/equipment";
    }
}


