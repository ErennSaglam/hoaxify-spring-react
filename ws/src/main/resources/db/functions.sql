-- Veritabanı tarafında yaşayan iş mantığı (bankalardaki stored procedure'lerin PostgreSQL karşılığı).
-- Uygulama açılırken Hibernate tabloları oluşturduktan SONRA çalışır
-- (spring.jpa.defer-datasource-initialization=true). CREATE OR REPLACE sayesinde her açılışta güvenle tekrar çalışır.
-- Komut ayırıcı ";;" (application.properties: spring.sql.init.separator), çünkü fonksiyon gövdesinde ";" var.

CREATE OR REPLACE FUNCTION hoaxify_user_hoax_count(p_user_id BIGINT)
RETURNS BIGINT
LANGUAGE sql
STABLE
AS $$
    SELECT COUNT(*) FROM hoax WHERE user_id = p_user_id;
$$;;
