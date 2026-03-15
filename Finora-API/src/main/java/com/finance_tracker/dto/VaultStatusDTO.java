package com.finance_tracker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VaultStatusDTO {

    private boolean vaultEnabled;

    private String vaultSalt;
}
