package com.warehouse.system.dto;

import java.util.ArrayList;
import java.util.List;

public class StockForm {
    private Long warehouseId;
    private Long itemTypeId;
    private Integer quantity;

    private String serialMode;
    private String startSerial;
    private String manualSerials;
    private String prefix;
    private boolean kit;

    // 1. שדה לקליטת שם הקטגוריה מהטופס
    private String categoryName;

    // 2. השדה הקריטי: רשימת הרכיבים שתגיע מהטבלה ב-HTML
    private List<KitComponentDTO> kitComponents = new ArrayList<>();

    // --- Getters & Setters ---

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getItemTypeId() { return itemTypeId; }
    public void setItemTypeId(Long itemTypeId) { this.itemTypeId = itemTypeId; }

    public Integer getQuantity() { return (quantity != null) ? quantity : 1; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getSerialMode() { return serialMode; }
    public void setSerialMode(String serialMode) { this.serialMode = serialMode; }

    public String getStartSerial() { return startSerial; }
    public void setStartSerial(String startSerial) { this.startSerial = startSerial; }

    public String getManualSerials() { return manualSerials; }
    public void setManualSerials(String manualSerials) { this.manualSerials = manualSerials; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public boolean isKit() { return kit; }
    public void setKit(boolean kit) { this.kit = kit; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public List<KitComponentDTO> getKitComponents() { return kitComponents; }
    public void setKitComponents(List<KitComponentDTO> kitComponents) { this.kitComponents = kitComponents; }

    // --- מחלקת עזר (Inner Class) כדי להחזיק את נתוני הרכיב מהטופס ---
    public static class KitComponentDTO {
        private String componentName;
        private String subCatalogNumber;
        private int quantity;

        public String getComponentName() { return componentName; }
        public void setComponentName(String componentName) { this.componentName = componentName; }

        public String getSubCatalogNumber() { return subCatalogNumber; }
        public void setSubCatalogNumber(String subCatalogNumber) { this.subCatalogNumber = subCatalogNumber; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}