package com.aims.util;

import com.aims.entity.AssetMedia;
import com.aims.entity.enums.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AssetMediaUtils {

    private AssetMediaUtils() {}

    public static Map<Long, List<AssetMedia>> groupByAssetId(List<AssetMedia> mediaList) {
        Map<Long, List<AssetMedia>> grouped = new HashMap<>();
        for (AssetMedia media : mediaList) {
            Long assetId = media.getAsset().getId();
            grouped.computeIfAbsent(assetId, k -> new java.util.ArrayList<>()).add(media);
        }
        return grouped;
    }

    public static String photoUrl(List<AssetMedia> mediaList) {
        return mediaList.stream()
                .filter(m -> m.getMediaType() == MediaType.PHOTO)
                .map(AssetMedia::getFileUrl)
                .findFirst()
                .orElse(null);
    }

    public static String videoUrl(List<AssetMedia> mediaList) {
        return mediaList.stream()
                .filter(m -> m.getMediaType() == MediaType.VIDEO)
                .map(AssetMedia::getFileUrl)
                .findFirst()
                .orElse(null);
    }
}
