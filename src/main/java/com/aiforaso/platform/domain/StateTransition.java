package com.aiforaso.platform.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "state_transition")
public class StateTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "literature_id")
    private Literature literature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_state_id")
    private IndicatorState fromState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_state_id")
    private IndicatorState toState;

    @Column(nullable = false, length = 2000)
    private String conditionText;

    @Column(precision = 5, scale = 2)
    private BigDecimal transitionProbability;

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

    public IndicatorState getFromState() {
        return fromState;
    }

    public void setFromState(IndicatorState fromState) {
        this.fromState = fromState;
    }

    public IndicatorState getToState() {
        return toState;
    }

    public void setToState(IndicatorState toState) {
        this.toState = toState;
    }

    public String getConditionText() {
        return conditionText;
    }

    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }

    public BigDecimal getTransitionProbability() {
        return transitionProbability;
    }

    public void setTransitionProbability(BigDecimal transitionProbability) {
        this.transitionProbability = transitionProbability;
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
