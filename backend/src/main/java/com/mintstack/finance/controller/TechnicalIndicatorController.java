package com.mintstack.finance.controller;

import com.mintstack.finance.dto.response.*;
import com.mintstack.finance.service.TechnicalIndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Teknik Analiz Göstergeleri API Controller
 */
@RestController
@RequestMapping("/api/v1/indicators")
@RequiredArgsConstructor
@Tag(name = "Technical Indicators", description = "Teknik analiz göstergeleri hesaplama")
@SecurityRequirement(name = "bearer")
public class TechnicalIndicatorController {

    private final TechnicalIndicatorService technicalIndicatorService;

    @GetMapping("/rsi/{symbol}")
    @Operation(summary = "RSI hesapla", description = "Relative Strength Index hesaplar (0-100 arası)")
    public ResponseEntity<ApiResponse<Double>> calculateRSI(
            @PathVariable @Parameter(description = "Enstrüman sembolü", example = "THYAO") String symbol,
            @RequestParam(defaultValue = "14") @Parameter(description = "Periyot", example = "14") int period) {
        
        Double rsi = technicalIndicatorService.calculateRSI(symbol, period);
        
        if (rsi == null) {
            return ResponseEntity.ok(ApiResponse.error("RSI hesaplanamadı - yeterli veri yok"));
        }
        
        String interpretation = interpretRSI(rsi);
        return ResponseEntity.ok(ApiResponse.success(rsi, interpretation));
    }

    @GetMapping("/macd/{symbol}")
    @Operation(summary = "MACD hesapla", description = "Moving Average Convergence Divergence hesaplar")
    public ResponseEntity<ApiResponse<MACDResult>> calculateMACD(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(defaultValue = "12") int fastPeriod,
            @RequestParam(defaultValue = "26") int slowPeriod,
            @RequestParam(defaultValue = "9") int signalPeriod) {
        
        MACDResult macd = technicalIndicatorService.calculateMACD(symbol, fastPeriod, slowPeriod, signalPeriod);
        
        if (macd == null) {
            return ResponseEntity.ok(ApiResponse.error("MACD hesaplanamadı - yeterli veri yok"));
        }
        
        String signal = macd.histogram() > 0 ? "Yükseliş Momentumu" : "Düşüş Momentumu";
        return ResponseEntity.ok(ApiResponse.success(macd, signal));
    }

    @GetMapping("/bollinger/{symbol}")
    @Operation(summary = "Bollinger Bands hesapla", description = "Bollinger bantlarını hesaplar")
    public ResponseEntity<ApiResponse<BollingerBandsResult>> calculateBollingerBands(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(defaultValue = "20") int period,
            @RequestParam(defaultValue = "2.0") double stdDev) {
        
        BollingerBandsResult bollinger = technicalIndicatorService.calculateBollingerBands(symbol, period, stdDev);
        
        if (bollinger == null) {
            return ResponseEntity.ok(ApiResponse.error("Bollinger Bands hesaplanamadı - yeterli veri yok"));
        }
        
        String interpretation = interpretBollinger(bollinger);
        return ResponseEntity.ok(ApiResponse.success(bollinger, interpretation));
    }

