package com.aiforaso.platform.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "literature")
public class Literature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 64)
    private String sourceType;

    @Column(nullable = false, length = 120)
    private String diseaseArea;

    @Column(nullable = false, length = 4000)
    private String summary;

    @Column(length = 1000)
    private String keywords;

    private LocalDate publicationDate;

    @Column(length = 300)
    private String storagePath;

    @Column(length = 40)
    private String vectorSyncStatus;

    @Column(length = 500)
    private String vectorSyncDetail;

    private Integer vectorSyncedChunkCount;

    private LocalDateTime vectorSyncedAt;

    private LocalDateTime createdAt;

    private Long createdBy;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDiseaseArea() {
        return diseaseArea;
    }

    public void setDiseaseArea(String diseaseArea) {
        this.diseaseArea = diseaseArea;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getVectorSyncStatus() {
        return vectorSyncStatus;
    }

    public void setVectorSyncStatus(String vectorSyncStatus) {
        this.vectorSyncStatus = vectorSyncStatus;
    }

    public String getVectorSyncDetail() {
        return vectorSyncDetail;
    }

    public void setVectorSyncDetail(String vectorSyncDetail) {
        this.vectorSyncDetail = vectorSyncDetail;
    }

    public Integer getVectorSyncedChunkCount() {
        return vectorSyncedChunkCount;
    }

    public void setVectorSyncedChunkCount(Integer vectorSyncedChunkCount) {
        this.vectorSyncedChunkCount = vectorSyncedChunkCount;
    }

    public LocalDateTime getVectorSyncedAt() {
        return vectorSyncedAt;
    }

    public void setVectorSyncedAt(LocalDateTime vectorSyncedAt) {
        this.vectorSyncedAt = vectorSyncedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
