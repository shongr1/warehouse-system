package com.warehouse.system.ui;

import com.warehouse.system.entity.UserRole;
import com.warehouse.system.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui")
public class AuthController {

    public static final String SESSION_PN = "SESSION_PN";
    public static final String SESSION_ROLE = "SESSION_ROLE";

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "ui-login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String personalNumber,
                          HttpSession session,
                          RedirectAttributes ra) {

        var userOpt = userRepository.findByPersonalNumber(personalNumber.trim());
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found for personal number: " + personalNumber);
            return "redirect:/ui/login";
        }

        var user = userOpt.get();
        session.setAttribute(SESSION_PN, user.getPersonalNumber());
        session.setAttribute(SESSION_ROLE, user.getRole().name());

        return "redirect:/ui";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/ui/login";
    }

    // helpers
    public static String currentPn(HttpSession session) {
        return (String) session.getAttribute(SESSION_PN);
    }

    public static boolean isLoggedIn(HttpSession session) {
        String pn = currentPn(session);
        return pn != null && !pn.isBlank();
    }

    public static boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute(SESSION_ROLE);
        return UserRole.ADMIN.name().equals(role);
    }


    public static void requireLogin(HttpSession session) {
        if (!isLoggedIn(session)) {
            throw new RuntimeException("Not logged in");
        }
    }
}
