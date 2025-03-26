package com.microservice.document_processing_service.config;

import com.microservice.document_processing_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/documents")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                SimpMessageType simpMessageType = (SimpMessageType) message.getHeaders().get("simpMessageType");
                System.out.println("WebSocketConfig: Processing message type: " + simpMessageType);
                System.out.println("WebSocketConfig: Headers: " + message.getHeaders());

                try {
                    if (SimpMessageType.CONNECT.equals(simpMessageType)) {
                        // Извлекаем nativeHeaders
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> nativeHeaders = (Map<String, List<String>>) message.getHeaders().get("nativeHeaders");
                        String authHeader = null;

                        if (nativeHeaders != null && nativeHeaders.containsKey("Authorization")) {
                            List<String> authHeaderList = nativeHeaders.get("Authorization");
                            if (authHeaderList != null && !authHeaderList.isEmpty()) {
                                authHeader = authHeaderList.get(0); // Берем первый элемент списка
                            }
                        }

                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            System.out.println("WebSocketConfig: Validating token: " + token);
                            if (!jwtUtil.validateToken(token)) {
                                System.out.println("WebSocketConfig: Invalid JWT token");
                                throw new SecurityException("Invalid JWT token");
                            }
                            System.out.println("WebSocketConfig: Token validated successfully");
                        } else {
                            System.out.println("WebSocketConfig: No JWT token provided");
                            throw new SecurityException("No JWT token provided");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("WebSocketConfig: Error during message processing: " + e.getMessage());
                    throw e;
                }

                System.out.println("WebSocketConfig: Message processed successfully: " + simpMessageType);
                return message;
            }
        });
    }
}