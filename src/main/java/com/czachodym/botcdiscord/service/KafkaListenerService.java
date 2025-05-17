package com.czachodym.botcdiscord.service;

import com.czachodym.botcshared.dto.DiscordNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerService {
    private final DiscordService discordService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    @Value("${kafka.topics.channels-response}")
    private String channelsResponseTopic;
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    //im not an idiot (at least i think i'm not), as i don't work with kafka a lot i try different approaches.
    @KafkaListener(topics = "${kafka.topics.notification}", groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "discordNotificationKafkaListenerContainerFactory")
    public void listen(DiscordNotification discordNotification) {
        log.info("Received discord message to send.");
        discordService.sendMessage(discordNotification);
        log.info("Message sent.");
    }

    @KafkaListener(topics = "${kafka.topics.channels}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenForGetChannelsMessage(String message) {
        log.info("Received discord server list request.");
        JSONObject json = new JSONObject(message);
        String correlationId = json.getString("correlationId");

        JSONObject response = new JSONObject();
        response.put("correlationId", correlationId);
        try {
            response.put("payload", new ObjectMapper().writeValueAsString(discordService.getDiscordGuilds()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        kafkaTemplate.send(channelsResponseTopic, response.toString());
        log.info("Request completed, message sent.");
    }
}
