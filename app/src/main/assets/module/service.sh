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

# Colori "couiSingleFirst*" del sistema (Personalizzato, pulsanti dei recenti): dopo l'avvio si rimettono quelli scelti
# e, se erano cambiati, si riavvia il launcher.
if [ -f "$MODDIR/uxcolor.sh" ]; then
  ( sleep 20; sh "$MODDIR/uxcolor.sh" >> "$LOG" 2>&1; sleep 70; sh "$MODDIR/uxcolor.sh" >> "$LOG" 2>&1 ) &
fi

# Android 16/17: i colori scelti (creati al volo) spariscono ad ogni riavvio e il sistema rifa' i suoi
# poco dopo l'avvio: si rimettono, in parallelo al resto, dopo un po' e di nuovo piu' tardi.
if [ -f "$MODDIR/fixedcolors.sh" ]; then
  (
    # si riprova finche' i colori non risultano creati (all'avvio il sistema a volte non e' ancora pronto
    # e i comandi falliscono in silenzio), poi due ripassate di sicurezza
    for t in 15 15 20 30 30 60 60; do
      sleep $t
      sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1
      n=$(cmd overlay list 2>/dev/null | grep -c "^\[x\] com.android.shell:oxyfix")
      [ "${n:-0}" -ge 1 ] && break
    done
    for t in 60 120; do
      sleep $t
      sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1
    done
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
