package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.response.AllergenCheckResult;
import com.example.smartfoodordering.dto.response.WarningResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AllergenDetectionService {
    /** All warnings for the given items against the customer's current profile. */
    AllergenCheckResult check(Long customerId, Collection<Long> menuItemIds);

    /** Same check grouped by menu item id; items without warnings are absent from the map. */
    Map<Long, List<WarningResponse>> checkByItem(Long customerId, Collection<Long> menuItemIds);
}
