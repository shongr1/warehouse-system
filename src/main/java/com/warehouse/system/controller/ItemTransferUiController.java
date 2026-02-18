package com.warehouse.system.controller;

import com.warehouse.system.dto.CreateTransferRequestForm;
import com.warehouse.system.service.TransferService;
import com.warehouse.system.ui.AuthController;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ItemTransferUiController {

    private final TransferService transferService;

    public ItemTransferUiController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping("/ui/items/{itemId}/transfer-request")
    public String showForm(@PathVariable Long itemId, HttpSession session, Model model) {
        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        model.addAttribute("itemId", itemId);
        model.addAttribute("form", new CreateTransferRequestForm());
        return "transfer-request-form";
    }

    @PostMapping("/ui/items/{itemId}/transfer-request")
    public String submit(@PathVariable Long itemId,
                         CreateTransferRequestForm form,
                         HttpSession session,
                         RedirectAttributes ra) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String fromPn = AuthController.currentPn(session);

        transferService.createTransferRequest(itemId, fromPn, form.getToPersonalNumber(), form.getNote());

        ra.addFlashAttribute("success", "Transfer request sent (PENDING).");
        return "redirect:/ui";
    }
}
