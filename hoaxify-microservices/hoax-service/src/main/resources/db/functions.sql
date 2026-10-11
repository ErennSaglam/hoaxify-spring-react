-- Monolith'teki fonksiyonun aynısı (bkz. ws/src/main/resources/db/functions.sql)
CREATE OR REPLACE FUNCTION hoaxify_user_hoax_count(p_user_id BIGINT)
RETURNS BIGINT
LANGUAGE sql
STABLE
AS $$
    SELECT COUNT(*) FROM hoax WHERE user_id = p_user_id;
$$;;
