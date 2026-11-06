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
  # gli oxyfix hanno bersagli diversi (android e SystemUI): si prendono tutti
  L="OxydianThemeComponentDS_SYS.overlay $(cmd overlay list --user 0 2>/dev/null | grep -o 'com.android.shell:oxyfix[0-9]*' | tr '\n' ' ')"
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
  fixes=0
  while [ $n -lt $max ]; do
    sleep $step
    n=$((n+1))
    list=$(cmd overlay list --user 0 android 2>/dev/null | grep '^\[x\]')
    top=$(echo "$list" | tail -1)
    bad=0
    case "$top" in *com.android.systemui:*) bad=1;; esac
    # OxygenOS: puo' stare in cima un oxyfix ma il tema di sistema (DS_SYS) sotto i colori del sistema: va rimesso sopra anche in quel caso
    if [ "$bad" = 0 ] && [ -n "$OPLUS" ]; then
      dsn=$(echo "$list" | grep -n 'OxydianThemeComponentDS_SYS' | head -1 | cut -d: -f1)
      sun=$(echo "$list" | grep -n 'com.android.systemui:dynamic' | head -1 | cut -d: -f1)
      [ -n "$dsn" ] && [ -n "$sun" ] && [ "$dsn" -lt "$sun" ] && bad=1
    fi
    # anche tra gli overlay di SystemUI: il tema di SystemUI (DS_SUT) riacceso dopo gli oxyfix delle icone di stato li copre
    if [ "$bad" = 0 ]; then
      lsu=$(cmd overlay list --user 0 com.android.systemui 2>/dev/null | grep '^\[x\]')
      if echo "$lsu" | grep -q oxyfix && ! echo "$lsu" | tail -1 | grep -q oxyfix; then bad=1; top=$(echo "$lsu" | tail -1); fi
    fi
    case "$bad" in
      1)
        lastfix=$(date +%s)
        echo "colorwatch: colori del sistema davanti a quelli del Theme ($top), li rimetto $(date)" >> "$LOG"
        if [ -n "$OPLUS" ]; then repair_oos >> "$LOG" 2>&1; else sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1; fi
        # il sistema puo' rifare i suoi colori ancora dopo: si continua a controllare per un altro giro (al massimo 3 riparazioni)
        fixes=$((fixes+1))
        [ $fixes -ge 3 ] && break
        n=0;;
    esac
  done
done
