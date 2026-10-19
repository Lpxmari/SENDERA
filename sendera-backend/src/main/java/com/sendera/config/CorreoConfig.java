package com.sendera.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class CorreoConfig {

    @Bean
    @ConditionalOnExpression("'${app.mail.smtp.host:}' != ''")
    public JavaMailSender javaMailSender(
            @Value("${app.mail.smtp.host:}") String host,
            @Value("${app.mail.smtp.port:587}") int port,
            @Value("${app.mail.smtp.usuario:}") String usuario,
            @Value("${app.mail.smtp.contrasena:}") String contrasena) {

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(usuario);
        sender.setPassword(contrasena);
        sender.setDefaultEncoding("UTF-8");

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        sender.setJavaMailProperties(props);
        return sender;
    }
}
