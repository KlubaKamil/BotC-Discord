package com.czachodym.botcdiscord.service;

import com.czachodym.botcshared.dto.DiscordChannel;
import com.czachodym.botcshared.dto.DiscordGuild;
import com.czachodym.botcshared.dto.DiscordNotification;
import com.czachodym.botcshared.dto.DiscordThread;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DiscordService extends ListenerAdapter {
    private final JDA jda;

    public DiscordService(JDA jda) {
        this.jda = jda;

//        jda.addEventListener(this);
//        registerCommands(jda);
    }

//    @Override
//    public void onSlashCommandInteraction(SlashCommandInteractionEvent event){
//        switch (event.getName()) {
//            case "test" -> test(event);
//        }
//    }

    public List<DiscordGuild> getDiscordGuilds(){
        return jda.getGuilds().stream()
                .map(g -> DiscordGuild.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .channels(getDiscordChannels(g, g.getName().toLowerCase().contains("blood")))
                        .build())
                .toList();
    }

    private List<DiscordChannel> getDiscordChannels(Guild guild, boolean returnAll){
        return guild.getChannels().stream()
                .filter(c -> ((c.getType() == ChannelType.TEXT && ((TextChannel)c).canTalk() && (returnAll || c.getName().toLowerCase().contains("blood"))) ||
                        c.getType() == ChannelType.FORUM))
                .map(c -> DiscordChannel.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .channelType(c.getType())
                        .threads(getDiscordThreads(c, c.getName().toLowerCase().contains("blood")))
                        .build())
                .filter(dc -> dc.channelType() != ChannelType.FORUM || dc.threads().size() > 0)
                .toList();
    }

    private List<DiscordThread> getDiscordThreads(GuildChannel channel, boolean returnAll){
        if (channel.getType() != ChannelType.FORUM) {
            return List.of();
        }
        ForumChannel forum = (ForumChannel) channel;
        List<ThreadChannel> allThreads = new ArrayList<>(forum.getThreadChannels());
        try {
            allThreads.addAll(forum.retrieveArchivedPublicThreadChannels().complete());
        } catch (Exception ex) {
            log.warn("Could not retrieve archived threads for {} – {}", forum.getName(), ex.getMessage());
        }
        return allThreads.stream()
                .collect(Collectors.toMap(
                        ISnowflake::getId,
                        Function.identity(),
                        (a, b) -> a))
                .values()
                .stream()
                .filter(t -> t.canTalk() && (returnAll || t.getName().toLowerCase().contains("blood")))
                .map(t -> DiscordThread.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .channelType(t.getType())
                        .build())
                .toList();
    }

    public void sendMessage(DiscordNotification discordNotification){
        String message = discordNotification.message();
        List<String> messagesParts = new ArrayList<>();
        int maxLength = 2000;
        for (int start = 0; start < message.length(); start += maxLength) {
            int end = Math.min(start + maxLength, message.length());
            messagesParts.add(message.substring(start, end));
        }
        for(String messagePart: messagesParts) {
            discordNotification.channelsToNotify().forEach(c -> {
                if (c.channelType() == ChannelType.TEXT) {
                    Objects.requireNonNull(jda.getTextChannelById(c.id())).sendMessage(messagePart).queue();
                } else if (c.channelType() == ChannelType.GUILD_PUBLIC_THREAD) {
                    Objects.requireNonNull(jda.getThreadChannelById(c.id())).sendMessage(messagePart).queue();
                }
            });
        }
    }

//    private void registerCommands(JDA jda){
//        jda.upsertCommand("test", "ebe ebe")
//                .queue();
//    }

//    private void test(SlashCommandInteractionEvent event){
//    }
}
