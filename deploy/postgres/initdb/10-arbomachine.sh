#!/bin/sh
# Wspolny PostgreSQL 18.6: rola i baza ArboMachine. Baze tworzy INFRASTRUKTURA (ten skrypt), schemat "arbomachine"
# i tabele tworzy aplikacja migracjami Flyway przy starcie. Bez uprawnien do baz innych aplikacji (np. arnocrm).
#
# Uruchamianie:
#  - automatycznie: skopiowany do /docker-entrypoint-initdb.d/ wspolnego kontenera postgres (tylko przy PIERWSZEJ
#    inicjalizacji pustego wolumenu danych),
#  - recznie na dzialajacym serwerze:
#      docker exec -i -e ARBOMACHINE_DB_PASSWORD=... postgres sh < deploy/postgres/initdb/10-arbomachine.sh
# Wymagane: ARBOMACHINE_DB_PASSWORD (to samo co DB_PASSWORD aplikacji). Opcjonalnie ARBOMACHINE_DB_NAME,
# ARBOMACHINE_DB_USERNAME (domyslnie arbomachine). Haslo nigdy nie trafia do repozytorium.
set -eu
: "${ARBOMACHINE_DB_PASSWORD:?ustaw ARBOMACHINE_DB_PASSWORD}"
database_name="${ARBOMACHINE_DB_NAME:-arbomachine}"
database_user="${ARBOMACHINE_DB_USERNAME:-arbomachine}"

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" --dbname postgres \
     -v db_name="$database_name" -v db_user="$database_user" -v db_password="$ARBOMACHINE_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION', :'db_user', :'db_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'db_user') \gexec
SELECT format('CREATE DATABASE %I OWNER %I ENCODING ''UTF8'' TEMPLATE template0', :'db_name', :'db_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'db_name') \gexec
-- Tylko wlasciciel (i administrator) moze sie laczyc z baza ArboMachine.
REVOKE ALL ON DATABASE :"db_name" FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE :"db_name" TO :"db_user";
-- Limit polaczen roli (pula aplikacji to 4) - jedna aplikacja nie wyczerpie max_connections wspolnego serwera.
ALTER ROLE :"db_user" CONNECTION LIMIT 10;
\connect :"db_name"
-- Schemat public nieuzywany przez aplikacje; nikt poza wlascicielem nie tworzy w nim obiektow.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SQL
