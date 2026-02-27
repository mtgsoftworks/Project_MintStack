package com.mintstack.finance.dto.response;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.PortfolioTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioTransactionResponse {

    private UUID id;

    private UUID portfolioId;

    private UUID instrumentId;

    private String instrumentSymbol;

    private String instrumentName;

    private Instrument.InstrumentType instrumentType;

    private PortfolioTransaction.TransactionType transactionType;

    private PortfolioTransaction.OrderType orderType;

    private PortfolioTransaction.OrderStatus orderStatus;

    private BigDecimal quantity;

    private BigDecimal filledQuantity;

    private BigDecimal remainingQuantity;

    private BigDecimal price;

    private BigDecimal averageFillPrice;

    private BigDecimal total;

    private BigDecimal grossTotal;

    private BigDecimal commissionAmount;

    private BigDecimal netTotal;

    private BigDecimal limitPrice;

    private BigDecimal stopPrice;

    private BigDecimal realizedProfitLoss;

    private String cancelReason;

    private LocalDate transactionDate;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime filledAt;
}
