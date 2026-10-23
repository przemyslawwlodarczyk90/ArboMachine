package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.entity.Tree;

/**
 * Punkt rozszerzenia dla modulow zaleznych od drzewa (zdjecia, prace, waloryzacja, kontrole, oznaczenia, grupy):
 * usuniecie ich danych tuz przed usunieciem drzewa, w tej samej transakcji (etap 44). Modul drzew nie zna tych
 * modulow - kazdy sam sprzata swoje rekordy (a pliki dopiero po commicie).
 */
public interface TreeDataCleaner {

    void deleteDataOf(Tree treeToDelete);
}
