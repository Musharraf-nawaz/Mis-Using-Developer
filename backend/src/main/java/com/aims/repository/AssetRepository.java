package com.aims.repository;

import com.aims.entity.Asset;
import com.aims.entity.enums.AssetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsBySerialNumber(String serialNumber);

    boolean existsByAssetTag(String assetTag);

    @Query("SELECT a FROM Asset a LEFT JOIN FETCH a.assignedTo WHERE " +
           "(:search IS NULL OR LOWER(a.assetName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.serialNumber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:company IS NULL OR a.companyName = :company) " +
           "AND (:assetType IS NULL OR a.assetType = :assetType) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:assignedToId IS NULL OR a.assignedTo.id = :assignedToId)")
    Page<Asset> findWithFilters(@Param("search") String search,
                                @Param("company") String company,
                                @Param("assetType") String assetType,
                                @Param("status") AssetStatus status,
                                @Param("assignedToId") Long assignedToId,
                                Pageable pageable);

    List<Asset> findByAssignedToId(Long userId);

    long countByStatus(AssetStatus status);

    @Query("SELECT a.status as status, COUNT(a) as count FROM Asset a GROUP BY a.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT FUNCTION('TO_CHAR', a.assignedDate, 'YYYY-MM') as month, COUNT(a) as count " +
           "FROM Asset a WHERE a.assignedDate IS NOT NULL " +
           "AND a.assignedDate >= :fromDate GROUP BY FUNCTION('TO_CHAR', a.assignedDate, 'YYYY-MM') " +
           "ORDER BY month")
    List<Object[]> countAssignmentsByMonth(@Param("fromDate") java.time.LocalDate fromDate);
}
