package com.warehouse.system.repository;

import com.warehouse.system.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByItemType_IdAndSerialNumber(Long itemTypeId, String serialNumber);

}
