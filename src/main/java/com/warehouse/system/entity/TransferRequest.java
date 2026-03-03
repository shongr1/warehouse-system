package com.warehouse.system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_requests")
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Column(name="quantity", nullable=false)
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name="note")
    private String note;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="decided_at")
    private LocalDateTime decidedAt;

    // --- Getters & Setters הסטנדרטיים ---

    public Long getId() { return id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public User getFromUser() { return fromUser; }
    public void setFromUser(User fromUser) { this.fromUser = fromUser; }

    public User getToUser() { return toUser; }
    public void setToUser(User toUser) { this.toUser = toUser; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    // --- מתודות עזר למניעת שגיאות ב-Service (חשוב!) ---

    // אם ה-Service עדיין קורא ל-getItemId
    public Long getItemId() {
        return (item != null) ? item.getId() : null;
    }

    // אם ה-Service עדיין קורא ל-getFromUserId
    public Long getFromUserId() {
        return (fromUser != null) ? fromUser.getId() : null;
    }

    // אם ה-Service עדיין קורא ל-getToUserId
    public Long getToUserId() {
        return (toUser != null) ? toUser.getId() : null;
    }
}