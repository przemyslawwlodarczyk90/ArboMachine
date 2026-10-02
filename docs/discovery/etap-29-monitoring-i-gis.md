# Etap 29 — monitoring stanu drzew, grupy drzew, import i eksport GIS

**Status:** V1 zaimplementowane. Pytania i założenia: `docs/DO_DOPRECYZOWANIA.md`, sekcje „Monitoring stanu drzew”,
„Grupy drzew i GIS”, „Dane zlecenia projektu”.

Etapy 29 i 30 zrealizowano razem, łącząc dwa polecenia: (A) monitoring, powiadomienia, kalendarz, offline, eksporty
i PDF oraz (B) import/eksport GIS (SHP, GPKG, DXF), obszary drzew („porosty”), symbole iglastych/liściastych, dane
zlecenia i finalizacja MVP. Ten dokument opisuje część danych (monitoring, grupy drzew, GIS, dane zlecenia),
`etap-30-integracje-i-finalizacja.md` — powiadomienia, kalendarz, wydanie i podsumowanie etapów 27–30.

## Monitoring stanu drzew

| Hipoteza | Realizacja V1 |
|---|---|
| Arborysta wraca do drzew i odnotowuje kontrole | `TreeMonitoringRecord`: data kontroli (wymagana), ocena opcjonalna, uwagi (do 5000 znaków); wiele kontroli na drzewo, najnowsza pierwsza |
| Potrzebna ogólna ocena stanu | Dobry / Umiarkowany / Zły (`GOOD` / `FAIR` / `POOR`), brak = „Nie oceniono”. **Skala umowna, nie VTA, nie klasa ryzyka** |
| Ważna jest ostatnia kontrola | Plakietka na liście drzew i w karcie, filtr „Stan” (w tym „Nie sprawdzone”), `GET …/monitoring/latest` |
| Zły stan wymaga reakcji | Zapis kontroli z oceną „Zły” → powiadomienie właściciela (etap 30), po zatwierdzeniu transakcji |
| Kontrole w terenie bez sieci | Kolejka offline etapu 26: `CREATE/UPDATE/DELETE_TREE_MONITORING_RECORD`, tymczasowe ujemne id |
| Kontrole w dokumencie i eksporcie | PDF: liczba drzew skontrolowanych / w złym stanie, tabela kontroli w karcie drzewa; XLSX: arkusz „Monitoring”; GIS: `mon_date`, `mon_status` |

API: `GET/POST /api/projects/{p}/trees/{t}/monitoring`, `GET/PUT/DELETE …/monitoring/{recordId}`,
`GET /api/projects/{p}/monitoring/latest`. Cudze drzewo / projekt → 404.

### Karta oceny stanu z pliku projektu arborysty (do discovery)

Plik projektu aplikacji terenowej, z której arborysta korzysta dziś, zawiera strukturalną kartę drzewa. To
najlepszy dostępny materiał do rozmowy o docelowym modelu oceny stanu (V1 go **nie** implementuje):

- **Wartość drzewa:** pomnik przyrody, gatunek chroniony, ostoja, inne.
- **Liczba przewodników:** 1–10. **Kształt korony:** regularna / asymetryczna (odrosty z pnia, zdeformowana cięciem).
- **Odziomek, pień:** pochylony, pęknięcia, słabe wiązanie, owocniki grzybów, rozkład drewna, pędy regeneracyjne,
  rany, ubytek powierzchniowy / wgłębny (mały / średni / rozległy), ubytek kominowy, rozwidlenie V / U.
- **Konary, gałęzie:** posusz 5–100%, jemioła, gniazda. **Korzenie, siedlisko, notatki.**

W arkuszu XLSX z tej aplikacji kolumny karty są łączone przy imporcie w pole „Stan / uwagi” drzewa.

## Grupy drzew (obszary)

W poleceniu „porosty”; w interfejsie **„Grupy drzew”** (kod: `TreeStand`, tabela `tree_stand`).

