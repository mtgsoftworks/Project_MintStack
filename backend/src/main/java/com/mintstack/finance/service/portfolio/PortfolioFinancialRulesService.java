package com.mintstack.finance.service.portfolio;

import com.mintstack.finance.entity.Instrument;
import com.mintstack.finance.entity.Portfolio;
import com.mintstack.finance.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PortfolioFinancialRulesService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.001000");
    private static final BigDecimal DEFAULT_MINIMUM_COMMISSION = new BigDecimal("1.000000");
    private static final BigDecimal DEFAULT_COMMISSION_TAX_RATE = new BigDecimal("0.050000");
    private static final BigDecimal MAX_COMMISSION_RATE = new BigDecimal("0.100000");
    private static final BigDecimal MAX_COMMISSION_TAX_RATE = new BigDecimal("0.300000");

    public BigDecimal getDefaultCommissionRate() {
        return DEFAULT_COMMISSION_RATE;
    }

    public BigDecimal getDefaultMinimumCommission() {
        return DEFAULT_MINIMUM_COMMISSION;
    }

    public BigDecimal getDefaultCommissionTaxRate() {
        return DEFAULT_COMMISSION_TAX_RATE;
    }

    public BigDecimal normalizePositiveOrDefault(BigDecimal value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Deger negatif olamaz");
        }
        return value;
    }

    public BigDecimal normalizeCommissionRate(BigDecimal commissionRate) {
        if (commissionRate == null) {
            return DEFAULT_COMMISSION_RATE;
        }
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(MAX_COMMISSION_RATE) > 0) {
            throw new BadRequestException("Komisyon orani 0 ile 0.10 arasinda olmalidir");
        }
        return commissionRate;
    }

    public BigDecimal normalizeCommissionTaxRate(BigDecimal taxRate) {
        if (taxRate == null) {
            return DEFAULT_COMMISSION_TAX_RATE;
        }
        if (taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(MAX_COMMISSION_TAX_RATE) > 0) {
            throw new BadRequestException("Komisyon vergisi 0 ile 0.30 arasinda olmalidir");
        }
        return taxRate;
    }

    public BigDecimal calculateCommission(Portfolio portfolio, Instrument instrument, BigDecimal grossTotal) {
        BigDecimal rate = normalizeCommissionRate(portfolio.getCommissionRate());
        BigDecimal minimumCommission = normalizePositiveOrDefault(portfolio.getMinimumCommissionAmount(), DEFAULT_MINIMUM_COMMISSION);
        BigDecimal taxRate = normalizeCommissionTaxRate(portfolio.getCommissionTaxRate());
        BigDecimal effectiveRate = rate.multiply(resolveInstrumentCommissionMultiplier(instrument.getType()));

        BigDecimal baseCommission = safe(grossTotal).multiply(effectiveRate).setScale(6, RoundingMode.HALF_UP);
        BigDecimal commission = baseCommission.max(minimumCommission);
        BigDecimal tax = commission.multiply(taxRate).setScale(6, RoundingMode.HALF_UP);
        return commission.add(tax).setScale(6, RoundingMode.HALF_UP);
    }

    public void ensureSufficientCash(Portfolio portfolio, BigDecimal requiredAmount) {
        BigDecimal availableCash = safe(portfolio.getCashBalance());
        if (availableCash.compareTo(requiredAmount) < 0) {
            throw new BadRequestException(
                "Yetersiz nakit bakiye. Gerekli: " + requiredAmount.stripTrailingZeros().toPlainString()
                    + ", Mevcut: " + availableCash.stripTrailingZeros().toPlainString()
            );
        }
    }

    private BigDecimal resolveInstrumentCommissionMultiplier(Instrument.InstrumentType type) {
        if (type == null) {
            return BigDecimal.ONE;
        }
        return switch (type) {
            case STOCK -> BigDecimal.ONE;
            case VIOP -> new BigDecimal("1.20");
            case CRYPTO -> new BigDecimal("1.50");
            case FUND -> new BigDecimal("0.80");
            case BOND -> new BigDecimal("0.60");
            default -> BigDecimal.ONE;
        };
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
