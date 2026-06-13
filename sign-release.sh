#!/bin/bash
# Script: sign-release.sh
# Uso: ./sign-release.sh path/to/app-release-unsigned.apk 1.8.1

APK_PATH=$1
VERSION=$2

KEYSTORE="release.jks"
KEYSTORE_PASS="PorraWC2026!"
KEY_ALIAS="porrawc2026"
KEY_PASS="PorraWC2026!"
OUTPUT="PorraWC2026-v${VERSION}.apk"

if [ -z "$APK_PATH" ] || [ -z "$VERSION" ]; then
    echo "Uso: ./sign-release.sh <path-to-unsigned-apk> <version>"
    echo "Ejemplo: ./sign-release.sh app/build/outputs/apk/release/app-release-unsigned.apk 1.8.1"
    exit 1
fi

if [ ! -f "$KEYSTORE" ]; then
    echo "Error: No se encuentra $KEYSTORE"
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "Error: No se encuentra $APK_PATH"
    exit 1
fi

echo "Firmando APK..."
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$KEYSTORE_PASS" \
  --key-pass "pass:$KEY_PASS" \
  --out "$OUTPUT" \
  "$APK_PATH"

if [ $? -eq 0 ]; then
    echo "APK firmado: $OUTPUT"
    echo "Verificando firma..."
    apksigner verify --verbose "$OUTPUT"
else
    echo "Error al firmar el APK"
    exit 1
fi
