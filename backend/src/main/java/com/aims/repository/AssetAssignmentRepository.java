package com.aims.repository;

import com.aims.entity.AssetAssignment;
import com.aims.entity.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {

    List<AssetAssignment> findByAssetIdOrderByCreatedAtDesc(Long assetId);

    Optional<AssetAssignment> findByAssetIdAndStatus(Long assetId, AssignmentStatus status);

    @Query("SELECT aa FROM AssetAssignment aa JOIN FETCH aa.asset JOIN FETCH aa.employee " +
           "WHERE aa.status = :status ORDER BY aa.assignedDate DESC")
    List<AssetAssignment> findRecentByStatus(@Param("status") AssignmentStatus status, Pageable pageable);

    @Query("SELECT aa FROM AssetAssignment aa WHERE aa.status = 'ACTIVE' " +
           "AND aa.expectedReturnDate < :today")
    List<AssetAssignment> findOverdueAssignments(@Param("today") LocalDate today);

    Page<AssetAssignment> findByEmployeeId(Long employeeId, Pageable pageable);
}
