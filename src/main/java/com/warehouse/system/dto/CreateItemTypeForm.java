package com.warehouse.system.dto;

import com.warehouse.system.entity.KitComponent;
import java.util.ArrayList;
import java.util.List;

public class CreateItemTypeForm {

    private String catalogNumber;
    private String name;
    private boolean serialized;

    // שדה חדש: האם זו ערכה
    private boolean kit;

    // שדה חדש: רשימת הרכיבים שמתקבלת מהטופס
    private List<KitComponent> components = new ArrayList<>();

    // Getters / Setters
    public boolean isKit() { return kit; }
    public void setKit(boolean kit) { this.kit = kit; }

    public List<KitComponent> getComponents() { return components; }
    public void setComponents(List<KitComponent> components) { this.components = components; }

    // שאר ה-Getters וה-Setters הקיימים שלך...
    public String getCatalogNumber() { return catalogNumber; }
    public void setCatalogNumber(String catalogNumber) { this.catalogNumber = catalogNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isSerialized() { return serialized; }
    public void setSerialized(boolean serialized) { this.serialized = serialized; }
}