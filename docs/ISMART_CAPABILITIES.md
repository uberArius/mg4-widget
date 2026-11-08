# Katalog możliwości iSMART

Stan analizy: 25 października 2026. Zakres: funkcje i schematy odnalezione w przypiętych kopiach SAIC-iSmart-API, nie kompletna specyfikacja producenta i nie obietnica ich działania na każdym aucie. To katalog możliwości do dalszej implementacji; wersja 0.3 udostępnia 28 kafelków (w tym grupowane odczyty i harmonogram ładowania), profil ogrzewania oraz diagnostykę 175 pól z pięciu odpowiedzi pojazdu, a nie wszystkie poniższe funkcje.

Źródła:
- Klient Python: https://github.com/SAIC-iSmart-API/saic-python-client-ng/tree/f7147c46c9e592ca57f26afd40689ea532503a96
- Gateway: https://github.com/SAIC-iSmart-API/saic-python-mqtt-gateway/tree/444b370a6b0eba4ed578535ac789d1d06a6b0c88

## Odczyty użytkowe

| Grupa | Dane obecne w schematach lub interpretowane przez gateway |
|---|---|
| Identyfikacja | Lista pojazdów, VIN, marka, model/nazwa, rocznik, seria, kolor, konfiguracja wyposażenia, pojazd bieżący, aktywacja i powiązanie z kontem |
| Konto i udostępnienia | Strefa czasowa, informacja o koncie dodatkowym, lista udostępnień i ich pola uprawnień/ważności; metadane profilu i sesji w odpowiedzi logowania |
| Bateria trakcyjna | SOC w %, zasięg elektryczny, dane energii/pojemności baterii, napięcie, prąd, moc, aktywność napędu HV |
| Ładowanie | Stan BMS, typ ładowania, wtyczka AC/DC, blokada przewodu, cel SOC, limit prądu, pozostały/upływający czas, początek i koniec, przyczyna zakończenia/błędu, harmonogram, dane ładowarki/identyfikator/dostawca gdy obecne |
| Ładowarka pokładowa | Napięcie i prąd wejściowy, dane faz; moc obliczana przez gateway |
| Ogrzewanie baterii | Stan, odpowiedź/odmowa/przyczyna zatrzymania, harmonogram |
| V2X | Pola stanu rozładowania, pozostałego czasu, celu SOC i przyczyny zatrzymania; zależne od modelu |
| Klimat | Temperatura kabiny i zewnętrzna, tryb zdalnej klimatyzacji, ogrzewanie tylnej szyby, poziomy grzania fotela lewego/prawego; pole grzania kierownicy i przyczyny błędu w schemacie, bez gotowej funkcji jego sterowania w sprawdzonym kliencie |
| Nadwozie | Stan zamków, każde z czterech drzwi, maska, bagażnik, cztery szyby, szyberdach |
| Światła | Drogowe, mijania, pozycyjne |
| Opony | Ciśnienie czterech opon, stan systemu monitorowania opon |
| Napęd/postój | Stan silnika/napędu, tryb zasilania, hamulec postojowy, aktywność magistrali i jej ostatni czas, stan alarmu, lastKeySeen (znaczenia nie należy utożsamiać automatycznie z „kluczyk w aucie”) |
| Przebieg i podróże | Przebieg całkowity, dzienny, od ostatniego ładowania, identyfikator/dystans bieżącej podróży, energia/zużycie dzienne i od ostatniego ładowania |
| Akumulator pomocniczy | Napięcie akumulatora 12 V; nie jest to pomiar jego pojemności ani zdrowia |
| GPS auta | Współrzędne, wysokość, kierunek, prędkość, czas i jakość pozycji, satelity/HDOP; cache ma ograniczony zestaw |
| Modele spalinowe/hybrydowe | Poziom paliwa, zasięg paliwowy i elektryczny; nie dla MG4 |
| Wiadomości | Alarmy, historia poleceń, wiadomości producenta; treść, tytuł, nadawca, daty, status przeczytania, liczniki nieprzeczytanych |
| Ustawienia alarmów | Przełączniki alarmów usterki pojazdu, geofencingu i uruchomienia samochodu |
| Aktualność | Czas stanu pojazdu i pozycji, informacja cache; flagi poprawności odczytów. onlineStatus cache nie dowodzi, że samochód jest obudzony |

