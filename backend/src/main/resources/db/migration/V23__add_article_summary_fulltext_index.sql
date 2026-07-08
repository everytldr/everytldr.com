-- ngram parser tokenizes CJK text into overlapping n-grams so Korean summaries become
-- searchable via MATCH ... AGAINST; the default whitespace parser cannot split Korean.
ALTER TABLE `article_summary`
    ADD FULLTEXT INDEX `ftx_article_summary_title_content` (`title`, `content`) WITH PARSER ngram;
