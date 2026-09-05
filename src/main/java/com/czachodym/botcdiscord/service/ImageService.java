package com.czachodym.botcdiscord.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService {
    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;

    public List<byte[]> getImages(long gameId) {
        Query query = new Query(Criteria.where("metadata.gameId").is(gameId));
        return gridFsTemplate.find(query)
                .map(file -> {
                    try {
                        return gridFsOperations.getResource(file).getInputStream().readAllBytes();
                    } catch (IOException e) {
                        log.error("Failed to load image {}", file.getFilename(), e);
                        return null;
                    }
                })
                .into(new ArrayList<>());
    }
}
