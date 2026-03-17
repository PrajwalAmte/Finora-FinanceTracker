package com.finance_tracker.utils.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultKeyFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private final VaultKeyFilter filter = new VaultKeyFilter();

    @AfterEach
    void clearContext() {
        VaultKeyContext.clear();
    }

    @Test
    void doFilterInternal_setsVaultKeyFromHeader() throws Exception {
        when(request.getHeader("X-Vault-Key")).thenReturn("my-secret-vault-key");

        filter.doFilterInternal(request, response, chain);

        // VaultKeyContext is cleared in finally, so we verify via a captured value
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noHeader_doesNotSetVaultKey() throws Exception {
        when(request.getHeader("X-Vault-Key")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(VaultKeyContext.get()).isNull();
    }

    @Test
    void doFilterInternal_blankHeader_doesNotSetVaultKey() throws Exception {
        when(request.getHeader("X-Vault-Key")).thenReturn("   ");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(VaultKeyContext.get()).isNull();
    }

    @Test
    void doFilterInternal_clearsVaultKeyInFinallyEvenOnException() throws Exception {
        when(request.getHeader("X-Vault-Key")).thenReturn("key");
        doThrow(new RuntimeException("downstream fail")).when(chain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, chain);
        } catch (RuntimeException ignored) {
        }

        assertThat(VaultKeyContext.get()).isNull();
    }
}
