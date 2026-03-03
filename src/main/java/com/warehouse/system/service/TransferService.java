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

    /**
     * חתימה ראשונית על פריט מהמחסן (ממצב IN_STOCK)
     */
    @Transactional
    public void signToMe(Long itemId, String pn) {
        var me = userRepository.findByPersonalNumber(pn.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + pn));

        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        if (item.getStatus() != ItemStatus.IN_STOCK) {
            throw new RuntimeException("Item must be IN_STOCK to sign");
        }

        // הגדרת חתימה
        item.setSignedBy(me);
        item.setStatus(ItemStatus.SIGNED);

        // קביעת תוקף חתימה לשנה אחת בדיוק
        LocalDateTime now = LocalDateTime.now();
        item.setSignatureDate(now);
        item.setSignatureExpiryDate(now.plusYears(1));

        itemRepository.save(item);
    }

    /**
     * יצירת בקשת העברה (או בקשת זיכוי למחסן)
     */
    @Transactional
    public void createTransferRequest(Long itemId, String fromPn, String toPn, String note) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // הגנות לוגיות
        if (item.getStatus() == ItemStatus.TRANSFER_PENDING) {
            throw new RuntimeException("Item is already in a transfer process");
        }

        if (item.getStatus() != ItemStatus.SIGNED) {
            throw new RuntimeException("Only SIGNED items can be transferred/returned");
        }

        // וידוי שהשולח הוא אכן החתום על הפריט
        if (item.getSignedBy() == null || !fromPn.equals(item.getSignedBy().getPersonalNumber().trim())) {
            throw new RuntimeException("You are not authorized to transfer this item");
        }

        var toUser = userRepository.findByPersonalNumber(toPn.trim())
                .orElseThrow(() -> new RuntimeException("Target user not found: " + toPn));

        // יצירת הבקשה
        TransferRequest tr = new TransferRequest();
        tr.setItemId(itemId);
        tr.setFromUserId(item.getSignedBy().getId());
        tr.setToUserId(toUser.getId());
        tr.setStatus(TransferStatus.PENDING);
        tr.setNote(note);
        tr.setCreatedAt(LocalDateTime.now());

        transferRequestRepository.save(tr);

        // נעילת הפריט לשינויים עד האישור
        item.setStatus(ItemStatus.TRANSFER_PENDING);
        itemRepository.save(item);
    }

    /**
     * אישור הבקשה - כאן מתבצע הזיכוי או ההעברה בפועל
     */
    @Transactional
    public void approve(Long transferRequestId, String approverPn) {
        var me = userRepository.findByPersonalNumber(approverPn.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + approverPn));

        var tr = transferRequestRepository.findById(transferRequestId)
                .orElseThrow(() -> new RuntimeException("TransferRequest not found"));

        if (tr.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Request is no longer pending");
        }

        var item = itemRepository.findById(tr.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        LocalDateTime now = LocalDateTime.now();

        // בדיקה: האם זה זיכוי למחסן?
        // (אם המאשר הוא בעל המחסן שבו הפריט רשום)
        if (item.getWarehouse().getOwner() != null &&
                item.getWarehouse().getOwner().getId().equals(me.getId())) {

            // לוגיקת זיכוי: הפריט חוזר למלאי פנוי
            item.setSignedBy(null);
            item.setStatus(ItemStatus.IN_STOCK);
            item.setSignatureDate(null); // אין חתימה פעילה
            item.setSignatureExpiryDate(null);

        } else {
            // לוגיקת העברה בין חיילים: הפריט עובר חתימה
            item.setSignedBy(me);
            item.setStatus(ItemStatus.SIGNED);
            item.setSignatureDate(now);
            item.setSignatureExpiryDate(now.plusYears(1)); // חידוש תוקף לשנה
        }

        itemRepository.save(item);

        // סגירת הבקשה
        tr.setStatus(TransferStatus.APPROVED);
        tr.setDecidedAt(now);
        transferRequestRepository.save(tr);
    }

    @Transactional
    public void reject(Long transferRequestId, String approverPn) {
        var tr = transferRequestRepository.findById(transferRequestId).orElseThrow();
        var item = itemRepository.findById(tr.getItemId()).orElseThrow();

        // החזרת המצב לקדמותו
        item.setStatus(ItemStatus.SIGNED);
        itemRepository.save(item);

        tr.setStatus(TransferStatus.REJECTED);
        tr.setDecidedAt(LocalDateTime.now());
        transferRequestRepository.save(tr);
    }
}