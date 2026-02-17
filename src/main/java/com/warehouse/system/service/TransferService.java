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

    public TransferService(ItemRepository itemRepository,
                           UserRepository userRepository,
                           TransferRequestRepository transferRequestRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.transferRequestRepository = transferRequestRepository;
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

        itemRepository.save(item);
    }

    @Transactional
    public void createTransferRequest(Long itemId, String fromPn, String toPn, String note) {

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // ✅ NEW: אם כבר בתהליך - לא מאפשרים ליצור עוד בקשה
        if (item.getStatus() == ItemStatus.TRANSFER_PENDING) {
            throw new RuntimeException("Item is already in TRANSFER_PENDING");
        }

        if (item.getStatus() != ItemStatus.SIGNED) {
            throw new RuntimeException("Item must be SIGNED to request transfer");
        }

        if (item.getSignedBy() == null || item.getSignedBy().getPersonalNumber() == null ||
                !fromPn.equals(item.getSignedBy().getPersonalNumber().trim())) {
            throw new RuntimeException("You are not the signer of this item");
        }

        if (toPn == null || toPn.isBlank()) {
            throw new RuntimeException("Target personal number is required");
        }

        var toUser = userRepository.findByPersonalNumber(toPn.trim())
                .orElseThrow(() -> new RuntimeException("Target user not found: " + toPn));

        // ✅ NEW: אם כבר יש בקשה פתוחה - חוסם
        if (transferRequestRepository.existsByItemIdAndStatus(itemId, TransferStatus.PENDING)) {
            throw new RuntimeException("There is already a PENDING request for this item");
        }

        TransferRequest tr = new TransferRequest();
        tr.setItemId(itemId);
        tr.setFromUserId(item.getSignedBy().getId());
        tr.setToUserId(toUser.getId());
        tr.setStatus(TransferStatus.PENDING);
        tr.setNote(note);
        tr.setCreatedAt(LocalDateTime.now());

        transferRequestRepository.save(tr);

        // ✅ NEW: משנה סטטוס כך שה-UI יראה "Pending" ולא יאפשר שוב
        item.setStatus(ItemStatus.TRANSFER_PENDING);
        itemRepository.save(item);
    }
    @Transactional
    public void approve(Long transferRequestId, String approverPn) {

        var me = userRepository.findByPersonalNumber(approverPn.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + approverPn));

        var tr = transferRequestRepository.findById(transferRequestId)
                .orElseThrow(() -> new RuntimeException("TransferRequest not found: " + transferRequestId));

        if (tr.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Request is not PENDING");
        }

        if (!me.getId().equals(tr.getToUserId())) {
            throw new RuntimeException("Forbidden: not your request");
        }

        var item = itemRepository.findById(tr.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found: " + tr.getItemId()));

        // להעביר חתימה
        item.setSignedBy(me);
        item.setStatus(ItemStatus.SIGNED);
        itemRepository.save(item);

        tr.setStatus(TransferStatus.APPROVED);
        tr.setDecidedAt(LocalDateTime.now());
        transferRequestRepository.save(tr);
    }

    @Transactional
    public void reject(Long transferRequestId, String approverPn) {

        var me = userRepository.findByPersonalNumber(approverPn.trim())
                .orElseThrow(() -> new RuntimeException("User not found: " + approverPn));

        var tr = transferRequestRepository.findById(transferRequestId)
                .orElseThrow(() -> new RuntimeException("TransferRequest not found: " + transferRequestId));

        if (tr.getStatus() != TransferStatus.PENDING) {
            throw new RuntimeException("Request is not PENDING");
        }

        if (!me.getId().equals(tr.getToUserId())) {
            throw new RuntimeException("Forbidden: not your request");
        }

        var item = itemRepository.findById(tr.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found: " + tr.getItemId()));

        // חוזר להיות SIGNED אצל השולח (כלומר נשאר אותו signed_by_user_id שהיה)
        item.setStatus(ItemStatus.SIGNED);
        itemRepository.save(item);

        tr.setStatus(TransferStatus.REJECTED);
        tr.setDecidedAt(LocalDateTime.now());
        transferRequestRepository.save(tr);
    }

}
