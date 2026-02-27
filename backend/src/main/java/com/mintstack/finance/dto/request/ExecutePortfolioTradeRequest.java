package com.mintstack.finance.dto.request;

import com.mintstack.finance.entity.PortfolioTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePortfolioTradeRequest {

    private UUID instrumentId;

    @Size(max = 20, message = "Enstruman sembolu en fazla 20 karakter olabilir")
    private String instrumentSymbol;

    @NotNull(message = "Islem tipi zorunludur")
    private PortfolioTransaction.TransactionType transactionType;

    @NotNull(message = "Miktar zorunludur")
    @Positive(message = "Miktar pozitif olmalidir")
    @DecimalMin(value = "0.000001", message = "Miktar en az 0.000001 olmalidir")
    private BigDecimal quantity;

    @Positive(message = "Fiyat pozitif olmalidir")
    private BigDecimal price;

    @Builder.Default
    private PortfolioTransaction.OrderType orderType = PortfolioTransaction.OrderType.MARKET;

    @Positive(message = "Limit fiyat pozitif olmalidir")
    private BigDecimal limitPrice;

    @Positive(message = "Stop fiyat pozitif olmalidir")
    private BigDecimal stopPrice;

    @PastOrPresent(message = "Islem tarihi bugun veya gecmiste olmalidir")
    private LocalDate transactionDate;

    @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
    private String notes;
}
