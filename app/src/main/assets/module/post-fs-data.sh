#!/system/bin/sh
# Oxydian Theme - post-fs-data
# Monta un overlay in RAM su /product/overlay con gli APK scelti nell'app (cartella "store"),
# PRIMA di system_server, cosi il sistema li trova alla scansione di avvio.
MODDIR=${0%/*}
SRC=$MODDIR/store
T=/mnt/oxydian_theme_ovl
TGT=/product/overlay
LOG=/data/local/tmp/oxydian_theme.log
BB=/data/adb/magisk/busybox
[ -x "$BB" ] || BB=/data/adb/ksu/bin/busybox
{
echo "=== oxydian_theme post-fs-data $(date) ==="
# Anche gli overlay del vecchio Oxydian (modulo Obsidian) passano da qui: due montaggi
# sovrapposti su /product/overlay si bloccano a vicenda a seconda di chi parte per primo.
OLD=/data/adb/modules/Obsidian/product/overlay
set -- "$SRC"/OxydianThemeComponent*.apk
[ -e "$1" ] || set -- "$OLD"/ObsidianComponent*.apk
[ -e "$1" ] || { echo "nessun APK in $SRC"; exit 0; }
mount 2>/dev/null | grep -q "oxytheme_ovl on $TGT " && umount "$TGT" 2>/dev/null
mount 2>/dev/null | grep -q " $T " && umount "$T" 2>/dev/null
mkdir -p "$T"
mount -t tmpfs -o mode=0755,size=64m tmpfs "$T" || { echo "tmpfs fallito"; exit 1; }
mkdir -p "$T/upper" "$T/work"
# La radice del montaggio prende l'etichetta SELinux della cartella "upper": con "tmpfs" system_server non puo' leggerla
# (ROM come crDroid) e non vede gli overlay. system_file e' quella delle cartelle overlay di sistema.
chmod 0755 "$T/upper"
chcon u:object_r:system_file:s0 "$T/upper" 2>/dev/null
for f in "$SRC"/OxydianThemeComponent*.apk; do
  b=${f##*/}
  cp -f "$f" "$T/upper/$b" && chown 0:0 "$T/upper/$b" && chmod 0644 "$T/upper/$b"
  chcon u:object_r:vendor_overlay_file:s0 "$T/upper/$b" 2>/dev/null || chcon u:object_r:system_file:s0 "$T/upper/$b" 2>/dev/null
done
for f in "$OLD"/ObsidianComponent*.apk; do
  [ -e "$f" ] || continue
  b=${f##*/}
  cp -f "$f" "$T/upper/$b" && chown 0:0 "$T/upper/$b" && chmod 0644 "$T/upper/$b"
  chcon u:object_r:vendor_overlay_file:s0 "$T/upper/$b" 2>/dev/null || chcon u:object_r:system_file:s0 "$T/upper/$b" 2>/dev/null
done
O="lowerdir=$TGT,upperdir=$T/upper,workdir=$T/work"
mount -t overlay oxytheme_ovl -o "$O" "$TGT" 2>&1 || "$BB" mount -t overlay oxytheme_ovl -o "$O" "$TGT" 2>&1 || { echo "overlay fallito"; umount "$T"; exit 1; }
# anche la radice del montaggio, per sicurezza (system_server deve poterla leggere)
chcon u:object_r:system_file:s0 "$TGT" 2>/dev/null
echo "montato:"; ls "$TGT" | grep -c OxydianThemeComponent
} >> "$LOG" 2>&1
