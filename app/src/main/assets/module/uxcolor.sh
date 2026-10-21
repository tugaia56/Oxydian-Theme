#!/system/bin/sh
# Oxydian Theme - rimette i colori di sistema scelti (ux_custom_color*.xml) se dopo l'avvio il sistema li ha cambiati,
# e in quel caso riavvia il launcher, che li legge una volta sola quando parte (altrimenti il pulsante "Chiudi" dei recenti
# resta con il colore vecchio).
D=/data/oplus/uxres/uxcolor
M=${0%/*}/uxcolor
[ -d "$M" ] || exit 0
CH=0
for f in ux_custom_color.xml ux_custom_color_night.xml; do
  [ -f "$M/$f" ] || continue
  if ! cmp -s "$M/$f" "$D/$f"; then
    cp -f "$M/$f" "$D/$f" && chmod 644 "$D/$f"
    CH=1
  fi
done
[ "$CH" = 1 ] && killall com.android.launcher
exit 0
