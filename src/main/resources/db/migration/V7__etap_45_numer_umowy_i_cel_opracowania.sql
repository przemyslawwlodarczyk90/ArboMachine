-- Etap 45: numer umowy / zlecenia i cel opracowania w projekcie (opcjonalne).
alter table arbomachine.project add column if not exists contract_number varchar(100);
alter table arbomachine.project add column if not exists document_purpose text;
