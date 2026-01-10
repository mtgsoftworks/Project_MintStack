package com.mintstack.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Watchlist entity for tracking favorite instruments
 */
@Entity
@Table(name = "watchlists", indexes = {
    @Index(name = "idx_watchlists_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Watchlist extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WatchlistItem> items = new ArrayList<>();

    /**
     * Add an instrument to the watchlist
     */
    public void addItem(Instrument instrument) {
        WatchlistItem item = WatchlistItem.builder()
                .watchlist(this)
                .instrument(instrument)
                .build();
        items.add(item);
    }

    /**
     * Remove an instrument from the watchlist
     */
    public void removeItem(Instrument instrument) {
        items.removeIf(item -> item.getInstrument().equals(instrument));
    }
}
