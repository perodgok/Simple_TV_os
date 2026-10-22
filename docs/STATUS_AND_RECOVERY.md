# Simple TV OS — состояние и аварийное восстановление
Дата: 22.10.2026. Репозиторий: perodgok/Simple_TV_os.

## Текущая конфигурация
- Xiaomi MiTV-MOOQ1, codename tarzan, Android TV 11, ABI armeabi-v7a (32-bit), RAM около 1.66 GiB.
- MacBook M1, macOS Monterey 12.4. Homebrew, ADB 1.0.41 (Platform Tools 37.0.1), Temurin JDK 17, Android SDK API 35, Build Tools 35.0.0.
- Подключение ADB по Wi-Fi: `192.168.0.108:5555` (DHCP; адрес может измениться).
- Собственный лаунчер `com.simpletvos.home/.MainActivity` — подтверждённый текущий HOME, запущен и сохранён после подтверждения KEEP.
- Google Launcher `com.google.android.tvlauncher` отключён для user 0, **не удалён**.
- PatchWall/Xiaomi TV Home `com.mitv.tvhome.atv` отключён для user 0.
- Дополнительно отключены: `com.mitv.tvhome.mitvplus`, `com.mitv.tvhome.michannel`, `com.miui.tv.analytics`, `com.xiaomi.statistic`.
- Заставка отключена: `settings secure screensaver_enabled=0`; прежде была 1.
- Таймер отключения экрана при проверке был `300000` (5 мин), последний сон — `timeout`. Изменение на 30 мин было предложено, но выполнение **не подтверждено**: обязательно проверить значение.
- FLauncher использовался для эксперимента, но не является HOME.
- HAPP и подписка пока не настроены.

## НЕ ОТКЛЮЧАТЬ
- YouTube: `com.google.android.youtube.tv`
- YouTube Music: `com.google.android.youtube.tvmusic`
- Zona: `mobi.zona`
- VLC: `org.videolan.vlc`
- BrowseHere: точный пакет не подтверждён — беречь оба `com.tcl.browser`, `com.internet.tvbrowser`.
- Google Play Services: `com.google.android.gms`, Play Store `com.android.vending`, WebView `com.google.android.webview`, `com.mediatek.wwtv.webview`.
- Не затрагивать пульт, Wi-Fi, Bluetooth, HDMI, звук и ADB.

## ВОССТАНОВЛЕНИЕ HOME — В ПЕРВУЮ ОЧЕРЕДЬ
На Mac в Terminal (если IP сменился, заменить TV новым адресом):
```bash
TV="192.168.0.108:5555"
adb connect "$TV"
adb -s "$TV" get-state
adb -s "$TV" shell pm enable --user 0 com.google.android.tvlauncher
adb -s "$TV" shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
adb -s "$TV" shell input keyevent KEYCODE_HOME
adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```
Если HOME не открылся, проверить ручной запуск:
```bash
adb -s "$TV" shell am start -n com.google.android.tvlauncher/.MainActivity
```
Это восстанавливает Google Launcher, **не возвращая автоматически PatchWall**.

## ПОЛНЫЙ ОТКАТ PATCHWALL И ТЕЛЕМЕТРИИ
```bash
TV="192.168.0.108:5555"
adb connect "$TV"
for pkg in \
  com.mitv.tvhome.atv \
  com.mitv.tvhome.mitvplus \
  com.mitv.tvhome.michannel \
  com.miui.tv.analytics \
  com.xiaomi.statistic
do
  adb -s "$TV" shell pm enable --user 0 "$pkg"
done
adb -s "$TV" shell settings put secure screensaver_enabled 1
```
Исходный таймер выключения экрана — 300000 мс. Восстановить его, только если значение изменяли:
```bash
adb -s "$TV" shell settings get system screen_off_timeout
adb -s "$TV" shell settings put system screen_off_timeout 300000
```

## БЫСТРАЯ ПРОВЕРКА
```bash
TV="192.168.0.108:5555"
adb -s "$TV" get-state
adb -s "$TV" shell pm list packages -d
adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s "$TV" shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'
adb -s "$TV" shell settings get secure screensaver_enabled
adb -s "$TV" shell settings get system screen_off_timeout
```

## ИСХОДНИКИ, APK И ЛОГИ НА MAC
- Корень проекта: `~/simple-tv-os/`
- Исходники v0.1: `~/simple-tv-os/simple-tv-os-v0.1/`
- Скрипт сборки: `~/simple-tv-os/simple-tv-os-v0.1/build-and-install.sh`
- APK: `~/simple-tv-os/simple-tv-os-v0.1/build/SimpleTVOS-v0.1.apk`
- Логи: `~/simple-tv-os/logs/`, включая `simple-tv-os-build.log`, `switch-home.log`, `optimization-20261022-225748.log`.
- Резервные записи: `~/simple-tv-os/backups/`.
- Список защищённых пакетов: `~/simple-tv-os/config/protected-packages.txt`.

## ПРАВИЛА БЕЗОПАСНОСТИ
1. По умолчанию **отключать**, а не удалять: `pm disable-user --user 0` и `pm enable --user 0`.
2. Не выполнять `pm uninstall` и не удалять APK.
3. Перед переключением HOME проверить запуск приложений и управление пультом; Google Launcher оставить как аварийный запасной вариант.
4. После каждого изменения проверять реальное поведение ТВ; ответ `Success` у `set-home-activity` на этой прошивке ранее не гарантировал переключение HOME.
5. Подтверждать опасные операции, вести лог, показывать маркеры начала и конца отчёта.
6. Резервные текстовые отчёты **не являются полным образом прошивки**.
7. Не публиковать секретную ссылку HAPP-подписки, ключи SSH и токены GitHub.

## ДАЛЬШЕ
Разработать Simple TV OS v0.2 (дизайн, реальные иконки, часы, пользовательский текст, управление пультом), установить HAPP локально, добавить виджеты, протестировать скорость и поведение после перезагрузки.
