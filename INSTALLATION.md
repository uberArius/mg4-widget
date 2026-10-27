# Installation and use / Instalacja i użytkowanie

## ENG

### Before installing

Read [RISKS.md](RISKS.md). This is unofficial experimental software distributed **outside Google Play**. Installing an APK requires you to accept the risks of sideloading. Download only from this repository's [release page](https://github.com/uberArius/mg4-widget/releases/tag/v0.7.4). Do not disable Play Protect or other security checks to work around a warning you do not understand.

### Install

1. On an Android 12+ phone, download `MG4-Widget-0.7.4-debug.apk` from the release assets. GitHub's “Source code” ZIP is **not** the installer.
2. Open the APK. If Android blocks installation from that browser/file manager, review the warning and, only if you accept it, enable **Allow from this source** for that specific app in system settings. Install the APK. You can turn that permission off again afterwards. Device or organization restrictions may prevent installation.
3. Open MG4 Widget, read the first-use notice, tick **I accept the risks** and press **Continue**. Without acceptance, iSMART requests and periodic refresh are blocked. Existing testers must also accept the notice after updating to this version.
4. The build is debug-signed for testing. Install updates over the existing app when signed with the same key; do not uninstall first unless you intend to erase local account data. Connections stop on **25 November 2026 at 00:00 UTC**, regardless of installation date.

### Connect and test

1. Prefer an **additional iSMART account** with access to a vehicle shared through the official MG app. Using the main account may log out the official app or cause session conflicts. Shared-account permissions vary and may exclude remote control.
2. Select the account region: Europe/UK or Australia/NZ. Enter email and password. The phone country-code field is used only for phone-number login; 48 means Poland and is not sent for email login. Passwords are masked; **Pokaż hasło** reveals them temporarily.
3. Choose the vehicle. For the first test, set **Aktualizacje i Bluetooth** to periodic refresh **0** and Bluetooth refresh off. Press **Odśwież** once. Stage 1 reads vehicle status; stage 2 reads charging data. Each stage can take up to 3 minutes. Vehicle status is displayed as soon as stage 1 completes. Do not interpret a timeout as proof that a command was not executed.
4. Use **Sprawdź wszystkie parametry auta** for a local diagnostic report. It can take up to 8 minutes, may wake the car and can show unavailable fields. It does not send control commands.
5. Tap a tile in the main app dashboard to select or deselect it for the widget. Selected tiles show ✓ and their position; new selections go to the top. Hold a selected tile and drag it onto another to reorder it; dragging onto an unselected tile moves it to the end of the selected group. Changes save immediately. Unselected readings remain below the selected group. Add the widget via the app or launcher and resize/scroll it as needed.
6. The widget's **Odśwież** button queues a background read without opening the app. **Ogrzewanie** opens a separate confirmation screen; configure the profile before explicitly starting it. Check the resulting car state. Start with cabin heating alone, then test optional demisting/seats individually on a parked car.
7. For trip-end refresh, pair the phone with the car, select that exact device, grant **Nearby devices**, and enable **Aktualizuj bezpośrednio po rozłączeniu BT**. Other Bluetooth devices do not trigger it. Reconnection cancels pending trip-end retries.
8. After successful tests, choose periodic refresh (15–60 minutes). **0 + Bluetooth off** means no automatic vehicle polling, except verification after a command you explicitly start. Frequent reads may wake the car and increase 12 V battery drain.

### Background operation on Android / Samsung

The app uses Android WorkManager. Internet access is required; pending reads wait for connectivity. Manual refresh requests priority, but Android workload/quotas, battery saver, Doze and manufacturer restrictions may delay execution. The app cannot grant itself unrestricted background execution.

In **Aktualizacje i Bluetooth**, review the reported Android restriction/optimization state and use **Ustawienia systemowe aplikacji**. If updates are delayed, consider **App info → Battery → Unrestricted**. On Samsung, also check **Settings → Battery (or Device care → Battery) → Background usage limits**: remove MG4 Widget from sleeping/deep-sleeping apps and add it to never-sleeping apps. Names vary by One UI version. This may increase phone battery use. The app cannot reliably detect every Samsung-specific setting. Reopen the app after force-stopping it. Physical BT, Android 17 and Fold-specific background behavior still need field testing.

### Reporting a problem

Post the app version, phone/Android version, region, vehicle model, elapsed time and redacted error text. Never post passwords, tokens, account emails, VINs or coordinates. Login has been reported working, but reliable vehicle readings are still being tested. Do not repeatedly submit commands when a result is unknown.

