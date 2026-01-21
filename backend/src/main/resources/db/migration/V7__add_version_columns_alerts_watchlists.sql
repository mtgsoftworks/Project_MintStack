-- V7__add_version_columns_alerts_watchlists.sql
-- Add missing optimistic lock version columns for BaseEntity-backed tables

ALTER TABLE IF EXISTS price_alerts
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE IF EXISTS watchlists
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE IF EXISTS watchlist_items
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
