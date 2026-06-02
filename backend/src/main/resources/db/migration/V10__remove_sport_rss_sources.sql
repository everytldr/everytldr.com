-- Remove seeded sport sources (The Guardian Football, BBC Sport).
-- IDs match the fixed Snowflake seed IDs from V5 and V7.
DELETE FROM `article_source`
WHERE `id` IN (45660871069790209, 45660871069790210);
