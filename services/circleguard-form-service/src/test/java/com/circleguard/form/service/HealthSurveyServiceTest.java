package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.repository.HealthSurveyRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthSurveyServiceTest {

    @Mock private HealthSurveyRepository surveyRepository;
    @Mock private QuestionnaireService questionnaireService;
    @Mock private SymptomMapper symptomMapper;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private HealthSurveyService surveyService;

    @BeforeEach
    void setUp() {
        surveyService = new HealthSurveyService(surveyRepository, questionnaireService, symptomMapper, kafkaTemplate, new SimpleMeterRegistry());
    }

    @Test
    void submitSurvey_withSymptoms_emitsSurveySubmittedEvent() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = buildSurvey(anonymousId, null);
        Questionnaire questionnaire = new Questionnaire();

        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.of(questionnaire));
        when(symptomMapper.hasSymptoms(survey, questionnaire)).thenReturn(true);
        when(surveyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        surveyService.submitSurvey(survey);

        verify(kafkaTemplate).send(eq("survey.submitted"), eq(anonymousId.toString()), any());
    }

    @Test
    void submitSurvey_withoutSymptoms_setsHasFeverFalse() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = buildSurvey(anonymousId, null);
        Questionnaire questionnaire = new Questionnaire();

        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.of(questionnaire));
        when(symptomMapper.hasSymptoms(survey, questionnaire)).thenReturn(false);
        when(surveyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        surveyService.submitSurvey(survey);

        assertThat(survey.getHasFever()).isFalse();
    }

    @Test
    void submitSurvey_withAttachment_setsPendingStatus() {
        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = buildSurvey(anonymousId, "/uploads/cert.pdf");

        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());
        when(surveyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        surveyService.submitSurvey(survey);

        assertThat(survey.getValidationStatus()).isEqualTo(ValidationStatus.PENDING);
    }

    @Test
    void validateSurvey_whenApproved_emitsCertificateValidatedEvent() {
        UUID surveyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID anonymousId = UUID.randomUUID();

        HealthSurvey survey = buildSurvey(anonymousId, "/uploads/cert.pdf");
        survey.setId(surveyId);

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        surveyService.validateSurvey(surveyId, ValidationStatus.APPROVED, adminId);

        verify(kafkaTemplate).send(eq("certificate.validated"), eq(anonymousId.toString()), any());
    }

    @Test
    void validateSurvey_whenRejected_doesNotEmitCertificateEvent() {
        UUID surveyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID anonymousId = UUID.randomUUID();

        HealthSurvey survey = buildSurvey(anonymousId, null);
        survey.setId(surveyId);

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        surveyService.validateSurvey(surveyId, ValidationStatus.REJECTED, adminId);

        verify(kafkaTemplate, never()).send(eq("certificate.validated"), any(), any());
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private HealthSurvey buildSurvey(UUID anonymousId, String attachmentPath) {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(anonymousId);
        survey.setAttachmentPath(attachmentPath);
        return survey;
    }
}
