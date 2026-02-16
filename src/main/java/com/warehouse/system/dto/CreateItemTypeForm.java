package com.warehouse.system.dto;

public class CreateItemTypeForm {

    private String catalogNumber;
    private String name;
    private boolean serialized;

    public String getCatalogNumber() { return catalogNumber; }
    public void setCatalogNumber(String catalogNumber) { this.catalogNumber = catalogNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isSerialized() { return serialized; }
    public void setSerialized(boolean serialized) { this.serialized = serialized; }
}
