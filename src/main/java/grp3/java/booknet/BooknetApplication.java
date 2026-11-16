package grp3.java.booknet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BooknetApplication {

    public static void main(String[] args) {
        SpringApplication.run(BooknetApplication.class, args);
    }

}
