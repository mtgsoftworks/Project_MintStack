package com.mintstack.finance.service.portfolio;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioFinancialRulesServiceTest {

    private final PortfolioFinancialRulesService service = new PortfolioFinancialRulesService();

    @Test
    void normalizePositiveOrDefault_ShouldReturnDefault_WhenNull() {
        BigDecimal result = service.normalizePositiveOrDefault(null, new BigDecimal("100.000000"));

        assertThat(result).isEqualByComparingTo("100.000000");
    }

    @Test
    void normalizePositiveOrDefault_ShouldThrow_WhenNegative() {
        assertThatThrownBy(() -> service.normalizePositiveOrDefault(new BigDecimal("-1"), BigDecimal.ONE))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void calculateCommission_ShouldApplyDefaults_WhenPortfolioRatesAreNull() {
        Portfolio portfolio = Portfolio.builder().build();
        Instrument instrument = Instrument.builder().type(Instrument.InstrumentType.STOCK).build();

        BigDecimal result = service.calculateCommission(portfolio, instrument, new BigDecimal("500"));

        assertThat(result).isEqualByComparingTo("1.050000");
    }

    @Test
    void calculateCommission_ShouldApplyInstrumentMultiplier_ForViop() {
        Portfolio portfolio = Portfolio.builder()
            .commissionRate(new BigDecimal("0.001000"))
            .minimumCommissionAmount(new BigDecimal("1.000000"))
            .commissionTaxRate(new BigDecimal("0.050000"))
            .build();
        Instrument instrument = Instrument.builder().type(Instrument.InstrumentType.VIOP).build();

        BigDecimal result = service.calculateCommission(portfolio, instrument, new BigDecimal("10000"));

        assertThat(result).isEqualByComparingTo("12.600000");
    }

    @Test
    void ensureSufficientCash_ShouldThrow_WhenCashIsInsufficient() {
        Portfolio portfolio = Portfolio.builder()
            .cashBalance(new BigDecimal("99"))
            .build();

        assertThatThrownBy(() -> service.ensureSufficientCash(portfolio, new BigDecimal("100")))
            .isInstanceOf(BadRequestException.class);
    }
}
