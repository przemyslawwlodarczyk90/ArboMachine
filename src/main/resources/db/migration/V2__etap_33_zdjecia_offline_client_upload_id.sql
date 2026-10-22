-- Etap 33: zdjecia offline - identyfikator uploadu z urzadzenia (ponowienie bez duplikatu).
-- IF NOT EXISTS: baza, na ktorej ddl-auto=update juz dodal kolumne, przechodzi migracje bez bledu.
alter table arbomachine.tree_photo add column if not exists client_upload_id varchar(36);
create unique index if not exists uk_tree_photo_tree_client_upload on arbomachine.tree_photo (tree_id, client_upload_id);
