-- Tylko test: baza "innej aplikacji" (jak arnocrm) na tym samym serwerze - ArboMachine nie moze sie do niej podlaczyc.
CREATE ROLE arnocrm LOGIN PASSWORD 'other-application-test-password';
CREATE DATABASE arnocrm OWNER arnocrm;
REVOKE ALL ON DATABASE arnocrm FROM PUBLIC;
