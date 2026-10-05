#!/bin/bash
# Legge il tag spinto (es. "v1.2.3"), aggiorna versionCode/versionName in app/build.gradle.kts
# e prepara le variabili VTag/VName/ApkName per il resto di release.yml.
# versionCode si incrementa sempre di 1 rispetto a quello gia' nel repository (Android rifiuta
# di installare un APK con versionCode piu' basso di quello installato).

NEWVERNAME=${GITHUB_REF_NAME/v/}
CURRENT_VERCODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
NEWVERCODE=$((CURRENT_VERCODE + 1))

echo 'VTag<<EOF' >> $GITHUB_ENV
echo ${GITHUB_REF_NAME} >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV

echo 'VName<<EOF' >> $GITHUB_ENV
echo 'Oxydian Theme v'$NEWVERNAME >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV

echo "ApkName=OxydianTheme-release-$NEWVERNAME.apk" >> $GITHUB_ENV

sed -i 's/versionCode.*/versionCode    = '$NEWVERCODE'/' app/build.gradle.kts
sed -i 's/versionName    =.*/versionName    = "'$NEWVERNAME'"/' app/build.gradle.kts
