#!/bin/sh
set -e

echo "=================================================="
echo "=== RUNTIME CONTAINER SYSTEM NETWORK DIAGNOSTIC ==="
echo "=================================================="

echo ""
echo "--- [CLI Diagnostic] Testing DNS Resolution with getent ---"
getent ahosts mysql-1326c09a-servicemate.f.aivencloud.com || echo "DNS resolution check"

echo ""
echo "--- [CLI Diagnostic] Testing TCP Port 24035 with Netcat ---"
nc -zv -w 5 mysql-1326c09a-servicemate.f.aivencloud.com 24035 2>&1 || echo "Netcat connection test completed"

echo ""
echo "--- [CLI Diagnostic] Testing OpenSSL MySQL STARTTLS Handshake ---"
(echo "QUIT" | openssl s_client -starttls mysql -connect mysql-1326c09a-servicemate.f.aivencloud.com:24035 2>&1 | grep -E "SSL handshake|Protocol|Cipher|Verify return code|subject=|CONNECTED" || true)

echo ""
echo "--- [Java Diagnostic] Running Java Runtime Network Diagnostic ---"
java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -cp /app/diagnostic-classes com.example.carservice.diagnostic.RuntimeNetworkDiagnostic || true

echo "=================================================="
echo "=== LAUNCHING SPRING BOOT APPLICATION ==="
echo "=================================================="
exec java \
  -Djava.net.preferIPv4Stack=true \
  -Djava.net.preferIPv4Addresses=true \
  -Dspring.datasource.url="${DB_URL:-$SPRING_DATASOURCE_URL}" \
  -Dspring.datasource.username="${DB_USERNAME:-root}" \
  -Dspring.datasource.password="${DB_PASSWORD:-}" \
  -jar app.jar
