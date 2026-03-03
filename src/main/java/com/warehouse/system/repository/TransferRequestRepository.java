package com.warehouse.system.repository;

import com.warehouse.system.entity.TransferRequest;
import com.warehouse.system.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {

    // בודק אם קיימת בקשה לפי ה-ID של הפריט (שימוש ב-Item_Id בגלל הקשר החדש)
    boolean existsByItem_IdAndStatus(Long itemId, TransferStatus status);

    // ✅ זו המתודה החשובה ל-Incoming Requests!
    // היא "חופרת" לתוך אובייקט toUser ומחפשת לפי ה-ID שלו
    List<TransferRequest> findByToUser_IdAndStatus(Long userId, TransferStatus status);

    // גרסה מסודרת לפי תאריך (למקרה שתרצה שהחדשים יהיו למעלה)
    List<TransferRequest> findByToUser_IdAndStatusOrderByCreatedAtDesc(Long userId, TransferStatus status);
}