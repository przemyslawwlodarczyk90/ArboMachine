-- Etap 35: czas, sposob i urzadzenie pomiaru lokalizacji drzewa.
alter table arbomachine.tree add column if not exists location_measured_at timestamp(6) with time zone;
alter table arbomachine.tree add column if not exists location_adapter_code varchar(40);
alter table arbomachine.tree add column if not exists location_device_label varchar(100);
