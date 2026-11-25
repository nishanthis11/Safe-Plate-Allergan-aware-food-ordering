package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.response.AllergenCheckResult;
import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.repository.CustomerFoodProfileRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.service.AllergenDetectionEngine;
import com.example.smartfoodordering.service.AllergenDetectionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AllergenDetectionServiceImpl implements AllergenDetectionService {
    private final CustomerFoodProfileRepository profileRepository;
    private final MenuItemRepository menuItemRepository;
    private final AllergenDetectionEngine engine;

    public AllergenDetectionServiceImpl(CustomerFoodProfileRepository profileRepository,
                                        MenuItemRepository menuItemRepository, AllergenDetectionEngine engine) {
        this.profileRepository = profileRepository;
        this.menuItemRepository = menuItemRepository;
        this.engine = engine;
    }

    @Override
    public AllergenCheckResult check(Long customerId, Collection<Long> menuItemIds) {
        List<WarningResponse> all = new ArrayList<>();
        checkByItem(customerId, menuItemIds).values().forEach(all::addAll);
        return new AllergenCheckResult(all);
    }

    @Override
    public Map<Long, List<WarningResponse>> checkByItem(Long customerId, Collection<Long> menuItemIds) {
        Map<Long, List<WarningResponse>> result = new LinkedHashMap<>();
        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return result;
        }
        CustomerFoodProfile profile = profileRepository.findDetailedByUserId(customerId)
                .orElseGet(CustomerFoodProfile::new);
        for (MenuItem item : menuItemRepository.findAllWithSafetyDataByIdIn(menuItemIds)) {
            List<WarningResponse> warnings = engine.evaluate(profile, item);
            if (!warnings.isEmpty()) {
                result.put(item.getId(), warnings);
            }
        }
        return result;
    }
}
