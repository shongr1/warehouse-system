package com.warehouse.system.controller;

import com.warehouse.system.dto.CreateTransferRequestForm;
import com.warehouse.system.entity.Item;
import com.warehouse.system.entity.User;
import com.warehouse.system.repository.ItemRepository;
import com.warehouse.system.repository.UserRepository;
import com.warehouse.system.service.TransferService;
import com.warehouse.system.ui.AuthController;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ItemTransferUiController {

    private final TransferService transferService;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ItemTransferUiController(TransferService transferService,
                                    ItemRepository itemRepository,
                                    UserRepository userRepository) {
        this.transferService = transferService;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/ui/items/{itemId}/transfer-request")
    public String submit(@PathVariable Long itemId,
                         CreateTransferRequestForm form,
                         @RequestParam(required = false) Long itemTypeId, // חשוב! וודא שזה נשלח מה-HTML
                         @RequestParam(required = false) Long warehouseId, // חשוב! וודא שזה נשלח מה-HTML
                         HttpSession session,
                         RedirectAttributes ra) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        // שליפת המשתמש הנוכחי
        String fromPn = AuthController.currentPn(session);
        User me = userRepository.findByPersonalNumber(fromPn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            int requestedQty = form.getQuantity();

            // אם ביקשת יותר מ-1, אנחנו מבצעים העברה חכמה (Bulk)
            if (requestedQty > 1) {
                // מוצאים את כל הפריטים מאותו סוג שחתומים עליי
                List<Item> myItems = itemRepository.findByWarehouseId(warehouseId != null ? warehouseId : 1L).stream()
                        .filter(i -> i.getItemType().getId().equals(itemTypeId))
                        .filter(i -> i.getSignedBy() != null && i.getSignedBy().getId().equals(me.getId()))
                        .limit(requestedQty)
                        .toList();

                if (myItems.size() < requestedQty) {
                    throw new RuntimeException("לא נמצאו מספיק פריטים חתומים להעברה. נמצאו רק: " + myItems.size());
                }

                // מעבירים כל פריט בנפרד עם כמות 1 (כדי לא לשבור את ה-Service)
                for (Item item : myItems) {
                    transferService.createTransferRequest(item.getId(), fromPn, form.getToPersonalNumber(), form.getNote(), 1);
                }
                ra.addFlashAttribute("success", "נשלחו " + myItems.size() + " בקשות העברה בהצלחה.");
            } else {
                // העברה רגילה של פריט בודד
                transferService.createTransferRequest(itemId, fromPn, form.getToPersonalNumber(), form.getNote(), 1);
                ra.addFlashAttribute("success", "בקשת ההעברה נשלחה בהצלחה.");
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/ui/warehouses/" + (warehouseId != null ? warehouseId : "1");
    }
}