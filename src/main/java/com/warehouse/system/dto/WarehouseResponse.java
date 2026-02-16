package com.warehouse.system.dto;

public class WarehouseResponse {

    private Long id;
    private String name;
    private String location;

    private String ownerFullName;
    private String ownerPersonalNumber;

    public WarehouseResponse() {}

    public WarehouseResponse(Long id, String name, String location,
                             String ownerFullName, String ownerPersonalNumber) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.ownerFullName = ownerFullName;
        this.ownerPersonalNumber = ownerPersonalNumber;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getOwnerPersonalNumber() {
        return ownerPersonalNumber;
    }
}
