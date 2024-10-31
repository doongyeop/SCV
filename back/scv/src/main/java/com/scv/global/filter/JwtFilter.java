package com.scv.global.filter;

import com.scv.domain.oauth2.CustomOAuth2User;
import com.scv.domain.oauth2.dto.OAuth2UserDTO;
import com.scv.domain.user.domain.User;
import com.scv.domain.user.exception.UserNotFoundException;
import com.scv.domain.user.repository.UserRepository;
import com.scv.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${spring.jwt.token.access.name}")
    private String ACCESS_TOKEN_NAME;

    @Value("${spring.jwt.token.refresh.name}")
    private String REFRESH_TOKEN_NAME;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = null;
        String refreshToken = null;

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(ACCESS_TOKEN_NAME)) {
                accessToken = cookie.getValue();
            } else if (cookie.getName().equals(REFRESH_TOKEN_NAME)) {
                refreshToken = cookie.getValue();
            }
        }

        if (accessToken == null) {
            return;
        }

        String userUuid = jwtUtil.getUserUuid(accessToken);
        User user = userRepository.findByUserUuid(userUuid).orElseThrow(UserNotFoundException::getInstance);

        OAuth2UserDTO oAuth2UserDTO = OAuth2UserDTO.builder()
                .userId(user.getUserId())
                .userUuid(user.getUserUuid())
                .userEmail(user.getUserEmail())
                .userImageUrl(user.getUserImageUrl())
                .userNickname(user.getUserNickname())
                .userCreatedAt(user.getUserCreatedAt())
                .userUpdatedAt(user.getUserUpdatedAt())
                .userIsDeleted(user.isUserIsDeleted())
                .build();

        CustomOAuth2User customOAuth2User = new CustomOAuth2User(oAuth2UserDTO);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
