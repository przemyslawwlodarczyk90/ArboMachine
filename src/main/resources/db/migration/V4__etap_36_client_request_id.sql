-- Etap 36: identyfikator utworzenia z urzadzenia (kolejka offline) - drzewo, praca, kontrola stanu.
alter table arbomachine.tree add column if not exists client_request_id varchar(36);
alter table arbomachine.tree_work add column if not exists client_request_id varchar(36);
alter table arbomachine.tree_monitoring_record add column if not exists client_request_id varchar(36);
create unique index if not exists uk_tree_project_client_request on arbomachine.tree (project_id, client_request_id);
create unique index if not exists uk_tree_work_tree_client_request on arbomachine.tree_work (tree_id, client_request_id);
create unique index if not exists uk_tree_monitoring_tree_client_request on arbomachine.tree_monitoring_record (tree_id, client_request_id);
