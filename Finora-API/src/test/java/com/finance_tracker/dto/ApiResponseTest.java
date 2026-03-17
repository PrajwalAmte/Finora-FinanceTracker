package com.finance_tracker.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_data_setsSuccessTrue() {
        var resp = ApiResponse.success("payload");
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo("payload");
        assertThat(resp.getMessage()).isNull();
        assertThat(resp.getTimestamp()).isNotNull();
    }

    @Test
    void success_messageAndData_setsMessageAndData() {
        var resp = ApiResponse.success("done", 42);
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("done");
        assertThat(resp.getData()).isEqualTo(42);
        assertThat(resp.getTimestamp()).isNotNull();
    }

    @Test
    void error_message_setsSuccessFalse() {
        ApiResponse<Void> resp = ApiResponse.error("fail");
        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("fail");
        assertThat(resp.getErrorCode()).isNull();
    }

    @Test
    void error_messageAndCode_setsErrorCode() {
        ApiResponse<Void> resp = ApiResponse.error("fail", "NOT_FOUND");
        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(resp.getTimestamp()).isNotNull();
    }
}
