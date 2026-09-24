package fatec.fusex.nexus.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Criptografia de senha. NUNCA salve senha pura no banco:
 *   beneficiario.setSenha(passwordEncoder.encode(senhaDigitada));
 * (Login/JWT ainda nao esta implementado: veja docs/DECISOES_PENDENTES.md)
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
