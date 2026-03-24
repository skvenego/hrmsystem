package com.hrm.hrmsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA fallback controller - forwards client-side routes to index.html
 * so React Router can handle them when users navigate directly or refresh.
 */
@Controller
public class SpaFallbackController {

    @GetMapping({"/login", "/register"})
    public String forwardAuthToIndex() {
        return "forward:/index.html";
    }

    @GetMapping("/dashboard/**")
    public String forwardDashboardToIndex() {
        return "forward:/index.html";
    }
}
