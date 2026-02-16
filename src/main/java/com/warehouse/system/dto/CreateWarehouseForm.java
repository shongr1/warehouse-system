package com.warehouse.system.dto;

public class CreateWarehouseForm {

    private String name;
    private String location;
    private String ownerPersonalNumber;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOwnerPersonalNumber() { return ownerPersonalNumber; }
    public void setOwnerPersonalNumber(String ownerPersonalNumber) { this.ownerPersonalNumber = ownerPersonalNumber; }
}
