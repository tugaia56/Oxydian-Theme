#!/system/bin/sh
# Oxydian Theme - controllo dei colori di sistema (Android 16/17 e OxygenOS)
# Quando SystemUI riparte (anche per un crash) il sistema rifa' i suoi colori automatici (neutral/accent/dynamic) e li mette
# in cima, sopra quelli scelti nel Theme: l'accento sparisce dai tasti rapidi, dal chip di Telefono ecc. Qui si controlla solo quando
# cambia il processo di SystemUI e, se serve, si rimettono i colori del Theme davanti (spegne e riaccende i suoi colori: solo cosi' tornano in cima).
#  - Android puro: gli overlay "oxyfix" li rifa' fixedcolors.sh.
#  - OxygenOS: bastano il tema di sistema del Theme (DS_SYS) e gli "oxyfix" (una quindicina di overlay, 2 secondi). Gli ObsidianComponent*
#    (decine) NON si toccano: riaccenderli tutti blocca SystemUI e puo' innescare un ciclo di riavvii.
MODDIR=${0%/*}
OPLUS=$(getprop ro.build.version.oplusrom)
lastfix=0
LOG=/data/local/tmp/oxydian_theme.log
last=$(pidof com.android.systemui)

# colori del Theme in cima (OxygenOS): solo DS_SYS e gli oxyfix
repair_oos() {
  L="OxydianThemeComponentDS_SYS.overlay $(cmd overlay list --user 0 android 2>/dev/null | grep -o 'com.android.shell:oxyfix[0-9]*' | tr '\n' ' ')"
  for o in $L; do cmd overlay disable --user 0 "$o" >/dev/null 2>&1; done
  for o in $L; do cmd overlay enable --user 0 "$o" >/dev/null 2>&1; done
}

while true; do
  sleep 2
  pid=$(pidof com.android.systemui)
  [ -n "$pid" ] && [ "$pid" != "$last" ] || continue
  last=$pid
  # al massimo una riparazione ogni 60 secondi (un ciclo di riavvii di SystemUI non deve ripetersi all'infinito): se e' troppo presto si aspetta, senza saltarla
  now=$(date +%s)
  wait=$((60 - (now - lastfix)))
  [ $wait -gt 0 ] && sleep $wait
  pid=$(pidof com.android.systemui); [ -n "$pid" ] && last=$pid
  # SystemUI rifa' i suoi colori in pochi secondi. OxygenOS: si lascia prima finire l'avvio (12 s) e poi si controlla ogni 2 secondi per 40;
  # Android puro: si controlla ogni secondo per 20. Appena quelli del sistema stanno davanti, si rimettono quelli del Theme.
  if [ -n "$OPLUS" ]; then step=2; max=20; sleep 12; else step=1; max=20; [ -f "$MODDIR/fixedcolors.sh" ] || continue; fi
  n=0
  while [ $n -lt $max ]; do
    sleep $step
    n=$((n+1))
    top=$(cmd overlay list --user 0 android 2>/dev/null | grep '^\[x\]' | tail -1)
    case "$top" in
      *com.android.systemui:*)
        lastfix=$(date +%s)
        echo "colorwatch: colori del sistema davanti a quelli del Theme ($top), li rimetto $(date)" >> "$LOG"
        if [ -n "$OPLUS" ]; then repair_oos >> "$LOG" 2>&1; else sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1; fi
        break;;
    esac
  done
done
