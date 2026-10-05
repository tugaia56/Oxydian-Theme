# Oxydian Theme

App di temi **overlay** (senza Xposed) per OxygenOS 16, compagna di [Oxydian](https://github.com/tugaia56/Oxydian-xposed).

Cosa fa:
- temi per app Google e app installate (elenco con interruttori e stato colorato);
- tema Dark Shadow per Impostazioni, SystemUI, Launcher e app OnePlus/OPPO;
- colori di sistema (accento e sfondo), barra di progresso circolare, tastierino PIN;
- icone Wi-Fi e Segnale (stile, dimensione, colore), barra di navigazione, Impostazioni, attività di rete;
- opzioni per alcune app.

Requisiti: Android 12+ (minSdk 31), **root** (KernelSU o Magisk). L'app crea il modulo `oxydian_theme`:
al primo avvio serve un riavvio, poi i cambi sono immediati.

Il colore delle icone Wi-Fi e Segnale, e i colori letti da altre parti di Oxydian, usano Oxydian (hook).

## Compilare

```
./gradlew assembleDebug
```

Per la release serve `keystore.properties` (non versionato) e un tag `vX.Y.Z`: il flusso GitHub compila,
pubblica la release e (se configurato) invia l'APK su Telegram.