    @GetMapping("/sma/{symbol}")
    @Operation(summary = "SMA hesapla", description = "Simple Moving Average hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateSMA(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(defaultValue = "50") @Parameter(description = "Periyot (örn: 20, 50, 200)") int period) {
        
        Double sma = technicalIndicatorService.calculateSMA(symbol, period);
        
        if (sma == null) {
            return ResponseEntity.ok(ApiResponse.error("SMA hesaplanamadı - yeterli veri yok"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(sma, period + " günlük SMA"));
    }

    @GetMapping("/ema/{symbol}")
    @Operation(summary = "EMA hesapla", description = "Exponential Moving Average hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateEMA(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(defaultValue = "20") @Parameter(description = "Periyot") int period) {
        
        Double ema = technicalIndicatorService.calculateEMA(symbol, period);
        
        if (ema == null) {
            return ResponseEntity.ok(ApiResponse.error("EMA hesaplanamadı - yeterli veri yok"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(ema, period + " günlük EMA"));
    }

    @GetMapping("/stochastic/{symbol}")
    @Operation(summary = "Stochastic Oscillator hesapla", description = "%K ve %D değerlerini hesaplar")
    public ResponseEntity<ApiResponse<StochasticResult>> calculateStochastic(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol,
            @RequestParam(defaultValue = "14") int kPeriod,
            @RequestParam(defaultValue = "3") int dPeriod) {
        
        StochasticResult stochastic = technicalIndicatorService.calculateStochastic(symbol, kPeriod, dPeriod);
        
        if (stochastic == null) {
            return ResponseEntity.ok(ApiResponse.error("Stochastic hesaplanamadı - yeterli veri yok"));
        }
        
        String interpretation = interpretStochastic(stochastic);
        return ResponseEntity.ok(ApiResponse.success(stochastic, interpretation));
    }


    @GetMapping("/atr/{symbol}")
    @Operation(summary = "ATR hesapla", description = "Average True Range volatilite gostergesini hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateATR(
            @PathVariable @Parameter(description = "Enstruman sembolu") String symbol,
            @RequestParam(defaultValue = "14") int period) {
        Double atr = technicalIndicatorService.calculateATR(symbol, period);
        if (atr == null) {
            return ResponseEntity.ok(ApiResponse.error("ATR hesaplanamadi - yeterli veri yok"));
        }
        return ResponseEntity.ok(ApiResponse.success(atr, period + " periyot ATR"));
    }

    @GetMapping("/adx/{symbol}")
    @Operation(summary = "ADX hesapla", description = "Trend gucu gostergesini hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateADX(
            @PathVariable @Parameter(description = "Enstruman sembolu") String symbol,
            @RequestParam(defaultValue = "14") int period) {
        Double adx = technicalIndicatorService.calculateADX(symbol, period);
        if (adx == null) {
            return ResponseEntity.ok(ApiResponse.error("ADX hesaplanamadi - yeterli veri yok"));
        }
        return ResponseEntity.ok(ApiResponse.success(adx, period + " periyot ADX"));
    }

    @GetMapping("/obv/{symbol}")
    @Operation(summary = "OBV hesapla", description = "On-Balance Volume hesaplar")
    public ResponseEntity<ApiResponse<Long>> calculateOBV(
            @PathVariable @Parameter(description = "Enstruman sembolu") String symbol,
            @RequestParam(defaultValue = "90") int limit) {
        Long obv = technicalIndicatorService.calculateOBV(symbol, limit);
        if (obv == null) {
            return ResponseEntity.ok(ApiResponse.error("OBV hesaplanamadi - yeterli veri yok"));
        }
        return ResponseEntity.ok(ApiResponse.success(obv, "OBV"));
    }

    @GetMapping("/vwap/{symbol}")
    @Operation(summary = "VWAP hesapla", description = "Volume Weighted Average Price hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateVWAP(
            @PathVariable @Parameter(description = "Enstruman sembolu") String symbol,
            @RequestParam(defaultValue = "20") int period) {
        Double vwap = technicalIndicatorService.calculateVWAP(symbol, period);
        if (vwap == null) {
            return ResponseEntity.ok(ApiResponse.error("VWAP hesaplanamadi - hacim verisi yok veya yetersiz"));
        }
        return ResponseEntity.ok(ApiResponse.success(vwap, period + " periyot VWAP"));
    }

    @GetMapping("/cci/{symbol}")
    @Operation(summary = "CCI hesapla", description = "Commodity Channel Index hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateCCI(
            @PathVariable @Parameter(description = "Enstruman sembolu") String symbol,
            @RequestParam(defaultValue = "20") int period) {
        Double cci = technicalIndicatorService.calculateCCI(symbol, period);
        if (cci == null) {
            return ResponseEntity.ok(ApiResponse.error("CCI hesaplanamadi - yeterli veri yok"));
        }
        return ResponseEntity.ok(ApiResponse.success(cci, period + " periyot CCI"));
    }

    @GetMapping("/mfi/{symbol}")
    @Operation(summary = "MFI hesapla", description = "Money Flow Index hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateMFI(
            @PathVariable @Parameter(description = "Enstruman sembolu") String symbol,
            @RequestParam(defaultValue = "14") int period) {
        Double mfi = technicalIndicatorService.calculateMFI(symbol, period);
        if (mfi == null) {
            return ResponseEntity.ok(ApiResponse.error("MFI hesaplanamadi - yeterli veri yok"));
        }
        return ResponseEntity.ok(ApiResponse.success(mfi, period + " periyot MFI"));
    }

    @GetMapping("/williams-r/{symbol}")
    @Operation(summary = "Williams %R hesapla", description = "Williams Percent Range hesaplar")
    public ResponseEntity<ApiResponse<Double>> calculateWilliamsR(
            @PathVariable @Parameter(description = "Enstruman sembolu") String symbol,
            @RequestParam(defaultValue = "14") int period) {
        Double williamsR = technicalIndicatorService.calculateWilliamsR(symbol, period);
        if (williamsR == null) {
            return ResponseEntity.ok(ApiResponse.error("Williams %R hesaplanamadi - yeterli veri yok"));
        }
        return ResponseEntity.ok(ApiResponse.success(williamsR, period + " periyot Williams %R"));
    }
    @GetMapping("/all/{symbol}")
    @Operation(summary = "Tüm göstergeleri hesapla", 
               description = "RSI, MACD, Bollinger, SMA, EMA, Stochastic - hepsini tek seferde")
    public ResponseEntity<ApiResponse<TechnicalIndicatorsResult>> calculateAll(
            @PathVariable @Parameter(description = "Enstrüman sembolü") String symbol) {
        
        TechnicalIndicatorsResult result = technicalIndicatorService.calculateAllIndicators(symbol);
        
        String message = "Genel Sinyal: " + result.overallSignal();
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    // =================== HELPER METHODS ===================

    private String interpretRSI(Double rsi) {
        if (rsi < 30) {
            return "Aşırı Satım (Oversold) - Alım fırsatı olabilir";
        } else if (rsi < 40) {
            return "Zayıf Bölge";
        } else if (rsi <= 60) {
            return "Nötr Bölge";
        } else if (rsi <= 70) {
            return "Güçlü Bölge";
        } else {
            return "Aşırı Alım (Overbought) - Satış düşünülebilir";
        }
    }

    private String interpretBollinger(BollingerBandsResult bollinger) {
        if (bollinger.percentB() < 0) {
            return "Fiyat alt bandın altında - Aşırı satım";
        } else if (bollinger.percentB() < 20) {
            return "Fiyat alt banda yakın";
        } else if (bollinger.percentB() <= 80) {
            return "Fiyat bantlar arasında - Normal aralık";
        } else if (bollinger.percentB() <= 100) {
            return "Fiyat üst banda yakın";
        } else {
            return "Fiyat üst bandın üstünde - Aşırı alım";
        }
    }

    private String interpretStochastic(StochasticResult stochastic) {
        return switch (stochastic.signal()) {
            case "OVERSOLD" -> "Aşırı Satım - Alım fırsatı";
            case "OVERBOUGHT" -> "Aşırı Alım - Satış düşünülebilir";
            case "BULLISH" -> "Yükseliş sinyali";
            case "BEARISH" -> "Düşüş sinyali";
            default -> "Nötr";
        };
    }
}

