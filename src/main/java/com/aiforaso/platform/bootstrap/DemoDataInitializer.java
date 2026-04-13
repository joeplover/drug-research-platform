package com.aiforaso.platform.bootstrap;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.aiforaso.platform.dto.LiteratureCreateRequest;
import com.aiforaso.platform.dto.UserCreateRequest;
import com.aiforaso.platform.dto.LiteratureView;
import com.aiforaso.platform.service.KnowledgeExtractionService;
import com.aiforaso.platform.service.LiteratureIngestionService;
import com.aiforaso.platform.service.LiteratureService;
import com.aiforaso.platform.service.UserService;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final LiteratureService literatureService;
    private final UserService userService;
    private final LiteratureIngestionService literatureIngestionService;
    private final KnowledgeExtractionService knowledgeExtractionService;

    public DemoDataInitializer(
            LiteratureService literatureService,
            UserService userService,
            LiteratureIngestionService literatureIngestionService,
            KnowledgeExtractionService knowledgeExtractionService) {
        this.literatureService = literatureService;
        this.userService = userService;
        this.literatureIngestionService = literatureIngestionService;
        this.knowledgeExtractionService = knowledgeExtractionService;
    }

    @Override
    public void run(String... args) {
        userService.ensureSeedUser("research_admin", "admin@aiforaso.local", "123456", "ADMIN");
        userService.ensureSeedUser("clinical_reviewer", "reviewer@aiforaso.local", "123456", "RESEARCHER");

        if (!literatureService.list(null).isEmpty()) {
            return;
        }

        LiteratureView first = literatureService.create(new LiteratureCreateRequest(
                "Efficacy and Safety of Bepirovirsen in Chronic Hepatitis B Infection",
                "PDF",
                "viral hepatitis",
                "Phase study discussing HBsAg reduction, HBV DNA dynamics, and treatment response patterns in ASO-supported chronic hepatitis B research.",
                "HBsAg,HBV DNA,ASO,hepatitis B",
                LocalDate.of(2024, 1, 10),
                "数据示例/Efficacy and Safety of Bepirovirsen in Chronic Hepatitis B Infection.pdf"));

        LiteratureView second = literatureService.create(new LiteratureCreateRequest(
                "Long-term open-label vebicorvir for chronic HBV infection",
                "PDF",
                "viral hepatitis",
                "Long-term safety follow-up highlighting ALT, AST, virologic rebound, and off-treatment response in nucleotide or oligonucleotide combination research.",
                "ALT,AST,HBV,off-treatment response",
                LocalDate.of(2024, 6, 25),
                "数据示例/Long-term open-label vebicorvir for chronic HBV infection Safety and off-treatment responses.pdf"));

        LiteratureView third = literatureService.create(new LiteratureCreateRequest(
                "Safety, pharmacodynamics, and antiviral activity of selgantolimod",
                "PDF",
                "infectious disease",
                "Immunology-oriented evidence covering antiviral activity, pharmacodynamic changes, HBeAg, and tolerability in viremic patients.",
                "pharmacodynamics,HBeAg,antiviral activity,safety",
                LocalDate.of(2023, 11, 18),
                "数据示例/Safety, pharmacodynamics, and antiviral activity of selgantolimod in viremic patients with chronic hepatitis B virus infection.pdf"));

        ingestQuietly(first.id());
        ingestQuietly(second.id());
        ingestQuietly(third.id());
    }

    private void ingestQuietly(Long literatureId) {
        try {
            literatureIngestionService.ingest(literatureId);
            literatureIngestionService.vectorize(literatureId);
            knowledgeExtractionService.extractIndicatorsByChunks(literatureId);
        } catch (Exception ignored) {
        }
    }
}
