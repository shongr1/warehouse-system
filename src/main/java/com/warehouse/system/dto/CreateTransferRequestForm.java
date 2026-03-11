package com.warehouse.system.dto;

public class CreateTransferRequestForm {
    private String toPersonalNumber;
    private String note;
    private int quantity;

    // הוסיף את השדה החדש כאן:
    private String signatureBase64;

    // קונסטרקטור ריק
    public CreateTransferRequestForm() {}

    // --- Getters & Setters ---

    public String getToPersonalNumber() { return toPersonalNumber; }
    public void setToPersonalNumber(String toPersonalNumber) { this.toPersonalNumber = toPersonalNumber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    // הוסיף את ה-Getter וה-Setter החדשים האלו:
    public String getSignatureBase64() { return signatureBase64; }
    public void setSignatureBase64(String signatureBase64) { this.signatureBase64 = signatureBase64; }
}