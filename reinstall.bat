@echo off
rem Installa sul telefono l'ULTIMO APK compilato di Oxydian Theme (dati e scelte restano).
rem Va bene sia dopo "Run" di Android Studio sia dopo Build APK: si prende il file piu' recente.
rem Attenzione: il telefono ha l'app firmata con la chiave "debug" di Studio; un APK firmato con la chiave
rem release non la aggiorna (errore INSTALL_FAILED_UPDATE_INCOMPATIBLE).
set APK=
for /f "usebackq delims=" %%f in (`powershell -NoProfile -Command "Get-ChildItem 'app\build\outputs\apk','app\build\intermediates\apk' -Recurse -Filter *.apk -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName"`) do set APK=%%f
if "%APK%"=="" (
    echo Nessun APK trovato: prima compila l'app.
    pause
    exit /b 1
)
echo Installo %APK% ...
adb install -r -t "%APK%"
echo.
echo Fatto. Se compare "Success" l'app e' aggiornata.
pause
