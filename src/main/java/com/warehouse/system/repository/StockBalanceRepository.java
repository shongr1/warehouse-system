package com.warehouse.system.repository;

import com.warehouse.system.entity.StockBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByWarehouseIdAndItemTypeId(Long warehouseId, Long itemTypeId);

    // already good: get all stocks that belong to the logged-in user (by personalNumber)
    List<StockBalance> findByWarehouseOwnerPersonalNumber(String personalNumber);

    List<StockBalance> findByWarehouseId(Long warehouseId);

    // NEW: same idea but by ownerId (sometimes nicer than personalNumber)
    List<StockBalance> findAllByWarehouse_Owner_Id(Long ownerId);
}
