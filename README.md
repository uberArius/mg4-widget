# MG4 Widget

Niezależna aplikacja na Androida z widżetem ekranu głównego dla samochodów MG obsługiwanych przez iSMART (regiony Europa/UK oraz Australia/Nowa Zelandia). Łączy się bezpośrednio z API iSMART — bez MQTT, bez własnego serwera i bez pośredników. Interfejs jest dostępny po polsku i angielsku.

*An independent Android app and home-screen widget for MG iSMART vehicles (EU/UK and AU/NZ). It talks directly to the iSMART API — no MQTT broker and no personal server. The UI is available in Polish and English.*

## Funkcje

- **Widżet ekranu głównego** — jeden konfigurowalny widżet „MG · morski” / „MG · sea-blue”: wybór i kolejność kafelków, ukrywanie ikon i przycisków, jedna lub dwie kolumny, przewijanie.
- **25 kafelków odczytów**, m.in. bateria, zasięg, temperatury, zamki, klimatyzacja, przebieg, napięcie 12 V, ładowanie (moc i pozostały czas), cel SOC, opony, drzwi/szyby, plan ładowania, maska, bagażnik, szyberdach, światła, hamulec postojowy i napęd.
- **Kafelek ładowania**: `MOC` (kW) i `KONIEC` (min), tytuł „Trwa ładowanie” (czerwony), „Nie ładuje” (niebieski) lub „Stan nieznany”.
- **Kafelek Bateria / Cel**: np. `56 / 80 (%)`.
- **Alarmy** — czerwone tło widżetu przy niskim ciśnieniu opon, odblokowanych zamkach lub otwartych drzwiach/szybach/masce/bagażniku, gdy napęd jest wyłączony.
- **Odświeżanie** — ręczne z widżetu, okresowe co 15–60 minut oraz po rozłączeniu wybranego urządzenia Bluetooth samochodu.
- **Profil ogrzewania** — temperatura 17–31°C, tryb szyby i podgrzewanie foteli; wynik polecenia jest weryfikowany na podstawie kolejnych odczytów.
- **Diagnostyka** — raport znanych pól pojazdu z surowych odpowiedzi API, bez haseł i tokenów.
- **Języki PL/EN** — automatyczny wybór po języku systemu lub ręczny przełącznik w menu.
- **Tryb demonstracyjny** — bez konta i bez sterowania autem; dane są oznaczone jako przykładowe.
- **Bezpieczeństwo** — dane logowania i tokeny szyfrowane lokalnie (AES-GCM / Android Keystore); brak haseł i tokenów w logach, kopii zapasowej i transferze danych aplikacji.

## Wymagania

- Android 12 lub nowszy (`minSdk 31`), `compileSdk`/`targetSdk` 36
- JDK 17+
- Android SDK — ścieżka w `local.properties` (klucz `sdk.dir`)
- Gradle 8.13 (wrapper w repozytorium)

## Budowanie

```powershell
./gradlew.bat :app:assembleDebug
```

Wynik: `app/build/outputs/apk/debug/app-debug.apk` (podpisany kluczem debug).

Testy jednostkowe:

```powershell
./gradlew.bat :app:testDebugUnitTest
```

Lint:

```powershell
./gradlew.bat :app:lintDebug
```

## Struktura repozytorium

- `app/` — kod aplikacji
- `publication/mg4-widget/` — publiczne materiały (opis, instalacja, ryzyka, licencje stron trzecich)
- `docs/` — notatki techniczne, walidacje i zrzuty ekranów
- `reference/` — zewnętrzne projekty referencyjne (nie są częścią aplikacji)
- `tools/` — skrypty pomocnicze

## Licencje i atrybucja

Kod i zasoby samej aplikacji są zastrzeżone — zobacz [LICENSE.md](LICENSE.md). Kod źródłowy znajduje się w katalogu `app/` i jest udostępniany do wglądu oraz oceny; nie udziela się licencji na jego kopiowanie ani modyfikowanie. Komponenty zewnętrzne pozostają na własnych licencjach; pełny wykaz w [THIRD_PARTY_NOTICES.txt](THIRD_PARTY_NOTICES.txt) oraz w `publication/mg4-widget/licenses/`.

Integracja adaptuje fragmenty [saic-python-client-ng](https://github.com/SAIC-iSmart-API/saic-python-client-ng) i [saic-python-mqtt-gateway](https://github.com/SAIC-iSmart-API/saic-python-mqtt-gateway) (licencje MIT; informacje zachowane).

## Zastrzeżenie

To niezależne, nieoficjalne i eksperymentalne oprogramowanie. Nie jest powiązane z MG ani SAIC i nie jest przez nie wspierane. Korzystanie oraz łączenie z iSMART odbywa się na własne ryzyko — przeczytaj [publication/mg4-widget/RISKS.md](publication/mg4-widget/RISKS.md).
