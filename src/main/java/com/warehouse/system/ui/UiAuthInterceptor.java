package com.warehouse.system.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UiAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String path = request.getRequestURI();

        // allow login routes
        if (path.startsWith("/ui/login")) return true;

        HttpSession session = request.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute(AuthController.SESSION_PN) != null);

        if (!loggedIn) {
            response.sendRedirect("/ui/login");
            return false;
        }

        return true;
    }
}
