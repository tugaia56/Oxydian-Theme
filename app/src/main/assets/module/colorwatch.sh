#!/system/bin/sh
# Oxydian Theme - controllo dei colori di sistema (Android 16/17)
# Quando SystemUI riparte (anche per un crash) il sistema rifa' i suoi colori automatici (neutral/accent/dynamic) e li mette
# in cima, sopra quelli scelti nel Theme (overlay "oxyfix"): l'accento sparisce dai tasti rapidi. Qui si controlla solo quando
# cambia il processo di SystemUI e, se serve, si rimettono i colori del Theme davanti (spegne e riaccende i suoi colori: solo cosi' tornano in cima).
MODDIR=${0%/*}
LOG=/data/local/tmp/oxydian_theme.log
last=$(pidof com.android.systemui)
while true; do
  sleep 2
  [ -f "$MODDIR/fixedcolors.sh" ] || continue
  pid=$(pidof com.android.systemui)
  [ -n "$pid" ] && [ "$pid" != "$last" ] || continue
  last=$pid
  # SystemUI rifa' i suoi colori in pochi secondi: per una ventina di secondi si controlla ogni secondo e, appena quelli del
  # sistema stanno davanti, si rimettono i colori del Theme (cosi' il ritardo e' minimo)
  n=0
  while [ $n -lt 20 ]; do
    sleep 1
    n=$((n+1))
    top=$(cmd overlay list --user 0 android 2>/dev/null | grep '^\[x\]' | tail -1)
    # i colori automatici del sistema (com.android.systemui:neutral/accent/dynamic) stanno davanti: si rimettono davanti quelli del Theme
    case "$top" in
      *com.android.systemui:*)
        echo "colorwatch: colori del sistema davanti a quelli del Theme ($top), li rimetto $(date)" >> "$LOG"
        # Android puro e OxygenOS: gli overlay oxyfix del Theme
        sh "$MODDIR/fixedcolors.sh" >> "$LOG" 2>&1
        # OxygenOS: i colori del Theme sono anche gli overlay ObsidianComponent* e DS_SYS (che stanno sopra gli oxyfix): spegni e riaccendi in ordine
        obs=$(cmd overlay list --user 0 android 2>/dev/null | grep -o -E 'com.android.shell:ObsidianComponent[A-Za-z0-9_]*|OxydianThemeComponentDS_SYS.overlay')
        if [ -n "$obs" ]; then
          for o in $obs; do cmd overlay disable --user 0 "$o" >/dev/null 2>&1; done
          for o in $obs; do cmd overlay enable --user 0 "$o" >/dev/null 2>&1; done
        fi;;
    esac
  done
done
