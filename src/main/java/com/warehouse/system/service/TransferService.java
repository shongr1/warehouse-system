package com.warehouse.system.service;

import com.warehouse.system.entity.*;
import com.warehouse.system.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransferService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final TransactionRepository transactionRepository;
    // הוספת ה-Repositories החסרים
    private final WarehouseRepository warehouseRepository;
    private final ItemTypeRepository itemTypeRepository;

    public TransferService(
            ItemRepository itemRepository,
            TransferRequestRepository transferRequestRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            WarehouseRepository warehouseRepository, // הזרקה חדשה
            ItemTypeRepository itemTypeRepository    // הזרקה חדשה
    ) {
        this.itemRepository = itemRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.warehouseRepository = warehouseRepository; // אתחול
        this.itemTypeRepository = itemTypeRepository;    // אתחול
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
    public void createTransferRequest(Long itemId, String fromPn, String toPn, String note, Integer quantity, String signatureBase64) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (item.getStatus() != ItemStatus.SIGNED && item.getStatus() != ItemStatus.TRANSFER_PENDING) {
            throw new RuntimeException("Only SIGNED items can be transferred");
        }

        int requestedQty = (quantity != null) ? quantity : 1;
        if (requestedQty <= 0 || requestedQty > item.getQuantity()) {
            throw new RuntimeException("Invalid quantity. Max available: " + item.getQuantity());
        }

        var fromUser = item.getSignedBy();
        var toUser = userRepository.findByPersonalNumber(toPn.trim())
                .orElseThrow(() -> new RuntimeException("Target user not found: " + toPn));

        TransferRequest tr = new TransferRequest();
        tr.setItem(item);
        tr.setFromUser(fromUser);
        tr.setToUser(toUser);
        tr.setQuantity(requestedQty);
        tr.setStatus(TransferStatus.PENDING);
        tr.setNote(note);
        tr.setSignatureBase64(signatureBase64);
        tr.setCreatedAt(LocalDateTime.now());

        transferRequestRepository.save(tr);

        if (requestedQty == item.getQuantity()) {
            item.setStatus(ItemStatus.TRANSFER_PENDING);
            itemRepository.save(item);
        }
    }

    @Transactional
    public void generateBulkItems(Long warehouseId, Long itemTypeId, String catalogNumber, String prefix, int start, int end) {
        var warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("מחסן לא נמצא"));
        var itemType = itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new RuntimeException("סוג פריט לא נמצא"));

        // אם המשתמש הזין מק"ט חדש במודאל, נעדכן את סוג הפריט
        if (catalogNumber != null && !catalogNumber.isEmpty()) {
            itemType.setCatalogNumber(catalogNumber);
            itemTypeRepository.save(itemType);
        }

        for (int i = start; i <= end; i++) {
            Item item = new Item();
            item.setWarehouse(warehouse);
            item.setItemType(itemType);
            item.setSerialNumber(prefix + i);
            item.setQuantity(1);
            item.setStatus(ItemStatus.IN_STOCK);

            // ירושה אוטומטית של הקטגוריה מסוג הפריט
            if (itemType.getCategory() != null) {
                item.setCategory(itemType.getCategory());
            }

            itemRepository.save(item);
        }
    }

    @Transactional
    public void approveBatch(List<Long> ids, String approverPn) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            this.approve(id, approverPn);
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

        Item item = tr.getItem();
        LocalDateTime now = LocalDateTime.now();
        Integer transferQty = tr.getQuantity();

        Transaction transaction = new Transaction();
        transaction.setItem(item);
        transaction.setIssuer(tr.getFromUser());
        transaction.setReceiver(tr.getToUser());
        transaction.setSignatureBase64(tr.getSignatureBase64());
        transaction.setType("TRANSFER");
        transaction.setConditionNote(tr.getNote());
        transactionRepository.save(transaction);

        boolean isReturnToWarehouse = item.getWarehouse().getOwner() != null &&
                item.getWarehouse().getOwner().getId().equals(me.getId());

        if (item.getQuantity() > transferQty) {
            item.setQuantity(item.getQuantity() - transferQty);
            item.setStatus(ItemStatus.SIGNED);
            itemRepository.save(item);

            Item newItem = new Item();
            newItem.setItemType(item.getItemType());
            newItem.setWarehouse(item.getWarehouse());
            newItem.setCategory(item.getCategory());
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