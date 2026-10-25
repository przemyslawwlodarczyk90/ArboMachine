-- Etap 51: opcjonalny termin nastepnej kontroli przy wpisie monitoringu (wyznacza arborysta).
alter table arbomachine.tree_monitoring_record add column if not exists next_inspection_date date;
