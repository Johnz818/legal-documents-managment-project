package com.example.legal.legalcase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseQueryService caseQueryService;

    public CaseController(CaseQueryService caseQueryService) {
        this.caseQueryService = caseQueryService;
    }

    @GetMapping
    public CaseListResponse getCases() {
        return caseQueryService.getLatestCases();
    }
}
