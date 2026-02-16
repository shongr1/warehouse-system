package com.warehouse.system.controller;

public record CreateWarehouseRequest(
        String name,
        String location,
        String ownerPersonalNumber
) {}