| Hipoteza | Realizacja V1 |
|---|---|
| Część drzew opisuje się zbiorczo (szpaler, aleja, zadrzewienie) | Grupa = nazwa, uwagi, obszar (wielokąt WGS 84 bez otworów), przypisane drzewa (drzewo może być w kilku grupach) |
| Obszar rysuje się na mapie albo przychodzi z rysunku | Zakładka „Grupy drzew”: rysowanie wielokąta na mapie (OpenLayers), import z DXF / SHP / GPKG / GeoJSON |
| Drzewa w obszarze przypisuje się automatycznie | „Przypisz drzewa leżące w obszarze” (punkt-w-wielokącie) + ręczny wybór; przy imporcie automatycznie |
| Grupa ma powierzchnię | Pole liczone na sferze (m², w UI m² / ha) |
| Grupy widać na mapie i w dokumentach | Warstwa grup na mapie projektu (przełącznik, nazwy bez nakładania), filtr listy drzew, tabela w PDF, arkusz XLSX, warstwa `stands` w GPKG i SHP, `stands.geojson` |

## Symbol iglastych

Na mapie **gwiazdka = gatunek iglasty**, koło = liściasty lub nieznany. Rozpoznanie po rodzaju łacińskim (Pinus,
Picea, Abies, Larix, Taxus, Thuja…) albo nazwie polskiej (sosna, świerk, jodła, modrzew, cis, żywotnik…) —
`SpeciesGroupClassifier` (backend, atrybut `sp_group` w eksporcie) i `speciesGroup.ts` (frontend). Filtr listy
drzew „Iglaste / Liściaste”.

## Import i eksport GIS

| Format | Import drzew | Import grup | Eksport |
|---|---|---|---|
| Shapefile (ZIP: `.shp`, `.dbf`, `.prj`, `.cpg`) | punkty | wielokąty | `shapefile.zip`: warstwy `trees` + `stands`, WGS 84, UTF-8 |
| GeoPackage | warstwy punktowe | warstwy wielokątów | `trees.gpkg`: warstwy `trees` + `stands` |
| GeoJSON | punkty | Polygon / MultiPolygon | `trees.geojson`, `stands.geojson` |
| DXF (ASCII) | warstwy `-Point` (+ numery z `-Label`) | warstwy `-Area` | — |
| XLSX / CSV | wiersze (także arkusze aplikacji terenowej) | — | `trees.xlsx` (Drzewa, Prace, Monitoring, Grupy drzew, Projekt), CSV |

- **Układy współrzędnych:** PL-2000 strefy 5–8 (EPSG 2176–2179) i PL-1992 (EPSG 2180) rozpoznawane z `.prj`, SRS
  GeoPackage albo z zakresu liczb; przeliczane do WGS 84 (szereg Krügera, zgodność z PROJ do 1e-8°). Import
  ostrzega „współrzędne przeliczone”. Eksport zawsze w WGS 84.
- **DXF:** numery drzew nie są atrybutem punktu, tylko osobnym tekstem obok. Dopasowanie globalne (koszt = odległość
  od linii bazowej tekstu z uwzględnieniem kierunku odnośnika i szerokości tekstu). Na pliku arborysty: 836 z 839
  poprawnie, 1 bez numeru — import zawsze pokazuje ostrzeżenie „numery odczytane z etykiet rysunku”. Okręgi pnia i
  korony z rysunku nie są importowane.
- **Arkusz aplikacji terenowej:** aliasy nagłówków (Etykieta, Gatunki (łac.), Gatunki, Obwody na 130 cm, Pozycja
  WGS84 - szerokość / długość, Karta drzewa), wiersze obszarów pomijane z ostrzeżeniem, obwody wielopniowe
  („57+34”, „48, 27”) przenoszone do uwag bez sumowania.
- Limity: plik 15 MB, „wszystko albo nic”, najpierw walidacja z podglądem.

Sprawdzone na prawdziwych plikach arborysty: 938 drzew z XLSX, 13 grup z DXF (105 drzew przypisanych automatycznie),
9 drzew iglastych; eksporty otwarte w GDAL 3.12.4 (SHP 938 punktów / 14 wielokątów, GPKG, GeoJSON).

## Dane zlecenia projektu

Projekt ma opcjonalne pola **Zleceniodawca**, **Adres obiektu**, **Data zlecenia** (`client_name`, `site_address`,
`order_date`). Widoczne w nagłówku projektu, dialogach tworzenia/edycji, PDF (strona tytułowa) i XLSX (arkusz
„Projekt”). PATCH: pominięte pole = bez zmian, pusty tekst = usunięcie wartości.
