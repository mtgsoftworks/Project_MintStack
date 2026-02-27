package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustPortfolioCashRequest {

    @NotNull(message = "Nakit islem tipi zorunludur")
    private CashAction action;

    @NotNull(message = "Tutar zorunludur")
    @DecimalMin(value = "0.000001", message = "Tutar pozitif olmalidir")
    private BigDecimal amount;

    @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
    private String notes;

    public enum CashAction {
        DEPOSIT,
        WITHDRAW
    }
}
