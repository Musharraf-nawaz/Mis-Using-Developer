package com.aims.dto.asset;

import com.aims.entity.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMediaResponse {
    private Long id;
    private MediaType mediaType;
    private String fileUrl;
    private String fileName;
    private LocalDateTime uploadedAt;
}
