package com.finance_tracker.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class TokenController {

    @Autowired
    private JwtService jwtService;

    private String cachedToken;

    @PostConstruct
    public void generateTokenOnStartup() {
        cachedToken = jwtService.generateToken();
        System.out.println("AUTO-JWT generated on startup");
    }

    @GetMapping("/token")
    public String getToken() {
        return cachedToken;
    }
}
