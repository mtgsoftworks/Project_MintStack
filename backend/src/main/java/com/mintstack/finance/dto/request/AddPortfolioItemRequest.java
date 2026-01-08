package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.*;
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
public class AddPortfolioItemRequest {

    @NotNull(message = "Enstrüman ID zorunludur")
    private UUID instrumentId;

    @NotNull(message = "Miktar zorunludur")
    @Positive(message = "Miktar pozitif olmalıdır")
    @DecimalMin(value = "0.000001", message = "Miktar en az 0.000001 olmalıdır")
    private BigDecimal quantity;

    @NotNull(message = "Alış fiyatı zorunludur")
    @Positive(message = "Alış fiyatı pozitif olmalıdır")
    private BigDecimal purchasePrice;

    @NotNull(message = "Alış tarihi zorunludur")
    @PastOrPresent(message = "Alış tarihi bugün veya geçmişte olmalıdır")
    private LocalDate purchaseDate;

    @Size(max = 500, message = "Not en fazla 500 karakter olabilir")
    private String notes;
}
