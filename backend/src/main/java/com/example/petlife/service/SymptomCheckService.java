package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.symptom.SymptomCheckForm;
import com.example.petlife.entity.SymptomCheckEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.PetMapper;
import com.example.petlife.mapper.SymptomCheckMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SymptomCheckService {

    private final SymptomCheckMapper symptomCheckMapper;
    private final PetMapper petMapper;
    private final PlanAccessService planAccessService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4.1-mini}")
    private String openaiModel;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    public SymptomCheckService(SymptomCheckMapper symptomCheckMapper,
                               PetMapper petMapper,
                               PlanAccessService planAccessService) {
        this.symptomCheckMapper = symptomCheckMapper;
        this.petMapper = petMapper;
        this.planAccessService = planAccessService;
    }

    public SymptomCheckEntity runCheck(Long petId, SymptomCheckForm form, LoginUser currentUser) {
        if (!planAccessService.canUseAiSymptom(currentUser)) {
            throw new BadRequestException("AI症状チェックはスタンダード以上で利用できます");
        }
        if (petMapper.findByIdAndOwnerUserId(petId, currentUser.id()) == null && !currentUser.canManagePets()) {
            throw new BadRequestException("対象ペットが見つかりません");
        }

        AiResult ai = callAiOrFallback(form);

        SymptomCheckEntity row = new SymptomCheckEntity(
                null,
                petId,
                currentUser.id(),
                form.getSymptomType(),
                form.getOnsetText(),
                form.getMemo(),
                ai.severity,
                ai.recommendation,
                ai.model,
                LocalDateTime.now()
        );
        symptomCheckMapper.insertReturningId(row);

        return new SymptomCheckEntity(
                null, petId, currentUser.id(), form.getSymptomType(), form.getOnsetText(), form.getMemo(),
                ai.severity, ai.recommendation, ai.model, LocalDateTime.now()
        );
    }

    public List<SymptomCheckEntity> recentByPet(Long petId, LoginUser currentUser) {
        if (petMapper.findByIdAndOwnerUserId(petId, currentUser.id()) == null && !currentUser.canManagePets()) {
            return List.of();
        }
        List<SymptomCheckEntity> desc = symptomCheckMapper.findRecentByPetId(petId, 5);
        List<SymptomCheckEntity> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc;
    }

    private AiResult callAiOrFallback(SymptomCheckForm form) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            return heuristic(form, "fallback-local");
        }

        try {
            String prompt = "症状チェックをJSONで返してください。" +
                    "フォーマット: {\"severity\":\"LOW|MEDIUM|HIGH\",\"recommendation\":\"OBSERVE|CONSULT|VISIT\"}. " +
                    "症状=" + nullToEmpty(form.getSymptomType()) +
                    ", 発症時期=" + nullToEmpty(form.getOnsetText()) +
                    ", メモ=" + nullToEmpty(form.getMemo());

            Map<String, Object> body = new HashMap<>();
            body.put("model", openaiModel);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", "あなたは獣医相談のトリアージ補助です。必ずJSONのみ返答。"),
                    Map.of("role", "user", "content", prompt)
            ));
            body.put("temperature", 0.2);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            String res = restTemplate.postForObject(
                    openaiBaseUrl + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    String.class
            );

            String severity = extractToken(res, "HIGH", "MEDIUM", "LOW");
            String recommendation = extractToken(res, "VISIT", "CONSULT", "OBSERVE");
            return new AiResult(severity, recommendation, openaiModel);
        } catch (Exception e) {
            return heuristic(form, "fallback-local");
        }
    }

    private AiResult heuristic(SymptomCheckForm form, String model) {
        String text = (nullToEmpty(form.getSymptomType()) + " " + nullToEmpty(form.getMemo())).toLowerCase();
        if (text.contains("血") || text.contains("呼吸") || text.contains("痙攣") || text.contains("ぐったり")) {
            return new AiResult("HIGH", "VISIT", model);
        }
        if (text.contains("吐") || text.contains("下痢") || text.contains("食欲") || text.contains("咳")) {
            return new AiResult("MEDIUM", "CONSULT", model);
        }
        return new AiResult("LOW", "OBSERVE", model);
    }

    private String extractToken(String text, String high, String medium, String low) {
        String upper = text == null ? "" : text.toUpperCase();
        if (upper.contains(high)) return high;
        if (upper.contains(medium)) return medium;
        return low;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static class AiResult {
        final String severity;
        final String recommendation;
        final String model;

        AiResult(String severity, String recommendation, String model) {
            this.severity = severity;
            this.recommendation = recommendation;
            this.model = model;
        }
    }
}
