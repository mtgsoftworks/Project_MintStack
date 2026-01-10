package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Watchlist item entity - links instruments to watchlists
 */
@Entity
@Table(name = "watchlist_items", indexes = {
    @Index(name = "idx_watchlist_items_watchlist", columnList = "watchlist_id"),
    @Index(name = "idx_watchlist_items_instrument", columnList = "instrument_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"watchlist_id", "instrument_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchlistItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_id", nullable = false)
    private Watchlist watchlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "added_at")
    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();

    @Column(name = "notes")
    private String notes;
}