Nie każdy model wypełnia każde pole. Brak odczytu i wartości specjalne nie oznaczają zera ani wyłączenia. Część wielkości (np. moc, energia, pojemność dla konkretnego modelu, dostępność) jest przeliczana albo szacowana przez gateway, nie stanowi bezpośredniego pomiaru z konta. Zadana lokalnie temperatura klimatyzacji nie powinna być prezentowana jako potwierdzona temperatura zadana w aucie, jeżeli API nie zwraca jej niezależnie.

## Zapis/sterowanie zaimplementowane w bibliotece

| Grupa | Polecenia |
|---|---|
| Klimatyzacja | Start/stop, temperatura zakodowana dla modelu, sam nawiew, przednie odszranianie/nawiew na szybę; niskopoziomowe parametry trybu nawiewu i AC |
| Fotele | Włączenie/wyłączenie lub poziom grzania dwóch przednich foteli, zależnie od wyposażenia |
| Tylna szyba | Start/stop ogrzewania |
| Ładowanie | Start/stop, docelowy SOC, limit prądu, harmonogram i tryb harmonogramu |
| Przewód ładowania | Blokowanie/odblokowanie portu/przewodu |
| Bateria trakcyjna | Start/stop ogrzewania baterii, włączenie/wyłączenie i godzina harmonogramu |
| V2X | Start/stop oddawania energii oraz docelowy/graniczny SOC |
| Zamki | Zamknięcie/otwarcie auta; polecenie otwarcia/odblokowania bagażnika. Nie oznacza automatycznie elektrycznego podnoszenia klapy |
| Szyby | Ogólne polecenia otwierania/zamykania wybranych szyb/szyberdachu; osobna funkcja zamknięcia szyby kierowcy. Sama obecność metody nie potwierdza działania na MG4 |
| Znajdź samochód | Światła, klakson, oba, zatrzymanie funkcji |
| Alarmy | Zmiana dostępnych przełączników alarmów |
| Wiadomości | Oznaczenie przeczytania, usunięcie pojedynczej wiadomości, usunięcie wszystkich alarmów/historii poleceń/wiadomości z danej grupy |

Wymuszenie odczytu to operacja komunikacyjna, nie zapis ustawienia auta. Parametry MQTT takie jak częstotliwość odczytu, ręczna pojemność baterii czy nadpisanie aktywności HV są ustawieniami gatewaya, nie poleceniami zapisu tych wartości w samochodzie.

Nie znaleziono gotowych funkcji zmiany nazwy/modelu/rocznika, tworzenia dodatkowego konta, zmiany hasła, aktualizacji firmware, ustawiania SOH ani dowolnego sterowania ogrzewaniem kierownicy. Nazwy w enumach (np. KEY_MANAGEMENT) nie są dowodem obsługi takiej funkcji.

## Wykrywanie możliwości

`vehicleModelConfiguration` to lista itemCode/itemName/itemValue. Gateway rozpoznaje m.in.:
- `HeatedSeat = 1`: fotele z regulacją poziomu;
- `HeatedSeat = 2`: fotele włącz/wyłącz;
- `BType = 1`: wsparcie celu SOC;
- `S35`: informacja używana do rozpoznawania szyberdachu.

Brak kodu traktujemy jako „nie ustalono”, nie dowód braku sprzętu. Dostępność zależy też od regionu, uprawnień konta i warunków auta. W interfejsie rozróżniamy: potwierdzona obsługa, brak obsługi, nieustalona obsługa, brak uprawnień. Nie wysyłamy komend testowych automatycznie przy logowaniu.

`modelName`, `series` i konfiguracja pomagają rozpoznać model. Nie znaleziono uniwersalnego pola `isXPower` ani pewnej tabeli identyfikującej wszystkie XPower. Seria EH32 oznacza rodzinę MG4. Dopisek XPOWER wymaga jednoznacznej nazwy lub zweryfikowanego mapowania kodu; w razie niepewności pokazujemy MG4/nazwę auta bez dopisku.

Klient przekazuje parametry foteli jako lewy i prawy. Dla UK/Australii wymagane jest zweryfikowane przypisanie do kierowcy/pasażera, z ustawieniem strony kierownicy, gdy konfiguracja nie rozstrzyga tego jednoznacznie.

