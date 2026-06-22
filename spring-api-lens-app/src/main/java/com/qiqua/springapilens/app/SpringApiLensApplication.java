package com.qiqua.springapilens.app;

import com.qiqua.springapilens.core.scanner.RepositoryScanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringApiLensApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringApiLensApplication.class, args);
    }

    @Bean
    RepositoryScanner repositoryScanner() {
        return new RepositoryScanner();
    }
}
