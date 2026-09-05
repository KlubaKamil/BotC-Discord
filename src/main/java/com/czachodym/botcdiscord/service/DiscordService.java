package com.czachodym.botcdiscord.service;

import com.czachodym.botcshared.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordService extends ListenerAdapter {
    private final JDA jda;
    private final ImageService imageService;

//    @Override
//    public void onSlashCommandInteraction(SlashCommandInteractionEvent event){
//        switch (event.getName()) {
//            case "test" -> test(event);
//        }
//    }

    public DiscordRootDto getDiscordGuildDtoList(){
        return DiscordRootDto.builder()
            .servers(jda.getGuilds().stream()
                .map(g -> DiscordGuildDto.builder()
                    .discordGuildId(g.getId())
                    .name(g.getName())
                    .channels(getDiscordChannelDtoList(g, true))
                    .build())
                .toList()
            ).build();
    }

    private List<DiscordChannelDto> getDiscordChannelDtoList(Guild guild, boolean returnAll){
        Role publicRole = guild.getPublicRole();
        return guild.getChannels().stream()
                .filter(c -> publicRole.getPermissions(c).contains(Permission.MESSAGE_SEND) &&
                        publicRole.getPermissions(c).contains(Permission.VIEW_CHANNEL) &&
                        (c.getType() == ChannelType.TEXT || c.getType() == ChannelType.FORUM))
                .map(c -> DiscordChannelDto.builder()
                        .discordChannelId(c.getId())
                        .name(c.getName())
                        .channelType(c.getType())
                        .threads(getDiscordThreadDtoList(c, publicRole))
                        .allowed(true)
                        .build())
                .filter(dc -> dc.channelType() != ChannelType.FORUM || !dc.threads().isEmpty())
                .toList();
    }

    private List<DiscordThreadDto> getDiscordThreadDtoList(GuildChannel channel, Role publicRole){
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
                .filter(t -> {
                    if (t.isLocked()) {
                        return false;
                    }
                    PermissionOverride threadOverride = t.getPermissionContainer().getPermissionOverride(publicRole);
                    if (threadOverride != null && (threadOverride.getDenied().contains(Permission.VIEW_CHANNEL) ||
                            threadOverride.getDenied().contains(Permission.MESSAGE_SEND_IN_THREADS))) {
                        return false;
                    }
                    return publicRole.getPermissions(t).contains(Permission.VIEW_CHANNEL)
                            && publicRole.getPermissions(t).contains(Permission.MESSAGE_SEND_IN_THREADS);
                })
                .map(t -> DiscordThreadDto.builder()
                        .discordThreadId(t.getId())
                        .name(t.getName())
                        .allowed(true)
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

        discordNotification.channelsToNotify().forEach(id -> {
            MessageChannel channel = jda.getTextChannelById(id);
            if (channel == null){
                channel = jda.getThreadChannelById(id);
            }
            if (channel == null) {
                return;
            }
            if(discordNotification.notificationType() == NotificationType.GAME){
                List<byte[]> resource = imageService.getImages(discordNotification.id());
                List<FileUpload> resources = resource.stream()
                        .map(f -> FileUpload.fromData(f, id + ".jpg"))
                        .toList();
                if(!resources.isEmpty()) {
                    channel.sendFiles(resources).queue();
                }
            }
            for(String messagePart: messagesParts) {
                channel.sendMessage(messagePart).queue();
            }
        });
    }

//    private void registerCommands(JDA jda){
//        jda.upsertCommand("test", "ebe ebe")
//                .queue();
//    }

//    private void test(SlashCommandInteractionEvent event){
//    }
}