## Regiony

Klient zawiera profile MG Europe (`eu`), Australia/Nowa Zelandia (`au`), Turcja (`tr`), a także odrębny profil marki MAXUS Europe. Projekt widżetu obsługuje wybór MG Europe (w tym konta UK korzystające z europejskiego backendu) oraz Australia/NZ. Wybór dotyczy regionu konta, a nie aktualnego położenia telefonu. Źródłowy README opisuje endpoint AU jako przetestowany, choć inny od używanego przez oficjalną aplikację w AU/NZ. Niezbędny test z użytkownikiem regionalnym.

## Konto dodatkowe

Schematy przewidują isSubaccount, subAccountList, status, operationType, locationAuthorization i okres ważności. Nie gwarantują identycznych uprawnień sterowania jak konto główne. Zalecenie konta dodatkowego znajduje się przy logowaniu, ale jego możliwości odczytu/sterowania trzeba sprawdzić; sam napis nie rozwiązuje ograniczeń uprawnień po stronie MG.

## Aneks: nazwy surowych pól odpowiedzi

Poniższy indeks pochodzi ze schematów klienta. Nazwa pola nie potwierdza jego obecności w konkretnym aucie ani poprawności interpretacji. Nieznane flagi i kody pozostają diagnostyką, a nie gotowymi kafelkami. Dane sesji nie służą do wyświetlania ani logowania.

**LoginResp** (`schema.py`): `access_token`, `account`, `avatar`, `client_id`, `dept_id`, `detail`, `expires_in`, `jti`, `languageType`, `license`, `oauth_id`, `post_id`, `refresh_token`, `role_id`, `role_name`, `scope`, `tenant_id`, `token_type`, `user_id`, `user_name`.

**GpsPosition** (`schema.py`): `gpsStatus`, `timeStamp`, `wayPoint`.

**VehicleModelConfiguration** (`vehicle/schema.py`): `itemCode`, `itemName`, `itemValue`.

**SubAccount** (`vehicle/schema.py`): `authorizationCardType`, `btKeyStatus`, `locationAuthorization`, `modelName`, `operationType`, `status`, `subaccountId`, `subscriberId`, `userAccount`, `userName`, `validityEndTime`, `validityStartTime`, `vin`.

**VinInfo** (`vehicle/schema.py`): `bindTime`, `brandName`, `colorName`, `isActivate`, `isCurrentVehicle`, `isSubaccount`, `modelName`, `modelYear`, `name`, `series`, `vin`, `subAccountList`, `vehicleModelConfiguration`.

**BasicVehicleStatus** (`vehicle/schema.py`): `batteryVoltage`, `bonnetStatus`, `bootStatus`, `canBusActive`, `clstrDspdFuelLvlSgmt`, `currentJourneyId`, `currentJourneyDistance`, `dippedBeamStatus`, `driverDoor`, `driverWindow`, `engineStatus`, `extendedData1`, `extendedData2`, `exteriorTemperature`, `frontLeftSeatHeatLevel`, `frontLeftTyrePressure`, `frontRightSeatHeatLevel`, `frontRightTyrePressure`, `fuelLevelPrc`, `fuelRange`, `fuelRangeElec`, `handBrake`, `interiorTemperature`, `lastKeySeen`, `lockStatus`, `mainBeamStatus`, `mileage`, `passengerDoor`, `passengerWindow`, `powerMode`, `rearLeftDoor`, `rearLeftTyrePressure`, `rearLeftWindow`, `rearRightDoor`, `rearRightTyrePressure`, `rearRightWindow`, `remoteClimateStatus`, `rmtHtdRrWndSt`, `sideLightStatus`, `steeringHeatLevel`, `steeringWheelHeatFailureReason`, `sunroofStatus`, `timeOfLastCANBUSActivity`, `vehElecRngDsp`, `vehicleAlarmStatus`, `wheelTyreMonitorStatus`.

**ExtendedVehicleStatus** (`vehicle/schema.py`): `alertDataSum`.

**VehicleStatusResp** (`vehicle/schema.py`): `basicVehicleStatus`, `extendedVehicleStatus`, `gpsPosition`, `statusTime`.