## PL

### Przed instalacją

Przeczytaj [RISKS.md](RISKS.md). To nieoficjalna aplikacja eksperymentalna, udostępniana **poza Google Play**. Instalacja APK wymaga świadomego zaakceptowania ryzyka instalacji spoza sklepu. Pobieraj plik wyłącznie ze [strony wydania](https://github.com/uberArius/mg4-widget/releases/tag/v0.7.4). Nie wyłączaj Play Protect ani innych zabezpieczeń, aby ominąć niezrozumiałe ostrzeżenie.

### Instalacja

1. Na telefonie z Androidem 12 lub nowszym pobierz `MG4-Widget-0.7.4-debug.apk` z załączników wydania. Archiwum „Source code” na GitHubie **nie jest** instalatorem.
2. Otwórz APK. Jeśli Android blokuje instalację z tej przeglądarki/menedżera plików, przeczytaj ostrzeżenie i tylko po akceptacji ryzyka włącz **Zezwalaj z tego źródła** dla tej konkretnej aplikacji. Zainstaluj APK. Później możesz ponownie wyłączyć to uprawnienie. Ograniczenia telefonu lub organizacji mogą uniemożliwiać instalację.
3. Uruchom MG4 Widget, przeczytaj komunikat, zaznacz **Akceptuję ryzyko** i naciśnij **Przejdź do aplikacji**. Bez akceptacji nie są wysyłane żądania iSMART ani planowane odczyty okresowe. Dotychczasowi testerzy również zobaczą ten ekran po aktualizacji.
4. Wersja jest podpisana kluczem debug do testów. Kolejne wersje z tym samym podpisem instaluj jako aktualizacje; odinstalowanie usuwa dane lokalne. Połączenia wygasają **25 listopada 2026 o 01:00 czasu polskiego (00:00 UTC)**, niezależnie od daty instalacji.

### Połączenie i pierwsze testy

1. Używaj **dodatkowego konta iSMART** z autem udostępnionym przez oficjalną aplikację MG. Konto główne może powodować wzajemne wylogowywanie i konflikt sesji. Uprawnienia konta dodatkowego mogą nie obejmować sterowania.
2. Wybierz region konta: Europa/UK albo Australia/NZ. Wpisz e-mail i hasło. Kod kraju dotyczy wyłącznie logowania numerem telefonu: 48 to Polska; przy e-mailu kod nie jest wysyłany. Hasło jest zamaskowane; **Pokaż hasło** pozwala je podejrzeć.
3. Wybierz auto. Na pierwszy test ustaw w **Aktualizacje i Bluetooth** interwał **0** i wyłącz odświeżanie po BT. Naciśnij **Odśwież** raz. Etap 1 odczytuje stan samochodu, etap 2 dane ładowania; każdy może trwać do 3 minut. Stan auta jest pokazywany od razu po etapie 1. Timeout nie dowodzi niewykonania komendy.
4. **Sprawdź wszystkie parametry auta** tworzy lokalny raport. Może trwać do 8 minut, budzić auto i pokazywać brak części pól. Nie wysyła poleceń sterujących.
5. Dotknij kafelka w głównym panelu aplikacji, aby wybrać go do widżetu lub usunąć z wyboru. Wybrane mają ✓ i numer pozycji; nowo wybrane trafiają na początek. Przytrzymaj wybrany kafelek i przeciągnij na inny, aby zmienić kolejność; upuszczenie na niewybranym przenosi na koniec wybranej grupy. Zmiany zapisują się od razu. Niewybrane odczyty pozostają pod wybraną grupą. Dodaj widżet z aplikacji lub menu pulpitu, zmieniaj jego rozmiar i przewijaj zawartość.
6. **Odśwież** na widżecie zleca odczyt w tle bez otwierania aplikacji. **Ogrzewanie** otwiera ekran dodatkowego potwierdzenia. Ustaw profil przed świadomym uruchomieniem i sprawdź stan auta. Najpierw testuj samą kabinę, później osobno szybę/fotele, na zaparkowanym samochodzie.
7. Dla odczytu po jeździe sparuj telefon z autem, wybierz dokładnie jego BT, zezwól na **Urządzenia w pobliżu** i włącz **Aktualizuj bezpośrednio po rozłączeniu BT**. Inne urządzenia nie wywołują odczytu. Ponowne połączenie anuluje oczekujące ponowienia po jeździe.
8. Po udanych testach ustaw okresowe odświeżanie (15–60 minut). **0 + BT wyłączone** oznacza brak samoczynnego odpytywania auta, poza sprawdzeniem wyniku polecenia uruchomionego przez Ciebie. Częste odczyty mogą budzić auto i zwiększać zużycie akumulatora 12 V.

### Praca w tle — Android / Samsung

Aplikacja korzysta z systemowego WorkManagera. Wymaga internetu; odczyty mogą czekać na sieć. Ręczne odświeżenie prosi system o priorytet, ale obciążenie/limity Androida, oszczędzanie baterii, Doze i ograniczenia producenta mogą je opóźniać. Aplikacja nie może sama nadać sobie nieograniczonej pracy w tle.

W **Aktualizacje i Bluetooth** sprawdź zgłoszony stan ograniczeń/optymalizacji Androida i przycisk **Ustawienia systemowe aplikacji**. Przy opóźnieniach rozważ **Informacje o aplikacji → Bateria → Bez ograniczeń**. Na Samsungu dodatkowo sprawdź **Ustawienia → Bateria (lub Pielęgnacja urządzenia → Bateria) → Limity użycia w tle**: usuń aplikację z uśpionych/głęboko uśpionych i dodaj do nigdy nieusypianych. Nazwy zależą od wersji One UI. Może to zwiększyć zużycie baterii telefonu. Aplikacja nie wykrywa pewnie wszystkich ustawień Samsunga. Po wymuszonym zatrzymaniu otwórz ją ponownie. Fizyczny BT, Android 17 i zachowanie na Foldzie wymagają dalszych testów.

### Zgłaszanie problemów

Podaj wersję aplikacji, telefon/Android, region, model auta, czas oczekiwania i komunikat bez danych prywatnych. Nie publikuj haseł, tokenów, e-maili konta, VIN ani współrzędnych. Logowanie zostało potwierdzone przez testera, lecz niezawodność odczytów jest nadal sprawdzana. Nie ponawiaj wielokrotnie poleceń o nieznanym wyniku.

## Background-work references

- [Android: persistent work and expedited requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Samsung: background sleeping and exceptions](https://developer.samsung.com/mobile/app-management.html)

## Tyre alarm / Alarm opon — 0.7.1

**ENG:** Open **Alarm ciśnienia opon**, select **Przód** (front) or **Tył** (rear), choose the minimum pressure and tap **Zapisz**. Each axle defaults to 2.4 bar; available values are 1.0–4.0 bar in 0.1 steps. The warning uses strictly lower readings and requires drive OFF. Saving immediately updates cached dashboard/widget warnings without contacting the vehicle. **Logowanie i dane konta** opens account options; **WYCZYŚĆ DANE** explicitly logs out and deletes local account/vehicle data. System Back or **Wróć do aplikacji** returns from the account screen. **Zamknij** closes the Bluetooth dialog.

**PL:** Otwórz **Alarm ciśnienia opon**, wybierz **Przód** lub **Tył**, ustaw próg i naciśnij **Zapisz**. Domyślnie obie osie mają 2,4 bar; zakres to 1,0–4,0 bar co 0,1. Alarm dotyczy wartości niższych od progu i wymaga wyłączonego napędu. Zapis od razu przelicza ostrzeżenia na podstawie ostatnich danych, bez łączenia z autem. **Logowanie i dane konta** otwiera opcje konta; dopiero **WYCZYŚĆ DANE** wylogowuje i usuwa lokalne dane konta/samochodu. Systemowe cofnięcie lub **Wróć do aplikacji** wraca z konta do panelu. **Zamknij** zamyka wybór BT.

## Combined charging tile / Zbiorczy kafelek ładowania — 0.7.4

**ENG:** Charging, battery power and remaining time are now one tile. Existing selections merge automatically, without duplicates. MOC is battery power in kW (the reported sign is preserved); KONIEC is remaining time in minutes, not a clock time. Red means the vehicle reported charging; blue means inactive or unknown (see title). Missing values appear as —. Readings can have different timestamps; check the tile timestamp.

**PL:** Ładowanie, Moc baterii i Do końca to teraz jeden kafelek. Dotychczasowe wybory łączą się automatycznie bez duplikatów. MOC to moc baterii w kW (zachowujemy znak odczytu); KONIEC to pozostały czas w minutach, nie godzina zakończenia. Czerwony oznacza odczytane ładowanie; niebieski brak ładowania lub stan nieznany (zgodnie z tytułem). Brak wartości to —. Odczyty mogą mieć różne czasy; sprawdzaj datę kafelka.
