package edu.m4z.secrets.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Demo application for Secret Manager Enabler.
 * 
 * <p>Demonstrates various features:
 * <ul>
 *   <li>@Secret annotation on fields</li>
 *   <li>${secret://} placeholders in configuration</li>
 *   <li>Secret rotation detection and handling</li>
 *   <li>DataSource reconfiguration on password rotation</li>
 * </ul>
 * 
 * @author Mehrez Ben Salem
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "edu.m4z.secrets")
@EnableScheduling
public class SecretManagerDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecretManagerDemoApplication.class, args);
    }
}