# v0.6.0 — parked warnings and full dashboard

## ENG

- Red outer widget background when the reported drive state is OFF and a tyre is below 2.4 bar, locks are unlocked, or a door, window/sunroof, bonnet or boot is open. Hidden tiles still participate in the check. Unknown drive state does not trigger the warning.
- Full app dashboard: all 28 selectable tiles in two columns, with refresh/heating buttons below. Widget selection does not restrict the app dashboard.
- Shared tile rendering keeps values, grouped 2×2 layouts and reading timestamps consistent between app and widget.

Based on the latest stored readings, not a guaranteed live safety monitor. Missing/stale data and remote climate verification remain subject to vehicle/API availability. The main app UI is Polish. Android 12+. Debug-signed test build. **Expiry unchanged: 25 November 2026, 00:00 UTC.**

Read [risks](https://github.com/uberArius/mg4-widget/blob/main/RISKS.md) and [installation](https://github.com/uberArius/mg4-widget/blob/main/INSTALLATION.md). Prefer an additional iSMART account. No application source is published. Licenses are embedded in the APK and included in the documentation archive.

## PL

- Czerwone zewnętrzne tło widżetu, gdy odczytany napęd jest wyłączony i opona ma poniżej 2,4 bar, zamki są odblokowane lub otwarte są drzwi, szyba/szyberdach, maska albo bagażnik. Ukryte kafelki także są uwzględniane. Nieznany stan napędu nie uruchamia ostrzeżenia.
- Pełny panel aplikacji: wszystkie 28 dostępnych kafelków w dwóch kolumnach, z przyciskami odświeżania/ogrzewania pod listą. Wybór kafelków widżetu nie ogranicza panelu.
- Wspólne renderowanie zapewnia zgodne wartości, układ grup 2×2 i czasy odczytów w aplikacji i widżecie.

Reguła opiera się na ostatnich zapisanych odczytach, nie jest gwarantowanym monitorem bezpieczeństwa w czasie rzeczywistym. Dostępność/aktualność danych i potwierdzanie klimatyzacji wymagają dalszych prób z autem. Główny interfejs po polsku. Android 12+. Testowy podpis debug. **Termin bez zmian: 25 listopada 2026, 01:00 w Polsce (00:00 UTC).**

Przeczytaj [ryzyka](https://github.com/uberArius/mg4-widget/blob/main/RISKS.md) i [instrukcję](https://github.com/uberArius/mg4-widget/blob/main/INSTALLATION.md). Używaj konta dodatkowego. Kod aplikacji nie jest publikowany. Licencje znajdują się w APK i archiwum dokumentacji.
