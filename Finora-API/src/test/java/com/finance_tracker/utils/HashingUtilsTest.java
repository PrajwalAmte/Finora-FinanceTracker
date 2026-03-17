package com.finance_tracker.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashingUtilsTest {

    private HashingUtils hashingUtils;

    @BeforeEach
    void setUp() {
        hashingUtils = new HashingUtils();
    }

    // ── toCanonicalJson ───────────────────────────────────────────────────────

    @Test
    void toCanonicalJson_nullValue_returnsNullString() {
        assertThat(hashingUtils.toCanonicalJson(null)).isEqualTo("null");
    }

    @Test
    void toCanonicalJson_simpleString_returnsJsonString() {
        String result = hashingUtils.toCanonicalJson("hello");
        assertThat(result).isEqualTo("\"hello\"");
    }

    @Test
    void toCanonicalJson_map_returnsSortedKeys() {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        map.put("z", 2);
        map.put("a", 1);

        String result = hashingUtils.toCanonicalJson(map);

        assertThat(result.indexOf("\"a\"")).isLessThan(result.indexOf("\"z\""));
    }

    @Test
    void toCanonicalJson_integer_returnsNumericString() {
        assertThat(hashingUtils.toCanonicalJson(42)).isEqualTo("42");
    }

    // ── computeHash ───────────────────────────────────────────────────────────

    @Test
    void computeHash_producesHexString() {
        String hash = hashingUtils.computeHash(
                "EXPENSE", "1", "CREATE",
                "null", "{\"amount\":100}",
                "2024-01-01T00:00:00Z",
                null, "1", 1);

        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void computeHash_sameInputs_producesSameHash() {
        String h1 = hashingUtils.computeHash("E", "1", "C", "null", "after", "ts", null, "user", 1);
        String h2 = hashingUtils.computeHash("E", "1", "C", "null", "after", "ts", null, "user", 1);

        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void computeHash_differentInputs_produceDifferentHashes() {
        String h1 = hashingUtils.computeHash("E", "1", "C", "null", "after-A", "ts", null, "user", 1);
        String h2 = hashingUtils.computeHash("E", "1", "C", "null", "after-B", "ts", null, "user", 1);

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void computeHash_nullPrevHash_usesEmptyString() {
        String hashWithNull = hashingUtils.computeHash("E", "1", "C", "n", "a", "t", null, "u", 1);
        String hashWithEmpty = hashingUtils.computeHash("E", "1", "C", "n", "a", "t", "", "u", 1);

        assertThat(hashWithNull).isNotNull();
        assertThat(hashWithEmpty).isNotNull();
    }

    @Test
    void computeHash_withPrevHash_chainedCorrectly() {
        String prev = "prevhash";
        String hash1 = hashingUtils.computeHash("E", "1", "C", "n", "a", "t", prev, "u", 1);
        String hash2 = hashingUtils.computeHash("E", "1", "C", "n", "a", "t", null, "u", 1);

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