**VehicleStatusCacheResp** (`vehicle/schema.py`): `basicVehicleStatus`, `gpsPosition`, `statusTime`, `onlineStatus`.

**VehicleControlResp** (`vehicle/schema.py`): `basicVehicleStatus`, `failureType`, `gpsPosition`, `rvcReqSts`, `rvcReqType`.

**ChargingStatus** (`vehicle_charging/schema.py`): `chargingCurrent`, `chargingDuration`, `chargingElectricityPhase`, `chargingGunState`, `chargingPileID`, `chargingPileSupplier`, `chargingState`, `chargingTimeLevelPrc`, `chargingType`, `chargingVoltage`, `endTime`, `fotaLowestVoltage`, `fuelRangeElec`, `lastChargeEndingPower`, `mileage`, `mileageOfDay`, `mileageSinceLastCharge`, `powerLevelPrc`, `powerUsageOfDay`, `powerUsageSinceLastCharge`, `realtimePower`, `startTime`, `staticEnergyConsumption`, `totalBatteryCapacity`, `workingCurrent`, `workingVoltage`.

**ChargeStatusResp** (`vehicle_charging/schema.py`): `chargingStatus`, `gpsPosition`, `statusTime`.

**ChrgMgmtData** (`vehicle_charging/schema.py`): `bmsAdpPubChrgSttnDspCmd`, `bmsAltngChrgCrntDspCmd`, `bmsChrgCtrlDspCmd`, `bmsChrgOtptCrntReq`, `bmsChrgOtptCrntReqV`, `bmsChrgSpRsn`, `bmsChrgSts`, `bmsDsChrgSpRsn`, `bmsEstdElecRng`, `bmsOnBdChrgTrgtSOCDspCmd`, `bmsPackCrnt`, `bmsPackCrntV`, `bmsPackSOCDsp`, `bmsPackVol`, `bmsPTCHeatReqDspCmd`, `bmsPTCHeatResp`, `bmsPTCHeatSpRsn`, `bmsReserCtrlDspCmd`, `bmsReserSpHourDspCmd`, `bmsReserSpMintueDspCmd`, `bmsReserStHourDspCmd`, `bmsReserStMintueDspCmd`, `ccuEleccLckCtrlDspCmd`, `ccuOffBdChrgrPlugOn`, `ccuOnbdChrgrPlugOn`, `chrgngAddedElecRng`, `chrgngAddedElecRngV`, `chrgngDoorOpenCnd`, `chrgngDoorPosSts`, `chrgngRmnngTime`, `chrgngRmnngTimeV`, `chrgngSpdngTime`, `chrgngSpdngTimeV`, `clstrElecRngToEPT`, `disChrgngRmnngTime`, `disChrgngRmnngTimeV`, `imcuChrgngEstdElecRng`, `imcuChrgngEstdElecRngV`, `imcuDschrgngEstdElecRng`, `imcuDschrgngEstdElecRngV`, `imcuVehElecRng`, `imcuVehElecRngV`, `onBdChrgrAltrCrntInptCrnt`, `onBdChrgrAltrCrntInptVol`.

**RvsChargeStatus** (`vehicle_charging/schema.py`): `chargingDuration`, `chargingElectricityPhase`, `chargingGunState`, `chargingPileID`, `chargingPileSupplier`, `chargingType`, `endTime`, `extendedData1`, `extendedData2`, `extendedData3`, `extendedData4`, `fotaLowestVoltage`, `fuelRangeElec`, `lastChargeEndingPower`, `mileage`, `mileageOfDay`, `mileageSinceLastCharge`, `powerUsageOfDay`, `powerUsageSinceLastCharge`, `realtimePower`, `startTime`, `staticEnergyConsumption`, `totalBatteryCapacity`, `workingCurrent`, `workingVoltage`.

**ChrgMgmtDataResp** (`vehicle_charging/schema.py`): `chrgMgmtData`, `rvsChargeStatus`.

**ChargingSettingResp** (`vehicle_charging/schema.py`): `bmsAltngChrgCrntDspCmd`, `bmsAltngChrgCrntResp`, `bmsChrgTrgtSOCResp`, `bmsEstdElecRng`, `bmsOnBdChrgTrgtSOCDspCmd`, `bmsPackCrnt`, `imcuDschrgTrgtSOCDspCmd`, `imcuDschrgTrgtSOCResp`, `rvcReqSts`.

