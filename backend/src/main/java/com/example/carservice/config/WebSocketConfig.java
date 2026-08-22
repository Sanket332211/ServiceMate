package com.example.carservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WebSocketConfig (Phase 1 Foundation)
 *
 * Configures real-time WebSocket communication using STOMP.
 * - Endpoint: /ws-servicemate (with SockJS fallback)
 * - Message Broker: /topic (broadcasts) and /queue (user-specific notifications)
 * - Application prefix: /app
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:${FRONTEND_URL:http://localhost:4200}}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory message broker destination prefixes for client subscriptions
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages sent from client to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-targeted private messages
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> patterns = new ArrayList<>(List.of("http://localhost:4200", "http://localhost:*"));
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !patterns.contains(s))
                    .forEach(patterns::add);
        }

        // Register WebSocket handshake endpoint with CORS allowance
        registry.addEndpoint("/ws-servicemate")
                .setAllowedOriginPatterns(patterns.toArray(new String[0]))
                .withSockJS();
    }
}
