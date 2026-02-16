package com.warehouse.system.controller;

import com.warehouse.system.entity.User;
import com.warehouse.system.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;





@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @GetMapping
    public List<User> getAll() {
        return userRepository.findAll();
    }
    @PostMapping
    public User create(@RequestBody UserCreateRequest req) {

        if (userRepository.existsByPersonalNumber(req.personalNumber())) {
            throw new IllegalArgumentException("personalNumber already exists: " + req.personalNumber());
        }

        User u = new User();
        u.setPersonalNumber(req.personalNumber());
        u.setFullName(req.fullName());
        u.setPasswordHash(encoder.encode(req.password()));

        return userRepository.save(u);
    }

    // DTO פנימי בשביל לא ליצור עכשיו package חדש
    public record UserCreateRequest(String personalNumber, String fullName, String password) {}
}
