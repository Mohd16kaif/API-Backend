package com.apishield.config;

import com.apishield.model.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToCurrencyConverter());
    }

    public static class StringToCurrencyConverter implements Converter<String, User.Currency> {
        @Override
        public User.Currency convert(String source) {
            try {
                return User.Currency.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid currency: " + source);
            }
        }
    }
}