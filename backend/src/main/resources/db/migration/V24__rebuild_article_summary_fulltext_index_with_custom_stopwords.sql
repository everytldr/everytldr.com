-- Rebuild article_summary FULLTEXT index with a custom InnoDB stopword table.
--
-- Rebuild article_summary FULLTEXT index with a custom InnoDB stopword table.
-- The custom stopword table keeps default InnoDB stopwords except 'a' and 'i',
-- so ngram tokens such as 'ai' are not filtered out.
-- Ensure the MySQL server is configured to use this table before running this migration.

CREATE TABLE IF NOT EXISTS `innodb_fulltext_stopwords` (
    `value` VARCHAR(30) NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

INSERT IGNORE INTO `innodb_fulltext_stopwords` (`value`) VALUES
                                                             ('about'),
                                                             ('an'),
                                                             ('are'),
                                                             ('as'),
                                                             ('at'),
                                                             ('be'),
                                                             ('by'),
                                                             ('com'),
                                                             ('de'),
                                                             ('en'),
                                                             ('for'),
                                                             ('from'),
                                                             ('how'),
                                                             ('in'),
                                                             ('is'),
                                                             ('it'),
                                                             ('la'),
                                                             ('of'),
                                                             ('on'),
                                                             ('or'),
                                                             ('that'),
                                                             ('the'),
                                                             ('this'),
                                                             ('to'),
                                                             ('was'),
                                                             ('what'),
                                                             ('when'),
                                                             ('where'),
                                                             ('who'),
                                                             ('will'),
                                                             ('with'),
                                                             ('und'),
                                                             ('www');

ALTER TABLE `article_summary`
    DROP INDEX `ftx_article_summary_title_content`;

ALTER TABLE `article_summary`
    ADD FULLTEXT INDEX `ftx_article_summary_title_content` (`title`, `content`) WITH PARSER ngram;