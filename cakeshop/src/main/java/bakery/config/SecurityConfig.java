package bakery.config;

import bakery.controller.OAuth2LoginSuccessHandler;
import bakery.config.LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private OAuth2LoginSuccessHandler oauth2SuccessHandler;

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // PUBLIC
                        .requestMatchers("/", "/login", "/register", "/error",
                                "/forgot-password", "/reset-password").permitAll()

                        .requestMatchers("/css/**", "/js/**", "/img/**",
                                "/static/**", "/webjars/**", "/uploads/**").permitAll()

                        .requestMatchers("/api/cart/**", "/api/order/**",
                                "/search", "/product/**", "/category/**", "/blogs/**").permitAll()

                        .requestMatchers("/oauth2/**").permitAll()

                        // ROLE
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/shipper/**").hasRole("SHIPPER")

                        .requestMatchers("/account/**", "/update/**")
                        .hasAnyRole("CUSTOMER", "ADMIN", "SHIPPER")

                        .anyRequest().authenticated()
                )

                // ✅ FIX LOGIN Ở ĐÂY
                .formLogin(form -> form
                        .loginPage("/login")

                        // 👉 QUAN TRỌNG: ép luôn về "/" sau login
                        .defaultSuccessUrl("/", true)

                        // 👉 vẫn giữ handler để phân role
                        .successHandler(loginSuccessHandler)

                        .failureHandler((request, response, exception) -> {
                            String error = "error";
                            if (exception instanceof org.springframework.security.authentication.DisabledException) {
                                error = "disabled";
                            }
                            response.sendRedirect("/login?error=" + error);
                        })
                        .permitAll()
                )

                // OAuth2
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oauth2SuccessHandler)
                )

                // LOGOUT
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                // 403
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                );

        return http.build();
    }
}