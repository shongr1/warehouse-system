package com.warehouse.system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // מקשר לפריט הספציפי מהמלאי (Item)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // מקשר למשתמש שביצע את הפעולה (האפסנאי)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    private User issuer;

    // מקשר למשתמש שקיבל את הפריט (החייל)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    private String type; // למשל: "ISSUE", "RETURN", "TRANSFER"

    private String conditionNote; // הערות על מצב הציוד

    @Lob
    @Column(columnDefinition = "TEXT") // שונה מ-LONGTEXT ל-TEXT כדי להתאים ל-PostgreSQL
    private String signatureBase64; // החתימה הדיגיטלית

    // קונסטרקטור ריק חובה
    public Transaction() {}

    @PrePersist
    protected void onCreate() {
        this.transactionDate = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public User getIssuer() { return issuer; }
    public void setIssuer(User issuer) { this.issuer = issuer; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getConditionNote() { return conditionNote; }
    public void setConditionNote(String conditionNote) { this.conditionNote = conditionNote; }

    public String getSignatureBase64() { return signatureBase64; }
    public void setSignatureBase64(String signatureBase64) { this.signatureBase64 = signatureBase64; }
}