package com.finance_tracker.utils.security;

public final class VaultKeyContext {

    private static final ThreadLocal<String> VAULT_KEY = new ThreadLocal<>();

    private VaultKeyContext() {
    }

    public static void set(String vaultKey) {
        VAULT_KEY.set(vaultKey);
    }

    public static String get() {
        return VAULT_KEY.get();
    }

    public static void clear() {
        VAULT_KEY.remove();
    }

    public static boolean isPresent() {
        String key = VAULT_KEY.get();
        return key != null && !key.isBlank();
    }
}
