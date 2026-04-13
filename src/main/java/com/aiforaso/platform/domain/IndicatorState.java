package com.aiforaso.platform.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "indicator_state")
public class IndicatorState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "literature_id")
    private Literature literature;

    @Column(nullable = false, length = 120)
    private String indicatorName;

    @Column(nullable = false, length = 40)
    private String stageType;

    @Column(nullable = false)
    private Integer stateOrder;

    @Column(nullable = false, length = 120)
    private String stateLabel;

    @Column(nullable = false, length = 2000)
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String evidenceLocator;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Literature getLiterature() {
        return literature;
    }

    public void setLiterature(Literature literature) {
        this.literature = literature;
    }

    public String getIndicatorName() {
        return indicatorName;
    }

    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    public String getStageType() {
        return stageType;
    }

    public void setStageType(String stageType) {
        this.stageType = stageType;
    }

    public Integer getStateOrder() {
        return stateOrder;
    }

    public void setStateOrder(Integer stateOrder) {
        this.stateOrder = stateOrder;
    }

    public String getStateLabel() {
        return stateLabel;
    }

    public void setStateLabel(String stateLabel) {
        this.stateLabel = stateLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceLocator() {
        return evidenceLocator;
    }

    public void setEvidenceLocator(String evidenceLocator) {
        this.evidenceLocator = evidenceLocator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
