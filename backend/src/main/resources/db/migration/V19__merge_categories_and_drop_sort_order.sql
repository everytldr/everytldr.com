UPDATE `category` SET `slug` = CONCAT('society-', `slug`) WHERE `slug` = 'rights' OR `slug` LIKE 'rights-%';
UPDATE `category` SET `slug` = CONCAT('society-', `slug`) WHERE `slug` = 'media' OR `slug` LIKE 'media-%';
UPDATE `category` SET `slug` = CONCAT('society-', `slug`) WHERE `slug` = 'education' OR `slug` LIKE 'education-%';
UPDATE `category` SET `slug` = CONCAT('technology-', `slug`) WHERE `slug` = 'science' OR `slug` LIKE 'science-%';

ALTER TABLE `category` DROP COLUMN `sort_order`;
