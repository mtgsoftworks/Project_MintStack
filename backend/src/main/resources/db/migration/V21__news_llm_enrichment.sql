-- LLM enrichment metadata for RSS news pipeline.

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS llm_summary VARCHAR(1000);

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS llm_sentiment VARCHAR(32);

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS llm_keywords VARCHAR(500);

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS llm_model VARCHAR(120);

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS llm_enriched_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_news_llm_enriched_at ON news(llm_enriched_at DESC);
