package com.finance_tracker.utils.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
public class VaultKeyFilter extends OncePerRequestFilter {

    private static final String VAULT_KEY_HEADER = "X-Vault-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String vaultKeyHeader = request.getHeader(VAULT_KEY_HEADER);
            if (vaultKeyHeader != null && !vaultKeyHeader.isBlank()) {
                VaultKeyContext.set(vaultKeyHeader);
            }
            chain.doFilter(request, response);
        } finally {
            VaultKeyContext.clear();
        }
    }
}
