package com.warehouse.system.repository;

import com.warehouse.system.entity.TransferRequest;
import com.warehouse.system.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    boolean existsByItemIdAndStatus(Long itemId, TransferStatus status);
    List<TransferRequest> findByToUserIdAndStatus(Long toUserId, TransferStatus status);

}
