package fatec.fusex.nexus.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // Habilita o @PreAuthorize nos Controllers
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita CSRF pois é uma API REST (não usamos formulários HTML)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // PROVISÓRIO: Libera todas as rotas HTTP para testes no Postman
                        // O @PreAuthorize nos Controllers ainda vai exigir o perfil correto!
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}