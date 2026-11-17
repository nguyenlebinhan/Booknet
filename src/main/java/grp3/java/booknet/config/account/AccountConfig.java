package grp3.java.booknet.config.account;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AccountConfig {
    @Bean
    public Clock accountClock() {
        return Clock.systemUTC();
    }
}
