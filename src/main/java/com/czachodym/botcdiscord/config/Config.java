package com.czachodym.botcdiscord.config;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class Config {
    @Value("${discord.token}")
    private String discordToken;

    @Bean
    public JDA jda() throws InterruptedException {
        return JDABuilder
                .createDefault(discordToken)
                .build()
                .awaitReady();
    }
}
