package com.mintstack.finance.dto.request;

import com.mintstack.finance.entity.PriceAlert.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertRequest {

    @NotBlank(message = "Enstrüman sembolü boş olamaz")
    private String symbol;

    @NotNull(message = "Alarm tipi belirtilmelidir")
    private AlertType alertType;

    @NotNull(message = "Hedef değer belirtilmelidir")
    @Positive(message = "Hedef değer pozitif olmalıdır")
    private BigDecimal targetValue;

    private String notes;
}
