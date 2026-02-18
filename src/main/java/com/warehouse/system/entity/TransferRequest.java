package com.warehouse.system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_requests")
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="item_id", nullable=false)
    private Long itemId;

    @Column(name="from_user_id", nullable=false)
    private Long fromUserId;

    @Column(name="to_user_id", nullable=false)
    private Long toUserId;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private TransferStatus status;

    @Column(name="note")
    private String note;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="decided_at")
    private LocalDateTime decidedAt;

    public Long getId() { return id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
