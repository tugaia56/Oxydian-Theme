#!/system/bin/sh
# Oxydian Theme - controllo dei colori di sistema (Android 16/17)
# Quando SystemUI riparte (anche per un crash) il sistema rifa' i suoi colori automatici (neutral/accent/dynamic) e li mette
# in cima, sopra quelli scelti nel Theme (overlay "oxyfix"): l'accento sparisce dai tasti rapidi. Qui si controlla solo quando
# cambia il processo di SystemUI e, se serve, si rimettono i colori del Theme davanti (spegne e riaccende i suoi colori: solo cosi' tornano in cima).
MODDIR=${0%/*}
# OxygenOS: i colori del Theme sono decine di overlay; riaccenderli da qui blocca SystemUI e puo' innescare un ciclo di riavvii: non si fa niente
[ -n "$(getprop ro.build.version.oplusrom)" ] && exit 0
lastfix=0
LOG=/data/local/tmp/oxydian_theme.log
last=$(pidof com.android.systemui)
while true; do
  sleep 2
  [ -f "$MODDIR/fixedcolors.sh" ] || continue
  pid=$(pidof com.android.systemui)
  [ -n "$pid" ] && [ "$pid" != "$last" ] || continue
  last=$pid
  # al massimo una riparazione ogni 60 secondi (un ciclo di riavvii di SystemUI non deve ripetersi all'infinito): se e' troppo presto si aspetta, senza saltarla
  now=$(date +%s)
  wait=$((60 - (now - lastfix)))
  [ $wait -gt 0 ] && sleep $wait
  pid=$(pidof com.android.systemui); [ -n "$pid" ] && last=$pid
  # SystemUI rifa' i suoi colori in pochi secondi: per una ventina di secondi si controlla ogni secondo e, appena quelli del
  # sistema stanno davanti, si rimettono i colori del Theme (cosi' il ritardo e' minimo)
  n=0
  while [ $n -lt 20 ]; do
    sleep 1
    n=$((n+1))
    top=$(cmd overlay list --user 0 android 2>/dev/null | grep '^\[x\]' | tail -1)
    case "$top" in
      *oxyfix*) ;;
      *)
        lastfix=$(date +%s)
        echo "colorwatch: colori del sistema davanti a quelli del Theme ($top), li rimetto $(date)" >> "$LOG"
        sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1;;
    esac
  done
done
