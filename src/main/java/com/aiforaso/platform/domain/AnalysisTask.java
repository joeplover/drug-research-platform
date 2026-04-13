package com.aiforaso.platform.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "analysis_task")
public class AnalysisTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String taskType;

    @Column(nullable = false, length = 60)
    private String status;

    @Column(nullable = false, length = 1500)
    private String inputText;

    @Column(nullable = false, length = 4000)
    private String resultSummary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String citations;

    @Column(length = 1000)
    private String contextLiteratureIds;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String evidenceJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String indicatorsJson;

    @Column(length = 500)
    private String analysisFocus;

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

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getCitations() {
        return citations;
    }

    public void setCitations(String citations) {
        this.citations = citations;
    }

    public String getContextLiteratureIds() {
        return contextLiteratureIds;
    }

    public void setContextLiteratureIds(String contextLiteratureIds) {
        this.contextLiteratureIds = contextLiteratureIds;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getIndicatorsJson() {
        return indicatorsJson;
    }

    public void setIndicatorsJson(String indicatorsJson) {
        this.indicatorsJson = indicatorsJson;
    }

    public String getAnalysisFocus() {
        return analysisFocus;
    }

    public void setAnalysisFocus(String analysisFocus) {
        this.analysisFocus = analysisFocus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
