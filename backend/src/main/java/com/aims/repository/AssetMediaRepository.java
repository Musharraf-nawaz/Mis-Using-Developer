package com.aims.repository;

import com.aims.entity.AssetMedia;
import com.aims.entity.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssetMediaRepository extends JpaRepository<AssetMedia, Long> {
    List<AssetMedia> findByAssetId(Long assetId);
    List<AssetMedia> findByAssetIdAndMediaType(Long assetId, MediaType mediaType);

    @Query("SELECT m FROM AssetMedia m JOIN FETCH m.asset a WHERE a.id IN :assetIds")
    List<AssetMedia> findByAssetIdIn(@Param("assetIds") Collection<Long> assetIds);
}
