package com.bookfair.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookfair.backend.dto.pricing.mapper.PricingMapper;
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
    private final PricingMapper pricingMapper;

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getActiveRules() {
        return pricingRuleRepository.findAllByActiveTrue().stream()
                .map(pricingMapper::toPricingRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PricingRuleResponse createPricingRule(PricingRuleRequest request) {
        pricingRuleValidator.validate(request.getConditionType(), request.getConditionValue());

        PricingRule rule = pricingMapper.toPricingRule(request);
        rule.setActive(true);

        PricingRule saved = pricingRuleRepository.save(rule);

        log.info("Created new pricing rule: {}", saved.getName());
        return pricingMapper.toPricingRuleResponse(saved);
    }
}

