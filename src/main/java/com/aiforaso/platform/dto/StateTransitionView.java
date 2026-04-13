package com.aiforaso.platform.dto;

import java.math.BigDecimal;

public record StateTransitionView(
        Long id,
        Long literatureId,
        Long fromStateId,
        Long toStateId,
        String conditionText,
        BigDecimal transitionProbability,
        String evidenceLocator) {
}
