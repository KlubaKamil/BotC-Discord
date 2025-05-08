package com.czachodym.botcdiscord.service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DiscordService extends ListenerAdapter {
    private final JDA jda;

    public DiscordService(JDA jda) {
        this.jda = jda;

        jda.addEventListener(this);
        registerCommands(jda);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event){
        switch (event.getName()) {
            case "test" -> test(event);
        }
    }

    private void registerCommands(JDA jda){
        jda.upsertCommand("test", "ebe ebe")
                .queue();
    }

    private void test(SlashCommandInteractionEvent event){
        event.reply("Niezły event wariacie").queue();
    }
}
