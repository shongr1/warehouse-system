package com.warehouse.system.repository;

import com.warehouse.system.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findAllByOwner_PersonalNumber(String personalNumber);

}
