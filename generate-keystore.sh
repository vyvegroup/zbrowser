#!/bin/bash
# Generate a release keystore for ZBrowser
# Usage: ./generate-keystore.sh

set -e

KEYSTORE_NAME="release.keystore"
KEY_ALIAS="zbrowser"
VALIDITY=10000
KEYSIZE=2048

echo "🔑 ZBrowser Keystore Generator"
echo "================================"
echo ""

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo "❌ keytool not found. Please install JDK first."
    exit 1
fi

# Check if keystore already exists
if [ -f "$KEYSTORE_NAME" ]; then
    echo "⚠️  Keystore file '$KEYSTORE_NAME' already exists!"
    read -p "Overwrite? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
    rm "$KEYSTORE_NAME"
fi

echo "Generating keystore..."
echo ""

keytool -genkey -v \
    -keystore "$KEYSTORE_NAME" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize "$KEYSIZE" \
    -validity "$VALIDITY" \
    -storetype PKCS12

echo ""
echo "✅ Keystore generated: $KEYSTORE_NAME"
echo ""

# Generate base64 for GitHub Actions
BASE64_FILE="keystore_base64.txt"
base64 -w 0 "$KEYSTORE_NAME" > "$BASE64_FILE"

echo "📋 GitHub Actions Setup"
echo "========================"
echo ""
echo "Add the following secrets to your GitHub repository:"
echo ""
echo "1. KEYSTORE_BASE64"
echo "   Value: Contents of $BASE64_FILE"
echo ""
echo "2. KEYSTORE_PASSWORD"
echo "   Value: The store password you entered above"
echo ""
echo "3. KEY_ALIAS"
echo "   Value: $KEY_ALIAS"
echo ""
echo "4. KEY_PASSWORD"
echo "   Value: The key password you entered above"
echo ""

# Create keystore.properties template
cat > keystore.properties << EOF
storeFile=$KEYSTORE_NAME
storePassword=YOUR_STORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
EOF

echo "📝 Template 'keystore.properties' created. Update with your passwords."
echo ""
echo "⚠️  IMPORTANT: Add these files to .gitignore:"
echo "   - $KEYSTORE_NAME"
echo "   - $BASE64_FILE"
echo "   - keystore.properties"