**ScheduledChargingResp** (`vehicle_charging/schema.py`): `bmsAdpPubChrgSttnDspCmd`, `bmsReserChrgCtrlResp`, `bmsReserCtrlDspCmd`, `bmsReserSpHourDspCmd`, `bmsReserSpMintueDspCmd`, `bmsReserStHourDspCmd`, `bmsReserStMintueDspCmd`, `rvcReqSts`.

**ChrgPtcHeatResp** (`vehicle_charging/schema.py`): `ptcHeatReqDspCmd`, `ptcHeatResp`, `rvcReqSts`.

**ChargingControlResp** (`vehicle_charging/schema.py`): `bmsAdpPubChrgSttnDspCmd`, `bmsAltngChrgCrntDspCmd`, `bmsAltngChrgCrntResp`, `bmsChrgCtrlDspCmd`, `bmsChrgOtptCrntReq`, `bmsChrgOtptCrntReqV`, `bmsChrgSpRsn`, `bmsChrgSts`, `bmsChrgTrgtSOCResp`, `bmsDsChrgCtrlDspCmd`, `bmsDsChrgCtrlResp`, `bmsDsChrgSpRsn`, `bmsEstdElecRng`, `bmsOnBdChrgTrgtSOCDspCmd`, `bmsPTCHeatReqDspCmd`, `bmsPTCHeatResp`, `bmsPTCHeatSpRsn`, `bmsPackCrnt`, `bmsPackCrntV`, `bmsPackSOCDsp`, `bmsPackVol`, `bmsReserChrgCtrlResp`, `bmsReserCtrlDspCmd`, `bmsReserSpHourDspCmd`, `bmsReserSpMintueDspCmd`, `bmsReserStHourDspCmd`, `bmsReserStMintueDspCmd`, `ccuEleccLckCtrlDspCmd`, `ccuEleccLckCtrlResp`, `ccuOffBdChrgrPlugOn`, `ccuOnbdChrgrPlugOn`, `chrgCtrlDspCmd`, `chrgCtrlResp`, `chrgngAddedElecRng`, `chrgngAddedElecRngV`, `chrgngDoorOpenCnd`, `chrgngDoorPosSts`, `chrgngRmnngTime`, `chrgngRmnngTimeV`, `chrgngSpdngTime`, `chrgngSpdngTimeV`, `clstrElecRngToEPT`, `disChrgngRmnngTime`, `disChrgngRmnngTimeV`, `imcuChrgngEstdElecRng`, `imcuChrgngEstdElecRngV`, `imcuDschrgTrgtSOCDspCmd`, `imcuDschrgTrgtSOCResp`, `imcuDschrgngEstdElecRng`, `imcuDschrgngEstdElecRngV`, `imcuVehElecRng`, `imcuVehElecRngV`, `onBdChrgrAltrCrntInptCrnt`, `onBdChrgrAltrCrntInptVol`, `rvcReqSts`.

**ScheduledBatteryHeatingResp** (`vehicle_charging/schema.py`): `startTime`, `status`.

**MessageEntity** (`message/schema.py`): `content`, `contentId`, `contentIdList`, `createTime`, `messageId`, `messageTime`, `messageType`, `readStatus`, `sender`, `showCheckButton`, `title`, `vin`.

**MessageResp** (`message/schema.py`): `alarmNumber`, `commandNumber`, `messages`, `newsNumber`, `recordsNumber`, `totalNumber`.

**UserTimezoneResp** (`user/schema.py`): `timezone`.

**AlarmSwitch** (`vehicle/alarm/schema.py`): `alarmType`, `functionSwitch`, `alarmSwitch`.

**AlarmSwitchResp** (`vehicle/alarm/schema.py`): `alarmSwitchList`.

Zagnieżdżone dane GPS: `gpsPosition.wayPoint` zawiera `hdop`, `heading`, `satellites`, `speed`, a `wayPoint.position`: `latitude`, `longitude`, `altitude`. Zagnieżdżony `LoginResp.detail` zawiera `languageType`.
