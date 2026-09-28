package com.coumba.websocket;

import com.coumba.security.CustomUserDetailsService;
import com.coumba.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("Interception d'une requête STOMP CONNECT");
            String token = extractToken(accessor);

            if (token == null || token.isBlank()) {
                log.warn("Échec de connexion WebSocket : Token JWT manquant dans les en-têtes STOMP ({})", accessor.toNativeHeaderMap());
                throw new AccessDeniedException("Token JWT manquant pour la connexion WebSocket");
            }

            try {
                String username = jwtService.extractUsername(token);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authentication);
                        log.info("Authentification WebSocket réussie pour l'utilisateur : {}", username);
                    } else {
                        log.error("Token JWT invalide ou expiré pour l'utilisateur : {}", username);
                        throw new AccessDeniedException("Token JWT invalide ou expiré");
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la validation du token WebSocket : {}", e.getMessage());
                throw new AccessDeniedException("Accès refusé : token invalide ou expiré");
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Chercher dans l'en-tête 'Authorization' (ex: "Bearer <token>")
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0);
            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
            return bearerToken;
        }

        // 2. Chercher dans l'en-tête 'token' personnalisé
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        // 3. Chercher dans 'passcode' STOMP (utilisé par certains clients)
        String passcode = accessor.getPasscode();
        if (passcode != null && !passcode.isBlank()) {
            return passcode;
        }

        // 4. Chercher dans les attributs de session (query parameter du handshake WebSocket)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
            Object tokenObj = sessionAttributes.get("token");
            if (tokenObj != null) {
                String tokenStr = tokenObj.toString();
                if (tokenStr.startsWith("Bearer ")) {
                    return tokenStr.substring(7);
                }
                return tokenStr;
            }
        }

        return null;
    }
}
