package com.example.carservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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
        // Register WebSocket handshake endpoint with CORS allowance for Angular
        registry.addEndpoint("/ws-servicemate")
                .setAllowedOriginPatterns("http://localhost:4200", "http://localhost:*")
                .withSockJS();
    }
}
