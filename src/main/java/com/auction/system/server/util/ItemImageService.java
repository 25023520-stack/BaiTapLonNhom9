package com.auction.system.server.util;

import com.auction.system.model.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

public class ItemImageService {

    private ItemImageService() {}

    public static void attachImageData(Item item) {
        if (item == null || item.getImagePath() == null || item.getImagePath().isBlank()) {
            return;
        }
        try {
            Path imagePath = Path.of(item.getImagePath());
            if (Files.exists(imagePath)) {
                item.setImageBase64(Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath)));
            }
        } catch (IOException ignored) {
            item.setImageBase64(null);
        }
    }

    public static List<Item> withImageData(List<Item> items) {
        items.forEach(ItemImageService::attachImageData);
        return items;
    }
}
