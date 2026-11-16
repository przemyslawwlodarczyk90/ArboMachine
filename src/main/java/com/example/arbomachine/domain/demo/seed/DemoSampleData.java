package com.example.arbomachine.domain.demo.seed;

import java.util.List;
import java.util.Random;

/**
 * Slowniki do danych pokazowych: gatunki z typowymi zakresami pomiarow i krotkie, realistyczne opisy arborystyczne.
 * Wszystkie nazwy obiektow, zleceniodawcow i adresy w trybie demo sa fikcyjne (miejscowosc "Zielonowo").
 */
final class DemoSampleData {

    record Species(String polish, String latin, boolean conifer, int minimumCircumferenceCm, int maximumCircumferenceCm,
                   double maximumHeightM) {}

    static final Species LIPA_DROBNOLISTNA = new Species("lipa drobnolistna", "Tilia cordata", false, 90, 380, 28);
    static final Species LIPA_SZEROKOLISTNA = new Species("lipa szerokolistna", "Tilia platyphyllos", false, 110, 360, 30);
    static final Species DAB_SZYPULKOWY = new Species("dąb szypułkowy", "Quercus robur", false, 120, 520, 30);
    static final Species KLON_ZWYCZAJNY = new Species("klon zwyczajny", "Acer platanoides", false, 40, 260, 24);
    static final Species KLON_JAWOR = new Species("klon jawor", "Acer pseudoplatanus", false, 70, 300, 26);
    static final Species JESION = new Species("jesion wyniosły", "Fraxinus excelsior", false, 90, 330, 30);
    static final Species KASZTANOWIEC = new Species("kasztanowiec zwyczajny", "Aesculus hippocastanum", false, 120, 380, 22);
    static final Species BRZOZA = new Species("brzoza brodawkowata", "Betula pendula", false, 50, 210, 24);
    static final Species GRAB = new Species("grab pospolity", "Carpinus betulus", false, 50, 220, 20);
    static final Species BUK = new Species("buk zwyczajny", "Fagus sylvatica", false, 90, 360, 30);
    static final Species TOPOLA = new Species("topola czarna", "Populus nigra", false, 150, 480, 30);
    static final Species WIERZBA = new Species("wierzba biała", "Salix alba", false, 120, 420, 22);
    static final Species OLSZA = new Species("olsza czarna", "Alnus glutinosa", false, 60, 220, 22);
    static final Species ROBINIA = new Species("robinia akacjowa", "Robinia pseudoacacia", false, 70, 260, 20);
    static final Species JARZAB = new Species("jarząb pospolity", "Sorbus aucuparia", false, 30, 140, 12);
    static final Species PLATAN = new Species("platan klonolistny", "Platanus × hispanica", false, 150, 420, 30);
    static final Species SOSNA = new Species("sosna zwyczajna", "Pinus sylvestris", true, 70, 260, 28);
    static final Species SWIERK = new Species("świerk pospolity", "Picea abies", true, 60, 230, 30);
    static final Species MODRZEW = new Species("modrzew europejski", "Larix decidua", true, 80, 250, 30);
    static final Species CIS = new Species("cis pospolity", "Taxus baccata", true, 40, 190, 12);
    static final Species ZYWOTNIK = new Species("żywotnik zachodni", "Thuja occidentalis", true, 40, 150, 12);
    static final Species DAGLEZJA = new Species("daglezja zielona", "Pseudotsuga menziesii", true, 90, 260, 30);

    static final List<String> GOOD_CONDITION = List.of(
            "Korona symetryczna, dobrze ulistniona. Pień bez widocznych uszkodzeń.",
            "Drzewo w dobrej kondycji, przyrost roczny prawidłowy.",
            "Pojedyncze suche gałęzie w dolnej części korony, bez znaczenia dla statyki.",
            "Kora bez spękań, nasada pnia bez oznak rozkładu.");
    static final List<String> FAIR_CONDITION = List.of(
            "Posusz do 15% korony, głównie w części wierzchołkowej.",
            "Niewielki ubytek kory od strony chodnika (uszkodzenie mechaniczne).",
            "Korona jednostronna, przegęszczona; zalecane cięcie prześwietlające.",
            "Zgrubienie i pęknięcie kory w rozwidleniu przewodników - do obserwacji.",
            "Przerzedzenie korony i mniejsze liście niż u sąsiednich drzew.");
    static final List<String> POOR_CONDITION = List.of(
            "Rozległy ubytek wgłębny u podstawy pnia, owocniki grzyba na korzeniach.",
            "Posusz powyżej 50% korony, odpadająca kora na pniu.",
            "Pęknięcie pnia w rozwidleniu (rozwidlenie typu V z wrośniętą korą).",
            "Drzewo zamierające - brak ulistnienia w górnej części korony.");
    static final List<String> HAZARDS = List.of(
            "Suche konary nad ciągiem pieszym - ryzyko wyłamania.",
            "Pochylenie drzewa w stronę jezdni, uniesiona płyta korzeniowa.",
            "Ubytek grożący złamaniem pnia przy silnym wietrze.",
            "Gałęzie kolidujące z linią napowietrzną.");
    static final List<String> FIELD_NOTES = List.of(
            "Pomiar obwodu na wysokości 130 cm.",
            "Dostęp od strony alejki.",
            "Drzewo z tabliczką inwentaryzacyjną z poprzedniej inwentaryzacji.",
            "Pień wielokrotny - podano obwód najgrubszego przewodnika.",
            "W sąsiedztwie ławki i kosza.",
            "");

    private DemoSampleData() {
    }

    static <T> T pick(Random random, List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    @SafeVarargs
    static <T> T pick(Random random, T... values) {
        return values[random.nextInt(values.length)];
    }
}
