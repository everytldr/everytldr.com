CREATE TABLE `briefing` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `briefing_date` date NOT NULL,
  `language` varchar(10) NOT NULL,
  `title` varchar(500) NOT NULL,
  `content` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_briefing_date_language` (`briefing_date`,`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `briefing_article` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `briefing_date` date NOT NULL,
  `article_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_briefing_article_date_article` (`briefing_date`,`article_id`),
  CONSTRAINT `fk_briefing_article_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
