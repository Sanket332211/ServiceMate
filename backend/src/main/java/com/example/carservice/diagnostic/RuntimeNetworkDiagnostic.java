package com.example.carservice.diagnostic;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Temporary runtime diagnostic to inspect JVM network configuration,
 * DNS resolution, and TCP socket connectivity to Aiven MySQL from the
 * final running container on Render.
 *
 * Contains ZERO credentials, passwords, or secrets.
 */
public class RuntimeNetworkDiagnostic {

    public static void main(String[] args) {
        String host = "mysql-1326c09a-servicemate.f.aivencloud.com";
        int port = 24035;

        System.out.println("==================================================");
        System.out.println("=== RUNTIME CONTAINER JAVA NETWORK DIAGNOSTIC ===");
        System.out.println("==================================================");

        // D. JVM Network Properties
        System.out.println("\n[1] JVM System Properties:");
        System.out.println("  java.net.preferIPv4Stack = " + System.getProperty("java.net.preferIPv4Stack"));
        System.out.println("  java.net.preferIPv4Addresses = " + System.getProperty("java.net.preferIPv4Addresses"));
        System.out.println("  java.version = " + System.getProperty("java.version"));
        System.out.println("  os.name = " + System.getProperty("os.name"));

        // A & C. Java DNS & Address Resolution
        System.out.println("\n[2] DNS & InetAddress Resolution for " + host + ":");
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            System.out.println("  Total resolved addresses: " + addresses.length);
            for (int i = 0; i < addresses.length; i++) {
                InetAddress addr = addresses[i];
                String type = (addr instanceof Inet4Address) ? "IPv4" : (addr instanceof Inet6Address) ? "IPv6" : "Unknown";
                System.out.println("  Address [" + (i + 1) + "]: " + addr.getHostAddress() + " (" + type + ")");
            }

            // B. Direct Java Socket TCP tests to each resolved address
            System.out.println("\n[3] Direct TCP Socket Connection Tests to port " + port + ":");
            for (InetAddress addr : addresses) {
                String type = (addr instanceof Inet4Address) ? "IPv4" : "IPv6";
                System.out.print("  Connecting to " + addr.getHostAddress() + " (" + type + "):" + port + " ... ");
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(addr, port), 5000);
                    System.out.println("SUCCESS (TCP connection established)");
                } catch (Exception e) {
                    System.out.println("FAILED -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }

            // Test connecting by hostname directly (the exact way JDBC/Hikari connects)
            System.out.print("\n[4] Connecting by Hostname (" + host + ":" + port + ") ... ");
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 5000);
                System.out.println("SUCCESS (TCP connection established via hostname)");
            } catch (Exception e) {
                System.out.println("FAILED -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("  DNS Resolution FAILED: " + e.getMessage());
        }

        System.out.println("\n==================================================");
        System.out.println("=== END RUNTIME CONTAINER JAVA DIAGNOSTIC ===");
        System.out.println("==================================================\n");
    }
}
