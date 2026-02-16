package com.warehouse.system.repository;

import com.warehouse.system.entity.User;
import com.warehouse.system.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByPersonalNumber(String personalNumber);
    Optional<User> findByPersonalNumber(String personalNumber);
    List<User> findAllByRole(UserRole role);


}
