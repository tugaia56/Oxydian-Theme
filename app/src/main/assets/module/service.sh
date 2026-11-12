#!/system/bin/sh
# Oxydian Theme - service
# Ad ogni avvio abilita SOLO gli overlay dello store che non sono ancora attivi
# (quelli gia' attivi non vengono toccati: costo zero).
MODDIR=${0%/*}
SRC=$MODDIR/store
LOG=/data/local/tmp/oxydian_theme.log
# Android puro: i colori del Theme (creati al volo, spariscono ad ogni riavvio) si accendono appena il servizio overlay risponde,
# senza aspettare la fine dell'avvio: cosi' l'accento c'e' gia' al PIN iniziale (prima arrivavano dopo 1-3 minuti). Versione veloce.
if [ -z "$(getprop ro.build.version.oplusrom)" ] && [ -f "$MODDIR/fixedcolors_fast.sh" ]; then
  (
    i=0
    until cmd overlay list --user 0 >/dev/null 2>&1 || [ $i -ge 90 ]; do sleep 1; i=$((i+1)); done
    echo "=== fixedcolors_fast $(date) (attesa ${i}s, uptime $(cut -d' ' -f1 /proc/uptime)) ===" >> "$LOG"
    # presto nell'avvio il sistema rifiuta ancora i comandi (overlay non trovato): si riprova ogni 2 secondi finche' risultano accesi
    k=0
    while [ $k -lt 60 ]; do
      sh "$MODDIR/fixedcolors_fast.sh" >> "$LOG" 2>&1
      n=$(cmd overlay list --user 0 2>/dev/null | grep -c "^\[x\] com.android.shell:oxyfix")
      echo "fixedcolors_fast: giro $k, accesi $n, uptime $(cut -d' ' -f1 /proc/uptime)" >> "$LOG"
      [ "${n:-0}" -ge 5 ] && break
      k=$((k+1)); sleep 2
    done
    echo "=== fixedcolors_fast fine $(date) ===" >> "$LOG"
    # Orologio (per le sveglie), Contatti e Messaggi partono durante l'avvio, prima dei colori del Theme, e li tengono finche' il processo
    # vive (sfondo blu di serie). "am kill" chiude solo i processi in secondo piano e NON cancella le sveglie (a differenza di force-stop):
    # alla prossima apertura ripartono gia' coi colori giusti. Si ripete dopo il controllo qui sotto.
    close_early_apps() {
      for p in com.google.android.deskclock com.google.android.contacts com.google.android.apps.messaging; do
        am kill "$p" >/dev/null 2>&1
      done
      echo "fixedcolors_fast: app d'avvio chiuse (am kill), uptime $(cut -d' ' -f1 /proc/uptime)" >> "$LOG"
    }
    close_early_apps
    # subito dopo SystemUI rifa' i suoi colori e li mette davanti ai nostri (prima ci pensava colorwatch, ma solo dopo circa 1 minuto,
    # a PIN iniziale gia' mostrato): si controlla ogni secondo per 2 minuti e, appena stanno davanti, si rifa il giro veloce
    w=0; fx=0
    while [ $w -lt 120 ] && [ $fx -lt 4 ]; do
      sleep 1; w=$((w+1))
      l=$(cmd overlay list --user 0 android 2>/dev/null | grep '^\[x\]')
      a=$(echo "$l" | grep -n 'com.android.shell:oxyfix' | tail -1 | cut -d: -f1)
      b=$(echo "$l" | grep -n 'com.android.systemui:' | tail -1 | cut -d: -f1)
      if [ -n "$a" ] && [ -n "$b" ] && [ "$b" -gt "$a" ]; then
        echo "fixedcolors_fast: colori del sistema davanti, li rimetto, uptime $(cut -d' ' -f1 /proc/uptime)" >> "$LOG"
        sh "$MODDIR/fixedcolors_fast.sh" >> "$LOG" 2>&1
        fx=$((fx+1))
      fi
    done
    close_early_apps
  ) &
fi
until [ "$(getprop sys.boot_completed)" = "1" ]; do sleep 2; done
sleep 10
{
echo "=== oxydian_theme service $(date) ==="
set -- "$SRC"/OxydianThemeComponent*.apk
[ -e "$1" ] || { echo "nessun APK"; exit 0; }
LIST=$(cmd overlay list 2>/dev/null)
for f in "$SRC"/OxydianThemeComponent*.apk; do
  b=${f##*/}
  n=${b%.apk}.overlay
  if echo "$LIST" | grep -q "^\[x\] $n\$"; then
    continue
  elif echo "$LIST" | grep -q "^\[ \] $n\$"; then
    # i temi di app/Dark Shadow si accendono solo se sono nell'elenco scelto dall'utente
    # (un tema spento resta installato ma non va riacceso ad ogni avvio)
    case "$n" in
      OxydianThemeComponentApp_*|OxydianThemeComponentDS_*)
        if [ -f "$MODDIR/enabled.list" ] && ! grep -qx "$n" "$MODDIR/enabled.list"; then
          echo "lasciato spento $n"
          continue
        fi;;
    esac
    cmd overlay enable --user current "$n" 2>&1 && cmd overlay set-priority "$n" highest 2>&1
    echo "abilitato $n"
  else
    echo "non ancora registrato $n (serve un riavvio)"
  fi
