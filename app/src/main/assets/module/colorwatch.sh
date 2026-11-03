#!/system/bin/sh
# Oxydian Theme - controllo dei colori di sistema (Android 16/17)
# Quando SystemUI riparte (anche per un crash) il sistema rifa' i suoi colori automatici (neutral/accent/dynamic) e li mette
# in cima, sopra quelli scelti nel Theme (overlay "oxyfix"): l'accento sparisce dai tasti rapidi. Qui si controlla solo quando
# cambia il processo di SystemUI e, se serve, si rimettono i colori del Theme davanti (spegne e riaccende i suoi colori: solo cosi' tornano in cima).
MODDIR=${0%/*}
LOG=/data/local/tmp/oxydian_theme.log
last=$(pidof com.android.systemui)
while true; do
  sleep 20
  [ -f "$MODDIR/fixedcolors.sh" ] || continue
  pid=$(pidof com.android.systemui)
  [ -n "$pid" ] && [ "$pid" != "$last" ] || continue
  last=$pid
  # SystemUI ha bisogno di qualche secondo per rifare i suoi colori
  sleep 30
  top=$(cmd overlay list --user 0 android 2>/dev/null | grep '^\[x\]' | tail -1)
  case "$top" in
    *oxyfix*) ;;
    *)
      echo "colorwatch: colori del sistema davanti a quelli del Theme ($top), li rimetto $(date)" >> "$LOG"
      sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1;;
  esac
done
