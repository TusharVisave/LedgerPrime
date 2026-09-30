package com.expensetracker.controller;

import com.expensetracker.dto.AiReportResponse;
import com.expensetracker.dto.AnomalyDto;
import com.expensetracker.dto.InsightsResponse;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.AiReportService;
import com.expensetracker.service.AnomalyService;
import com.expensetracker.service.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;
    private final AnomalyService anomalyService;
    private final AiReportService aiReportService;

    @GetMapping
    public InsightsResponse get(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestParam(required = false) String month) {
        return insightService.insights(principal.getUserId(), month);
    }

    /** On-demand anomaly check: last-7-days spend vs 8-week average per category. */
    @GetMapping("/anomalies")
    public Map<String, List<AnomalyDto>> anomalies(
            @AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("anomalies", anomalyService.detect(principal.getUserId()));
    }

    /**
     * AI monthly report via a configured OpenAI-compatible chat API (BYOK).
     * Returns 501 when no API key is configured.
     */
    @PostMapping("/ai-report")
    public AiReportResponse aiReport(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestBody(required = false) Map<String, String> body) {
        String month = body == null ? null : body.get("month");
        return aiReportService.generate(principal.getUserId(), month);
    }
}
