package com.finance_tracker.utils.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VaultKeyContextTest {

    @AfterEach
    void tearDown() {
        VaultKeyContext.clear();
    }

    @Test
    void get_noKeySet_returnsNull() {
        assertThat(VaultKeyContext.get()).isNull();
    }

    @Test
    void set_thenGet_returnsKey() {
        VaultKeyContext.set("my-key");
        assertThat(VaultKeyContext.get()).isEqualTo("my-key");
    }

    @Test
    void clear_removesKey() {
        VaultKeyContext.set("my-key");
        VaultKeyContext.clear();
        assertThat(VaultKeyContext.get()).isNull();
    }

    @Test
    void isPresent_withKey_returnsTrue() {
        VaultKeyContext.set("my-key");
        assertThat(VaultKeyContext.isPresent()).isTrue();
    }

    @Test
    void isPresent_noKey_returnsFalse() {
        assertThat(VaultKeyContext.isPresent()).isFalse();
    }

    @Test
    void isPresent_blankKey_returnsFalse() {
        VaultKeyContext.set("   ");
        assertThat(VaultKeyContext.isPresent()).isFalse();
    }

    @Test
    void threadIsolation_eachThreadHasItsOwnKey() throws InterruptedException {
        VaultKeyContext.set("main-key");

        String[] threadValue = new String[1];
        Thread t = new Thread(() -> threadValue[0] = VaultKeyContext.get());
        t.start();
        t.join();

        assertThat(threadValue[0]).isNull();
        assertThat(VaultKeyContext.get()).isEqualTo("main-key");
    }
}
