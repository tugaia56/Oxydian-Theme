# Oxydian Theme

🌐 [English](README.md) · **Italiano**

**Temi overlay** (senza Xposed) per OxygenOS 16 — l'app compagna di [Oxydian](https://github.com/tugaia56/Oxydian-xposed).

Nata su suggerimento di Luigi (@LC9889): un'app senza Xposed, con **tutti gli overlay funzionanti**. Se ti serve un'app che non è nell'elenco, chiedilo nel gruppo e può essere aggiunta.

## Cosa fa

- **Colori di sistema** — 48 accenti e 22 sfondi, oppure un colore a scelta; **nero puro**, saturazione e luminosità regolabili
- **Temi per 88 app** (Google e di terze parti) e **Dark Shadow** per Impostazioni, SystemUI, Launcher e app OnePlus/OPPO
- **Icone** — Wi-Fi (18 stili), segnale mobile (42 stili) e barra di navigazione (9 stili), con dimensione e colore
- **Pack icone** per le Impostazioni
- Colori di **barra di progresso** circolare e **tastierino PIN**
- **Nascondi attività di rete** (frecce In/Out)
- **Automazione colori** — cambia i colori di sistema con Tasker, MacroDroid o Automate
- Selezione stile Substratum: spunti le voci e premi **Attiva / Disattiva / Rimuovi**
- **Backup e ripristino** delle scelte, **controllo aggiornamenti** integrato

## Requisiti

- OxygenOS 16 (testato su OnePlus 12), Android 12+ (minSdk 31)
- **Root** (KernelSU o Magisk)

L'app crea il modulo `oxydian_theme`: al primo avvio serve un riavvio, poi i cambi sono immediati. Il colore delle icone Wi-Fi e Segnale, e i colori letti da altre parti di Oxydian, usano Oxydian (hook).

## Automazione colori (Tasker / MacroDroid / Automate)

Attivala in **Mods → Automazione colori**, applica almeno una volta i colori di sistema dall'app, poi invia un broadcast:

- Azione: `it.tugaia56.oxydian.theme.action.APPLY_CONFIG`
- Pacchetto: `it.tugaia56.oxydian.theme`
- Extra (manda solo quelli da cambiare): `accent` e `background` (nome di un preset, `#RRGGBB` oppure `default`), `randomColor`, `pitchBlack`, `accentSaturation`, `backgroundSaturation` (0–200), `backgroundLightness` (da −10 a 10)

Il pulsante **Guida** nell'app ha le istruzioni passo passo.

## Compilare

```
./gradlew assembleDebug
```

Per la release serve `keystore.properties` (non versionato) e un tag `vX.Y.Z`: il flusso GitHub compila, pubblica la release e (se configurato) invia l'APK su Telegram.

## Crediti

- **Luigi (@LC9889)** — il compilatore degli overlay (aapt2, allineamento, firma) e le utility per root e file vengono dal suo Oxygen Customizer
- **Dark Shadow Theme** — gli overlay e i preset di accento, sfondo, tastierino PIN, barra di progresso e icone vengono da questo lavoro
- **Substratum** — il formato dei temi overlay da cui nasce tutto
- **Claude** (Anthropic) — assistente IA usato durante tutto lo sviluppo
