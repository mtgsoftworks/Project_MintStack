package com.mintstack.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistResponse {

    private UUID id;
    private String name;
    private String description;
    private Boolean isDefault;
    private Integer itemCount;
    private List<WatchlistItemResponse> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchlistItemResponse {
        private UUID id;
        private String symbol;
        private String name;
        private String type;
        private BigDecimal currentPrice;
        private BigDecimal previousClose;
        private LocalDateTime addedAt;

        public BigDecimal getChange() {
            if (currentPrice != null && previousClose != null) {
                return currentPrice.subtract(previousClose);
            }
            return null;
        }

        public BigDecimal getChangePercent() {
            if (currentPrice != null && previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0) {
                return currentPrice.subtract(previousClose)
                        .divide(previousClose, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            return null;
        }
    }
}
