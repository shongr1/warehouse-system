package com.warehouse.system.repository;

import com.warehouse.system.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // מחזיר את כל ההיסטוריה של פריט ספציפי (לפי ה-ID שלו)
    List<Transaction> findByItemId(Long itemId);

    // מחזיר את כל הפריטים שחייל ספציפי חתום עליהם (לפי מספר אישי)
    List<Transaction> findByReceiverPersonalNumber(String personalNumber);

    // מחזיר את כל הניפוקים שבוצעו על ידי אפסנאי מסוים
    List<Transaction> findByIssuerPersonalNumber(String personalNumber);
}