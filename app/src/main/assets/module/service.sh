#!/system/bin/sh
# Oxydian Theme - service
# Ad ogni avvio abilita SOLO gli overlay dello store che non sono ancora attivi
# (quelli gia' attivi non vengono toccati: costo zero).
MODDIR=${0%/*}
SRC=$MODDIR/store
LOG=/data/local/tmp/oxydian_theme.log
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
  while [ $i -lt 30 ]; do
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
    for t in 45 45 60 90; do
      sleep $t
      wait_idle
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

# I colori di sistema (overlay su "android") devono restare sopra gli overlay di altre app/moduli
# (es. i vecchi accento/sfondo di Oxydian, ColorBlendr), che si riapplicano dopo l'avvio:
# si riporta in cima, adesso e di nuovo dopo un po'.
SYS=OxydianThemeComponentDS_SYS.overlay
for t in 0 40 90; do
  sleep $t
  cmd overlay list 2>/dev/null | grep -q "^\[x\] $SYS\$" && cmd overlay set-priority "$SYS" highest >> "$LOG" 2>&1
done
