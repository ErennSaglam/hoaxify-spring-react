#!/bin/bash
# PostgreSQL container'ı İLK kez açılırken bir kez çalışır (veri klasörü boşsa).
# "Database per service": her servisin kendi veritabanı VE kendi kullanıcısı var.
# auth_svc kullanıcısı userdb'ye bağlanamaz; servisler birbirinin verisine sadece API ile ulaşır.
set -euo pipefail

create_service_db() {
  local service="$1" password="$2"
  echo "Creating database ${service}db owned by ${service}_svc"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE USER ${service}_svc WITH PASSWORD '${password}';
    CREATE DATABASE ${service}db OWNER ${service}_svc;
    REVOKE ALL ON DATABASE ${service}db FROM PUBLIC;
EOSQL
}

create_service_db auth "$AUTH_DB_PASSWORD"
create_service_db user "$USER_DB_PASSWORD"
create_service_db hoax "$HOAX_DB_PASSWORD"
