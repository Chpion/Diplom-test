package com.diplom.agafonov.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        logger.info("JwtAuthenticationFilter initialized");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {
        logger.debug("JwtAuthenticationFilter: Processing request for URI: {}", request.getRequestURI());

        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/webjars") ||
                requestURI.startsWith("/swagger-resources") ||
                requestURI.equals("/api/auth/login") ||
                requestURI.equals("/api/auth/register") ||
                requestURI.startsWith("/api/search/")){
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        logger.debug("Received Authorization header: {}", header);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            logger.debug("Validating JWT token: {}", token);
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String login = claims.getSubject();
                logger.debug("Extracted login from token: {}", login);
                if (login == null) {
                    logger.error("JWT token does not contain a subject (login)");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token does not contain a subject");
                    return;
                }

                List<?> rolesObj = claims.get("roles", List.class);
                logger.debug("Roles in token: {}", rolesObj);
                if (rolesObj == null) {
                    logger.error("JWT token does not contain roles");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token does not contain roles");
                    return;
                }

                List<SimpleGrantedAuthority> authorities = rolesObj.stream()
                        .filter(role -> role instanceof String)
                        .map(role -> {
                            String roleName = (String) role;
                            logger.debug("Processing role: {}", roleName);
                            return new SimpleGrantedAuthority(roleName); // Убедимся, что роль передаётся как есть
                        })
                        .collect(Collectors.toList());

                logger.debug("Authorities created: {}", authorities);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        login, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.debug("Authentication set for user: {} with authorities: {}", login, authorities);
            } catch (Exception e) {
                logger.error("JWT validation failed: {}", e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token: " + e.getMessage());
                return;
            }
        } else {
            logger.debug("No valid Authorization header or not a Bearer token for URI: {}", requestURI);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        chain.doFilter(request, response);
    }
}