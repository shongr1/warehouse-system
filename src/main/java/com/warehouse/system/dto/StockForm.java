package com.warehouse.system.dto;

public class StockForm {
    private Long warehouseId;
    private Long itemTypeId;
    private Integer quantity;

    private String serialMode;
    private String startSerial;
    private String manualSerials;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(Long itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getSerialMode() {
        return serialMode;
    }

    public void setSerialMode(String serialMode) {
        this.serialMode = serialMode;
    }

    public String getStartSerial() {
        return startSerial;
    }

    public void setStartSerial(String startSerial) {
        this.startSerial = startSerial;
    }

    public String getManualSerials() {
        return manualSerials;
    }

    public void setManualSerials(String manualSerials) {
        this.manualSerials = manualSerials;
    }
}

