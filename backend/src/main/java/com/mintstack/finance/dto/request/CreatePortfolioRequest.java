package com.mintstack.finance.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class CreatePortfolioRequest {

    @NotBlank(message = "Portfoy adi zorunludur")
    @Size(min = 2, max = 100, message = "Portfoy adi 2-100 karakter arasinda olmalidir")
    private String name;

    @Size(max = 500, message = "Aciklama en fazla 500 karakter olabilir")
    private String description;

    private Boolean isDefault;

    @DecimalMin(value = "0.000000", message = "Baslangic nakit bakiyesi negatif olamaz")
    private BigDecimal initialCashBalance;

    @DecimalMin(value = "0.000000", message = "Komisyon orani negatif olamaz")
    @DecimalMax(value = "0.100000", message = "Komisyon orani en fazla %10 olabilir")
    private BigDecimal commissionRate;

    @DecimalMin(value = "0.000000", message = "Minimum komisyon negatif olamaz")
    private BigDecimal minimumCommissionAmount;

    @DecimalMin(value = "0.000000", message = "Komisyon vergisi negatif olamaz")
    @DecimalMax(value = "0.300000", message = "Komisyon vergisi en fazla %30 olabilir")
    private BigDecimal commissionTaxRate;
}
