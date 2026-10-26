# Description / Opis

## ENG

New in 0.6.0: the app dashboard always displays all 28 selectable tiles in two columns, with refresh and heating below the list. Widget tile selection remains independent. If the last reported drive state is OFF and any tyre is below 2.4 bar, the locks are unlocked or any door/window (including sunroof when reported), bonnet or boot is open, the outer widget/dashboard background turns red. The rule uses all stored readings, including hidden tiles. Exactly 2.4 bar is not below the threshold. Missing drive status is not assumed OFF. Cached readings can be old: check their timestamps; this is not a live safety monitor.

MG4 Widget is an independent Android application focused on a configurable home-screen widget. It connects directly to the selected iSMART region and vehicle. No MQTT or additional server is needed.

Choose from 28 tiles and arrange enabled tiles by dragging or using arrows. Combined tiles show both temperatures, four tyre pressures, four doors, four door windows and front-seat heating levels. Tyres, doors and windows use a physical 2×2 layout: front at the top, left on the left. Set the correct steering-wheel side for UK/Australia. The charging-plan tile shows the mode and saved hours when returned by the vehicle; a target-SOC mode does not promise a fixed end time.

The widget adapts to one or two columns. In one column only “MG” remains in the header. Icons and the refresh/heating buttons can be hidden. Unlocking, active climate control and charging are highlighted using received data. Missing or stale readings are not treated as current zero values.

Refresh can be manual, periodic (0 = off; otherwise 15–60 minutes) or triggered by disconnection of a selected paired car Bluetooth device. Bluetooth is a clue that a trip ended, not proof that the driver left. Android may delay background work. Active polling may wake the car and drain its 12 V battery faster.

The heating button opens a confirmation screen. A saved profile can include cabin temperature, windscreen demisting and front-seat heating where supported. Capability detection is incomplete; unknown seat support requires an explicit user test option. No automatic command probing is performed. Command acceptance alone is not proof of operation: subsequent vehicle readings are checked. A timeout does not prove the command failed.

A diagnostic scan reads five known vehicle-response groups and reports values or missing/error responses for 175 raw fields plus interpreted readings. Availability varies by model, region and account permissions. It does not claim to enumerate every service or account function provided by MG.

This test build expires on **25 November 2026 at 00:00 UTC**. The main UI is Polish. Actual vehicle-data retrieval and region/device compatibility still require field testing. Read [risks and responsibility](RISKS.md) before use.

## PL

Nowość 0.6.0: panel aplikacji zawsze pokazuje wszystkie 28 dostępnych kafelków w dwóch kolumnach, z odświeżaniem i ogrzewaniem pod listą. Wybór kafelków widżetu pozostaje niezależny. Gdy ostatnio odczytany napęd jest wyłączony i dowolna opona ma poniżej 2,4 bar, zamki są odblokowane lub otwarte są dowolne drzwi/szyba (także szyberdach, jeśli raportowany), maska albo bagażnik, zewnętrzne tło widżetu/panelu staje się czerwone. Reguła obejmuje wszystkie zapisane odczyty, także ukryte kafelki. Dokładnie 2,4 bar nie uruchamia alarmu. Brak stanu napędu nie jest uznawany za wyłączenie. Dane mogą być stare: sprawdzaj ich czas; nie jest to monitor bezpieczeństwa działający w czasie rzeczywistym.

MG4 Widget to niezależna aplikacja Android skoncentrowana na konfigurowalnym widżecie pulpitu. Łączy się bezpośrednio z wybranym regionem iSMART i samochodem. Nie wymaga MQTT ani dodatkowego serwera.

Wybierz spośród 28 kafelków i ustaw kolejność przeciąganiem lub strzałkami. Zbiorcze kafelki obejmują obie temperatury, cztery ciśnienia opon, cztery drzwi, cztery szyby drzwiowe i poziomy ogrzewania przednich foteli. Opony, drzwi i szyby mają układ 2×2: przód u góry, lewa strona po lewej. Dla UK/Australii ustaw właściwą stronę kierownicy. Plan ładowania pokazuje tryb i zapisane godziny, jeśli auto je zwraca; tryb docelowego SOC nie oznacza stałej godziny końca.

Widżet dopasowuje się do jednej lub dwóch kolumn. Przy jednej kolumnie w nagłówku zostaje tylko „MG”. Ikony i przyciski odświeżania/ogrzewania można ukryć. Odblokowane zamki, aktywny klimat i ładowanie są wyróżniane na podstawie odczytów. Brakujące lub stare dane nie są traktowane jako aktualne zero.

Odświeżanie może być ręczne, okresowe (0 = wyłączone; pozostałe wartości 15–60 minut) lub po rozłączeniu wybranego sparowanego BT auta. Rozłączenie jest wskazówką końca jazdy, nie dowodem wysiadania. Android może opóźnić pracę w tle. Aktywne odczyty mogą budzić auto i szybciej zużywać akumulator 12 V.

Przycisk ogrzewania otwiera ekran potwierdzenia. Profil może obejmować temperaturę kabiny, nawiew na szybę i ogrzewanie przednich foteli, jeśli są obsługiwane. Wykrywanie wyposażenia nie jest pełne; nieznana obsługa foteli wymaga jawnego włączenia opcji testu. Nie wysyłamy automatycznych poleceń próbnych. Przyjęcie komendy nie jest potwierdzeniem działania: aplikacja sprawdza późniejszy odczyt. Timeout nie dowodzi niewykonania komendy.

Diagnostyka odczytuje pięć znanych grup odpowiedzi pojazdu i pokazuje wyniki lub braki/błędy dla 175 surowych pól oraz interpretowanych odczytów. Dostępność zależy od modelu, regionu i uprawnień konta. Nie jest to lista wszystkich usług i funkcji konta MG.

Wersja wygasa **25 listopada 2026 o 01:00 czasu polskiego (00:00 UTC)**. Główny interfejs jest po polsku. Pobieranie danych rzeczywistego auta i zgodność z regionami/urządzeniami wymagają dalszych testów. Przed użyciem przeczytaj [ostrzeżenia i odpowiedzialność](RISKS.md).
