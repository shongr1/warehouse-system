package com.warehouse.system.dto;

public class CreateTransferRequestForm {
    private String toPersonalNumber;
    private String note;

    public String getToPersonalNumber() { return toPersonalNumber; }
    public void setToPersonalNumber(String toPersonalNumber) { this.toPersonalNumber = toPersonalNumber; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
