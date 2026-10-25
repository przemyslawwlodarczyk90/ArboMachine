-- Etap 52: podpis zdjecia w dokumencie i wybor "w dokumencie" (domyslnie tak - stare zdjecia zostaja w PDF).
alter table arbomachine.tree_photo add column if not exists caption varchar(500);
alter table arbomachine.tree_photo add column if not exists included_in_document boolean not null default true;
