# Testy na Samsung Fold 7 / MG4 XPower — 0.2

## Wygląd bez auta

- Dodaj „MG · morski”. Sprawdź zewnętrzny i rozłożony ekran Folda, zmianę szerokości, orientacji i większą czcionkę.
- Zaznacz 0, 1, 10 i wszystkie dostępne kafelki. Przewijaj listę; nagłówek i przyciski pozostają poza nią.
- Zmień kolejność, wyłącz ikony, każdy przycisk osobno i oba. Wyłączone ikony nie pozostawiają odstępu, tekst jest wyśrodkowany.
- Ogrzewanie w demonstracji jest nieaktywne. Po aktualizacji stary Full staje się nowym widżetem, Mini/Standard dodaj ponownie.

## Konto i odczyty

- Wybierz prawidłowy region konta (EU/UK albo AU/NZ). Nie podawaj haseł ani tokenów w czacie.
- Sprawdź konto dodatkowe: widoczność samochodu, odczyty, możliwość sterowania. Sam fakt udostępnienia nie gwarantuje wszystkich uprawnień.
- Sprawdź nazwę modelu; zwykły MG4 nie może dostać dopisku XPOWER tylko na podstawie EH32.
- Porównaj odczyty i ich czas z autem. Nieznana wartość jest kreską; stare dane zachowują datę. Dane ładowania mogą mieć czas nieznany.
- Zamki odblokowane, aktywny klimat i ładowanie mają czerwony kafelek po odczycie. Otwarte drzwi to odrębny parametr od zamków.
- Przy braku sieci/odmowie sesji stare odczyty pozostają; aplikacja nie loguje się w pętli.

## Odświeżanie i BT

- 0 + BT OFF: otwarcie aplikacji i restart telefonu nie odczytują auta; działa przycisk ręczny.
- Sprawdź 15 i 60 minut, aktywny tryb i cache. One UI może opóźniać wykonanie.
- Wybierz właściwy sparowany BT i włącz przełącznik. Po jeździe sprawdź czas pomiaru. Zdarzenie uruchamia próbę od razu, ale odpowiedź auta może być opóźniona.
- Ponowne połączenie anuluje oczekujące próby. Rozłączenie słuchawek nie wywołuje odczytu. Wyłączenie BT telefonu może zostać uznane za rozłączenie samochodu.
- Powtórz z zablokowanym ekranem i oszczędzaniem energii; zapisz rzeczywiste opóźnienie.

## Profil ogrzewania na zaparkowanym samochodzie

- Sprawdź temperaturę kabiny, osobno tryb szyby (profil 22°C), fotele według wykrytego wyposażenia.
- Potwierdź stronę kierownicy i mapowanie kierowca/pasażer dla UK/AU.
- Pominięty fotel nie może zostać przypadkowo wyłączony; bez jego świeżego odczytu komenda powinna zostać pominięta.
- Po komendzie porównaj realne działanie z odczytem. „Brak potwierdzenia” nie oznacza wyłączenia. Nie ponawiaj bez sprawdzenia stanu.
- Zamknij ekran podczas oczekiwania. Zadanie ma kontynuować sprawdzenie. Po przerwaniu procesu nie może powtórzyć nowej komendy.
- Zatrzymaj klimatyzację. Fotele wyłącza się osobno profilem z poziomem 0.

## Ważność wersji

- Termin tego APK: 25.11.2026 00:00 UTC. Po nim login/odczyt/sterowanie są blokowane, a usunięcie konta i ustawienia lokalne pozostają dostępne.
- Nowe wydanie podpisane tym samym kluczem powinno instalować się jako aktualizacja. Reinstalacja tego samego APK nie zmienia zapisanej w nim daty wygaśnięcia.

Do zgłoszenia błędu: ekran, komunikat bez danych konta, godzina i czynność. Nie są potrzebne hasła, tokeny ani pełny VIN.

## Dodatkowe testy 0.3

- Porównaj plan ładowania z aplikacją MG: wyłączony, włączony, przez północ oraz do celu SOC. Brak odczytu nie może oznaczać „wyłączony”; sprawdź godziny lokalne auta.
- Otwieraj pojedyncze drzwi/szyby i sprawdź układ 2×2, także dla kierownicy po prawej. Szyberdach nie wchodzi do grupy szyb drzwiowych.
- Sprawdź wartości ciśnienia czterech opon, obie temperatury i oba fotele. Brak jednej wartości pozostawia kreskę i informację o niepełnym odczycie.
- Zmień szerokość i wysokość widżetu; sprawdź przewijanie, ostatni kafelek, widoczne/ukryte ikony i przyciski.
- Uruchom diagnostykę przyciskiem, zamknij i otwórz ekran podczas odczytu. Sprawdź raport po zakończeniu. Powtórz bez internetu: błędy mają być widoczne przy polach, bez podstawiania starszego cache.
- Diagnostyka nie powinna zmieniać harmonogramu, ogrzewania ani ładowania. Poszczególne źródła mogą nie być dostępne na koncie dodatkowym; ich brak nie powinien ukrywać wyników pozostałych źródeł.
