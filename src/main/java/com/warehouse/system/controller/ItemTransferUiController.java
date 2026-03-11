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
                         @RequestParam(required = false) Long itemTypeId,
                         @RequestParam(required = false) Long warehouseId,
                         HttpSession session,
                         RedirectAttributes ra) {

        if (!AuthController.isLoggedIn(session)) return "redirect:/ui/login";

        String fromPn = AuthController.currentPn(session);
        User me = userRepository.findByPersonalNumber(fromPn)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // טיפול בחתימה: אם היא ריקה, אנחנו לא קורסים
            String signature = form.getSignatureBase64();
            if (signature == null || signature.isEmpty()) {
                // במקום לזרוק שגיאה, אנחנו שמים ערך ריק או מדפיסים ללוג
                System.out.println("DEBUG: Transfer requested without digital signature string.");
                signature = "";
            }

            int requestedQty = form.getQuantity();

            if (requestedQty > 1) {
                // לוגיקה עבור העברה קבוצתית
                List<Item> myItems = itemRepository.findByWarehouseId(warehouseId != null ? warehouseId : 1L).stream()
                        .filter(i -> i.getItemType().getId().equals(itemTypeId))
                        .filter(i -> i.getSignedBy() != null && i.getSignedBy().getId().equals(me.getId()))
                        .limit(requestedQty)
                        .toList();

                if (myItems.size() < requestedQty) {
                    throw new RuntimeException("לא נמצאו מספיק פריטים חתומים. נמצאו רק: " + myItems.size());
                }

                for (Item item : myItems) {
                    transferService.createTransferRequest(item.getId(), fromPn, form.getToPersonalNumber(), form.getNote(), 1, signature);
                }
                ra.addFlashAttribute("success", "נשלחו " + myItems.size() + " בקשות העברה.");
            } else {
                // העברה בודדת
                transferService.createTransferRequest(itemId, fromPn, form.getToPersonalNumber(), form.getNote(), 1, signature);
                ra.addFlashAttribute("success", "בקשת ההעברה נשלחה בהצלחה.");
            }

        } catch (Exception e) {
            // אם בכל זאת יש שגיאה (כמו כמות לא תקינה), נציג אותה
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/ui/warehouses/" + (warehouseId != null ? warehouseId : "1");
    }
}