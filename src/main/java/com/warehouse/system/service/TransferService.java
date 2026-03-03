package com.warehouse.system.service;

import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransferService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final TransferRequestRepository transferRequestRepository;

    public TransferService(
            ItemRepository itemRepository,
            TransferRequestRepository transferRequestRepository,
            UserRepository userRepository
    ) {
        this.itemRepository = itemRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void signToMe(Long itemId, String pn) {
        var me = userRepository.findByPersonalNumber(pn.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + pn));

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        if (item.getStatus() != ItemStatus.IN_STOCK) {
            throw new RuntimeException("Item must be IN_STOCK to sign");
        }

        item.setSignedBy(me);
        item.setStatus(ItemStatus.SIGNED);
        LocalDateTime now = LocalDateTime.now();
        item.setSignatureDate(now);
        item.setSignatureExpiryDate(now.plusYears(1));

        itemRepository.save(item);
    }

    @Transactional
    public void createTransferRequest(Long itemId, String fromPn, String toPn, String note, Integer quantity) {
        // שליפת הפריט
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (item.getStatus() != ItemStatus.SIGNED) {
            throw new RuntimeException("Only SIGNED items can be transferred");
        }

        // בדיקת כמות
        int requestedQty = (quantity != null) ? quantity : 1;
        if (requestedQty <= 0 || requestedQty > item.getQuantity()) {
            throw new RuntimeException("Invalid quantity. Max available: " + item.getQuantity());
        }

        // שליפת המשתמשים כאובייקטים (חיוני ל-Entity החדש)
        var fromUser = item.getSignedBy();
        var toUser = userRepository.findByPersonalNumber(toPn.trim())
                .orElseThrow(() -> new RuntimeException("Target user not found: " + toPn));

        // יצירת הבקשה - שים לב לשימוש ב-setItem ו-setFromUser/ToUser
        TransferRequest tr = new TransferRequest();
        tr.setItem(item);
        tr.setFromUser(fromUser);
        tr.setToUser(toUser);
        tr.setQuantity(requestedQty);
        tr.setStatus(TransferStatus.PENDING);
        tr.setNote(note);
        tr.setCreatedAt(LocalDateTime.now());

        transferRequestRepository.save(tr);

        // אם מעבירים את כל הכמות, נסמן את הפריט כ"ממתין להעברה"
        if (requestedQty == item.getQuantity()) {
            item.setStatus(ItemStatus.TRANSFER_PENDING);
            itemRepository.save(item);
        }
    }

    @Transactional
    public void approve(Long transferRequestId, String approverPn) {
        var me = userRepository.findByPersonalNumber(approverPn.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var tr = transferRequestRepository.findById(transferRequestId)
                .orElseThrow(() -> new RuntimeException("TransferRequest not found"));

        if (tr.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Request is no longer pending");
        }

        // שימוש ב-tr.getItem() במקום שליפה ידנית נוספת מה-Repo
        Item item = tr.getItem();
        LocalDateTime now = LocalDateTime.now();
        Integer transferQty = tr.getQuantity();

        // בדיקה אם מחזירים למחסן (המשתמש המאשר הוא בעל המחסן)
        boolean isReturnToWarehouse = item.getWarehouse().getOwner() != null &&
                item.getWarehouse().getOwner().getId().equals(me.getId());

        if (item.getQuantity() > transferQty) {
            // פיצול פריט (נשאר חלק אצלי, חלק עובר)
            item.setQuantity(item.getQuantity() - transferQty);
            item.setStatus(ItemStatus.SIGNED);
            itemRepository.save(item);

            Item newItem = new Item();
            newItem.setItemType(item.getItemType());
            newItem.setWarehouse(item.getWarehouse());
            newItem.setCategory(item.getCategory());
            newItem.setSerialNumber(null); // ב-Bulk לרוב אין סיריאלי ייחודי לכל יחידה מפוצלת
            newItem.setQuantity(transferQty);

            if (isReturnToWarehouse) {
                newItem.setSignedBy(null);
                newItem.setStatus(ItemStatus.IN_STOCK);
            } else {
                newItem.setSignedBy(me);
                newItem.setStatus(ItemStatus.SIGNED);
                newItem.setSignatureDate(now);
                newItem.setSignatureExpiryDate(now.plusYears(1));
            }
            itemRepository.save(newItem);

        } else {
            // העברת כל הפריט (שינוי סטטוס/חתימה קיים)
            if (isReturnToWarehouse) {
                item.setSignedBy(null);
                item.setStatus(ItemStatus.IN_STOCK);
                item.setSignatureDate(null);
                item.setSignatureExpiryDate(null);
            } else {
                item.setSignedBy(me);
                item.setStatus(ItemStatus.SIGNED);
                item.setSignatureDate(now);
                item.setSignatureExpiryDate(now.plusYears(1));
            }
            itemRepository.save(item);
        }

        tr.setStatus(TransferStatus.APPROVED);
        tr.setDecidedAt(now);
        transferRequestRepository.save(tr);
    }

    @Transactional
    public void reject(Long transferRequestId, String approverPn) {
        var tr = transferRequestRepository.findById(transferRequestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Item item = tr.getItem();

        if (item.getStatus() == ItemStatus.TRANSFER_PENDING) {
            item.setStatus(ItemStatus.SIGNED);
            itemRepository.save(item);
        }

        tr.setStatus(TransferStatus.REJECTED);
        tr.setDecidedAt(LocalDateTime.now());
        transferRequestRepository.save(tr);
    }
}