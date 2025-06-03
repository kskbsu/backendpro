package com.backend.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.Customizer; // Customizer는 formLogin 람다식 내부에서 사용되므로, 직접적인 import는 필요 없을 수 있습니다.
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Spring Security 활성화
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt 해시 함수를 사용한 PasswordEncoder
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화 (API 서버로만 사용 시 고려, 실제 서비스에서는 적절한 설정 필요)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/auth/**", "/ws-chat/**", "/h2-console/**").permitAll() // 인증/인가 API, 웹소켓, H2 콘솔은 인증 없이 허용
                                .requestMatchers("/login.html", "/signup.html", "/css/**", "/js/**", "/favicon.ico").permitAll() // 로그인, 회원가입 페이지 및 정적 리소스 허용
                                .requestMatchers("/").authenticated() // 루트 경로 (index.html)는 인증 필요
                                .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )
                .formLogin(formLogin ->
                        formLogin
                                .loginPage("/login.html") // 사용자 정의 로그인 페이지 경로
                                .loginProcessingUrl("/login") // 로그인 처리 URL (폼이 제출될 경로, 기본값)
                                .defaultSuccessUrl("/index.html", true) // 로그인 성공 시 이동할 기본 URL (true: 항상 이 URL로 강제 이동)
                                .failureUrl("/login.html?error=true") // 로그인 실패 시 이동할 URL
                                .permitAll() // 로그인 페이지 관련 경로는 모두 허용
                )
                .logout(logout -> // 로그아웃 설정 추가 (선택 사항)
                        logout
                                .logoutUrl("/logout") // 로그아웃 처리 URL
                                .logoutSuccessUrl("/login.html?logout") // 로그아웃 성공 시 이동할 URL
                                .invalidateHttpSession(true) // HTTP 세션 무효화
                                .deleteCookies("JSESSIONID") // JSESSIONID 쿠키 삭제 (세션 기반 인증 시)
                                .permitAll()
                )
                .headers(headers -> // H2 콘솔 프레임 허용을 위한 설정
                        headers.frameOptions(frameOptions ->
                                frameOptions.sameOrigin()
                        )
                );

        return http.build();
    }
}
