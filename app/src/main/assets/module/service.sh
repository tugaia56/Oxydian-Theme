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
    cmd overlay enable --user current "$n" 2>&1 && cmd overlay set-priority "$n" highest 2>&1
    echo "abilitato $n"
  else
    echo "non ancora registrato $n (serve un riavvio)"
  fi
done
echo "=== fine ==="
} >> "$LOG" 2>&1
