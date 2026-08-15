package com.example.trainbooking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TrafficControlProperties.class)
public class AppConfig {
}
