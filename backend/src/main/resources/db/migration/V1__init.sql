CREATE TABLE `category` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `slug` varchar(50) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rss_source` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `language` varchar(10) NOT NULL,
  `name` varchar(100) NOT NULL,
  `url` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rss_source_url` (`url`),
  KEY `idx_rss_source_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `article` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `language` varchar(10) NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `source` varchar(100) NOT NULL,
  `source_url` varchar(1000) NOT NULL,
  `thumbnail_url` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `article_ingestion_job` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `state` varchar(32) NOT NULL,
  `url_hash` binary(32) NOT NULL,
  `article_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_ingestion_job_article` (`article_id`),
  UNIQUE KEY `uk_article_ingestion_job_url_hash` (`url_hash`),
  CONSTRAINT `fk_article_ingestion_job_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `article_ingestion_job_chk_1` CHECK ((`state` in (_utf8mb4'PENDING',_utf8mb4'PROCESSING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'RETRY_SCHEDULED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `article_summary` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content` text NOT NULL,
  `language` varchar(10) NOT NULL,
  `title` varchar(500) NOT NULL,
  `article_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_summary_article_language` (`article_id`,`language`),
  CONSTRAINT `fk_article_summary_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `article_like` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `ip_hash` char(64) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `article_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_like_article_ip` (`article_id`,`ip_hash`),
  KEY `idx_article_like_article_active` (`article_id`,`is_active`),
  CONSTRAINT `fk_article_like_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `article_comment` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `content` text NOT NULL,
  `ip_hash` char(64) NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `password_hash` char(60) NOT NULL,
  `article_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_article_comment_article_id` (`article_id`,`id`),
  KEY `fk_article_comment_parent` (`parent_id`),
  CONSTRAINT `fk_article_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_article_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `article_comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `article_category` (
  `id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `article_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_category_article_category` (`article_id`,`category_id`),
  KEY `fk_article_category_category` (`category_id`),
  CONSTRAINT `fk_article_category_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_article_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
