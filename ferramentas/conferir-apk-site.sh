#!/usr/bin/env bash
# Confere o APK que vai para o site (somente leitura). Uso: bash ferramentas/conferir-apk-site.sh [caminho-do-apk]
# Falha (saida 1) se o APK for de debug, assinado com a chave de debug, com versao diferente da do Gradle ou da anunciada no site (apkVersion)
# ou maior que 12 MB. Rode antes de enviar o APK; site e APK sobem juntos.
set -u

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${1:-$RAIZ/site/wake-wake-up.apk}"
LIMITE=$((12 * 1024 * 1024))
erros=0
falha() { echo "FALHA: $*" >&2; erros=$((erros + 1)); }

[ -f "$APK" ] || { echo "FALHA: APK nao encontrado: $APK" >&2; exit 1; }

# Ferramentas do Android SDK (build-tools mais nova)
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${LOCALAPPDATA:-$HOME}/Android/Sdk}}"
BT="$(ls -d "$SDK"/build-tools/*/ 2>/dev/null | sort -V | tail -n 1)"
BT="${BT%/}"
AAPT2=""
for c in "$BT/aapt2.exe" "$BT/aapt2"; do [ -f "$c" ] && AAPT2="$c" && break; done
APKSIGNER="$BT/lib/apksigner.jar"
[ -n "$AAPT2" ] || { echo "FALHA: aapt2 nao encontrado em $SDK/build-tools/" >&2; exit 1; }
[ -f "$APKSIGNER" ] || { echo "FALHA: apksigner.jar nao encontrado em $BT/lib/" >&2; exit 1; }
command -v java >/dev/null 2>&1 || { echo "FALHA: java nao encontrado no PATH" >&2; exit 1; }

BADGING="$("$AAPT2" dump badging "$APK" 2>&1)" || { echo "FALHA: aapt2 nao leu o APK: $BADGING" >&2; exit 1; }

# 1. Nao pode ser build de debug
if echo "$BADGING" | grep -q "application-debuggable"; then
  falha "o APK e de debug (application-debuggable). Publique so o release assinado: bash ferramentas/gradle.sh :app:assembleRelease"
fi

# 2. Assinatura valida e diferente da chave de debug
CERTS="$(java -jar "$APKSIGNER" verify --print-certs "$APK" 2>&1)"
if [ $? -ne 0 ]; then
  falha "a assinatura do APK nao verifica: $(echo "$CERTS" | head -n 1)"
elif echo "$CERTS" | grep -q "CN=Android Debug"; then
  falha "o APK esta assinado com a chave de debug (CN=Android Debug). Assine com a chave de upload (keystore.properties)"
fi

# 3. Versao igual a do Gradle
VERSAO_APK="$(echo "$BADGING" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -n 1)"
VERSAO_GRADLE="$(sed -n 's/.*versionName *= *"\([^"]*\)".*/\1/p' "$RAIZ/app/build.gradle.kts" | head -n 1)"
if [ "$VERSAO_APK" != "$VERSAO_GRADLE" ]; then
  falha "versionName do APK ($VERSAO_APK) e diferente do de app/build.gradle.kts ($VERSAO_GRADLE)"
fi

# 3b. Versao anunciada no site (site-config.js) igual a do APK
VERSAO_SITE="$(sed -n 's/.*apkVersion *: *"\([^"]*\)".*//p' "$RAIZ/site/site-config.js" | head -n 1)"
if [ "$VERSAO_APK" != "$VERSAO_SITE" ]; then
  falha "o site anuncia a versao '$VERSAO_SITE' (apkVersion em site/site-config.js), mas o APK e '$VERSAO_APK'"
fi

# 4. Tamanho
BYTES="$(wc -c < "$APK" | tr -d ' ')"
if [ "$BYTES" -gt "$LIMITE" ]; then
  falha "o APK tem $BYTES bytes, mais que o limite de 12 MB ($LIMITE)"
fi

MB="$(awk -v b="$BYTES" 'BEGIN { printf "%.1f", b / 1048576 }')"
echo "APK: $APK"
echo "Versao: ${VERSAO_APK:-?} | Tamanho: $MB MB ($BYTES bytes)"
if [ "$erros" -gt 0 ]; then
  echo "RESULTADO: $erros problema(s). NAO envie este APK para o site." >&2
  exit 1
fi
echo "RESULTADO: ok, pode enviar junto com o site. Confira se site-config.js (apkVersion e apkSize) bate com estes valores."
