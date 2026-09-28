package com.coumba.websocket;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Préfixe pour les messages diffusés du serveur vers les clients abonnés
        config.enableSimpleBroker("/topic");
        // Préfixe pour les messages envoyés des clients vers les contrôleurs
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        HandshakeInterceptor queryParamInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                          WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    HttpServletRequest req = servletRequest.getServletRequest();
                    String token = req.getParameter("token");
                    if (token == null) {
                        token = req.getParameter("access_token");
                    }
                    if (token != null && !token.isBlank()) {
                        attributes.put("token", token);
                    }
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
            }
        };

        // Endpoint WebSocket standard
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(queryParamInterceptor);

        // Endpoint avec fallback SockJS pour compatibilité navigateurs Web
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(queryParamInterceptor)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Validation du token JWT lors de l'établissement de la connexion STOMP
        registration.interceptors(authChannelInterceptor);
    }
}
