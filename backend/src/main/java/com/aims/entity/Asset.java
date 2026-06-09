package com.aims.entity;

import com.aims.entity.enums.AssetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "asset_name", nullable = false, length = 200)
    private String assetName;

    @Column(name = "associated_developer", length = 150)
    private String associatedDeveloper;

    @Column(name = "project_name", length = 150)
    private String projectName;

    @Column(name = "project_offboarded", nullable = false)
    @Builder.Default
    private Boolean projectOffboarded = false;

    @Column(name = "asset_category", nullable = false, length = 100)
    private String assetCategory;

    @Column(name = "asset_type", nullable = false, length = 100)
    private String assetType;

    @Column(name = "serial_number", unique = true, length = 100)
    private String serialNumber;

    @Column(name = "asset_tag", unique = true, length = 100)
    private String assetTag;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssetStatus status = AssetStatus.AVAILABLE;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Column(name = "vendor_name", length = 150)
    private String vendorName;

    @Column(length = 50)
    private String condition;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