done
echo "=== fine ==="
} >> "$LOG" 2>&1

# Pause: all'avvio il telefono e' gia' molto carico (memoria quasi piena, molti moduli): i lavori qui sotto aspettano che il
# carico scenda, cosi' non peggiorano i blocchi alla schermata di avvio/PIN.
wait_idle() {
  i=0
  MAXI=${1:-30}
  while [ $i -lt $MAXI ]; do
    l=$(cut -d' ' -f1 /proc/loadavg 2>/dev/null)
    [ "${l%%.*}" -lt 8 ] 2>/dev/null && break
    sleep 5
    i=$((i+1))
  done
}

# Colori "couiSingleFirst*" del sistema (Personalizzato, pulsanti dei recenti): dopo l'avvio si rimettono quelli scelti
# e, se erano cambiati, si riavvia il launcher.
if [ -f "$MODDIR/uxcolor.sh" ]; then
  ( sleep 75; wait_idle; sh "$MODDIR/uxcolor.sh" >> "$LOG" 2>&1; sleep 150; sh "$MODDIR/uxcolor.sh" >> "$LOG" 2>&1 ) &
fi

# Android 16/17: i colori scelti (creati al volo) spariscono ad ogni riavvio e il sistema rifa' i suoi
# poco dopo l'avvio: si rimettono, a carico basso, e di nuovo piu' tardi. Poche passate, spaziate.
if [ -f "$MODDIR/fixedcolors.sh" ]; then
  (
    # si riprova finche' i colori non risultano creati (all'avvio il sistema a volte non e' ancora pronto)
    # Android puro (niente OxygenOS): prima passata presto (12 s, al massimo 20 s di attesa del carico), cosi' l'accento torna subito;
    # OxygenOS: attese lunghe di sempre (blocchi al PIN dopo l'avvio)
    if [ -z "$(getprop ro.build.version.oplusrom)" ]; then DELAYS="12 20 40 60"; IDLEMAX=4; else DELAYS="45 45 60 90"; IDLEMAX=30; fi
    for t in $DELAYS; do
      sleep $t
      # gia' creati dal giro veloce (all'avvio): si salta il primo giro completo
      [ "$(cmd overlay list 2>/dev/null | grep -c "^\[x\] com.android.shell:oxyfix")" -ge 1 ] && break
      wait_idle $IDLEMAX
      sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1
      n=$(cmd overlay list 2>/dev/null | grep -c "^\[x\] com.android.shell:oxyfix")
      [ "${n:-0}" -ge 1 ] && break
    done
    # una ripassata di sicurezza piu' tardi (il sistema rifa' i suoi colori)
    sleep 120
    wait_idle
    sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1
  ) &
fi

# Android 16/17: quando SystemUI riparte rimette i suoi colori sopra quelli del Theme; il controllo li riporta davanti.
if [ -f "$MODDIR/colorwatch.sh" ]; then
  sh "$MODDIR/colorwatch.sh" >> "$LOG" 2>&1 &
fi

# I colori di sistema (overlay su "android") devono restare sopra gli overlay di altre app/moduli
# (es. i vecchi accento/sfondo di Oxydian, ColorBlendr), che si riapplicano dopo l'avvio:
# si riporta in cima, adesso e di nuovo dopo un po'.
SYS=OxydianThemeComponentDS_SYS.overlay
for t in 0 40 90; do
  sleep $t
  cmd overlay list 2>/dev/null | grep -q "^\[x\] $SYS\$" && cmd overlay set-priority "$SYS" highest >> "$LOG" 2>&1
done
