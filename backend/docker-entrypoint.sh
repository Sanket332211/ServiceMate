#!/bin/sh

echo "=================================================="
echo "=== RUNTIME CONTAINER SYSTEM NETWORK SETUP ==="
echo "=================================================="

# 1. Ensure public DNS resolvers exist in /etc/resolv.conf if possible
if [ -w /etc/resolv.conf ]; then
    echo "Configuring public DNS fallbacks in /etc/resolv.conf..."
    echo "nameserver 8.8.8.8" >> /etc/resolv.conf 2>/dev/null || true
    echo "nameserver 1.1.1.1" >> /etc/resolv.conf 2>/dev/null || true
fi

# 2. Check and fix DNS resolution for Aiven Cloud MySQL
AIVEN_HOST="mysql-1326c09a-servicemate.f.aivencloud.com"
AIVEN_DEFAULT_IP="168.144.210.53"

echo ""
echo "--- Testing DNS resolution for ${AIVEN_HOST} ---"
if getent ahosts "${AIVEN_HOST}" >/dev/null 2>&1; then
    echo "DNS resolution succeeded for ${AIVEN_HOST}"
else
    echo "Standard DNS resolution failed for ${AIVEN_HOST}. Attempting public DNS lookup..."
    RESOLVED_IP=$(nslookup "${AIVEN_HOST}" 8.8.8.8 2>/dev/null | awk '/^Address: / { print $2 }' | tail -n1 || true)
    if [ -z "$RESOLVED_IP" ]; then
        RESOLVED_IP="${AIVEN_DEFAULT_IP}"
    fi
    echo "Mapping ${RESOLVED_IP} -> ${AIVEN_HOST} in /etc/hosts"
    echo "${RESOLVED_IP} ${AIVEN_HOST}" >> /etc/hosts 2>/dev/null || true
fi

echo ""
echo "--- Testing TCP Port 24035 with Netcat ---"
nc -zv -w 5 "${AIVEN_HOST}" 24035 2>&1 || echo "Netcat connection test completed"

echo ""
echo "--- Running Java Runtime Network Diagnostic ---"
java -Djava.net.preferIPv4Addresses=true -cp /app/diagnostic-classes com.example.carservice.diagnostic.RuntimeNetworkDiagnostic || true

echo "=================================================="
echo "=== LAUNCHING SPRING BOOT APPLICATION ==="
echo "=================================================="
exec java \
  -Djava.net.preferIPv4Addresses=true \
  -jar app.jar

