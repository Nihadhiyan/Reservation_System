package com.bookfair.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.pricing.request.PricingRuleRequest;
import com.bookfair.backend.dto.pricing.response.PricingRuleResponse;
import com.bookfair.backend.model.PricingRule;
import com.bookfair.backend.repository.PricingRuleRepository;
import com.bookfair.backend.service.validator.PricingRuleValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final PricingRuleValidator pricingRuleValidator;

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getActiveRules() {
        return pricingRuleRepository.findAllByActiveTrue().stream().map(rule -> {
            PricingRuleResponse response = new PricingRuleResponse();
            response.setId(rule.getId());
            response.setName(rule.getName());
            response.setDescription(rule.getDescription());
            response.setConditionType(rule.getConditionType());
            response.setConditionValue(rule.getConditionValue());
            response.setMultiplier(rule.getMultiplier());
            response.setActive(rule.getActive());
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public PricingRuleResponse createPricingRule(PricingRuleRequest request) {
        pricingRuleValidator.validate(request.getConditionType(), request.getConditionValue());

        PricingRule rule = new PricingRule();
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setConditionType(request.getConditionType());
        rule.setConditionValue(request.getConditionValue());
        rule.setMultiplier(request.getMultiplier());
        rule.setActive(true);

        PricingRule saved = pricingRuleRepository.save(rule);

        PricingRuleResponse response = new PricingRuleResponse();
        response.setId(saved.getId());
        response.setName(saved.getName());
        response.setDescription(saved.getDescription());
        response.setConditionType(saved.getConditionType());
        response.setConditionValue(saved.getConditionValue());
        response.setMultiplier(saved.getMultiplier());
        response.setActive(saved.getActive());

        log.info("Created new pricing rule: {}", saved.getName());
        return response;
    }
}
