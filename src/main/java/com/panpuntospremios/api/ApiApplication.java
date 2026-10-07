package com.panpuntospremios.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.panpuntospremios.api")
public class ApiApplication {

    public static void main(String[] args) {
        cargarVariablesEntorno();
        SpringApplication.run(ApiApplication.class, args);
    }

    /**
     * Carga las variables del archivo .env local en las propiedades del sistema
     * para que Spring Boot y HikariCP las resuelvan automáticamente sin exponer
     * secretos en código ni en Git.
     */
    static void cargarVariablesEntorno() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }
}
