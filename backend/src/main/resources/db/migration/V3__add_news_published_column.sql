-- V3__add_news_published_column.sql
-- Add is_published column to news table

ALTER TABLE news ADD COLUMN IF NOT EXISTS is_published BOOLEAN DEFAULT true;
