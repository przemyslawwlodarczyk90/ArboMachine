-- Etap 37: wersja danych drzewa (wykrywanie konfliktu edycji). Istniejace drzewa dostaja 0.
alter table arbomachine.tree add column if not exists data_version bigint default 0 not null;
