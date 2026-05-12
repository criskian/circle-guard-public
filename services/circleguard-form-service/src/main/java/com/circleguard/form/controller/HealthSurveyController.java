package com.circleguard.form.controller;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.repository.HealthSurveyRepository;
import com.circleguard.form.service.HealthSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/surveys")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HealthSurveyController {
    private final HealthSurveyService surveyService;
    private final HealthSurveyRepository surveyRepository;

    @PostMapping
    public ResponseEntity<HealthSurvey> submit(@RequestBody HealthSurvey survey) {
        return ResponseEntity.ok(surveyService.submitSurvey(survey));
    }

    @GetMapping
    public ResponseEntity<List<HealthSurvey>> listAll() {
        return ResponseEntity.ok(surveyRepository.findAll());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<HealthSurvey>> listPending() {
        return ResponseEntity.ok(surveyService.getPendingSurveys());
    }
}
