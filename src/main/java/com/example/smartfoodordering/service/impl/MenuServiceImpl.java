package com.example.smartfoodordering.service.impl;

import com.example.smartfoodordering.dto.request.MenuItemRequest;
import com.example.smartfoodordering.dto.request.RestaurantRequest;
import com.example.smartfoodordering.dto.response.MenuItemResponse;
import com.example.smartfoodordering.dto.response.MenuItemView;
import com.example.smartfoodordering.dto.response.RestaurantResponse;
import com.example.smartfoodordering.dto.response.WarningResponse;
import com.example.smartfoodordering.entity.Allergen;
import com.example.smartfoodordering.entity.Ingredient;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.MenuItemAllergen;
import com.example.smartfoodordering.entity.MenuItemIngredient;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.enums.AllergenSource;
import com.example.smartfoodordering.enums.AuditAction;
import com.example.smartfoodordering.exception.AccessDeniedAppException;
import com.example.smartfoodordering.exception.BusinessRuleException;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.mapper.MenuMapper;
import com.example.smartfoodordering.repository.AllergenRepository;
import com.example.smartfoodordering.repository.IngredientRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.repository.RestaurantRepository;
import com.example.smartfoodordering.security.CustomUserDetails;
import com.example.smartfoodordering.service.AllergenDetectionService;
import com.example.smartfoodordering.service.AuditService;
import com.example.smartfoodordering.service.MenuService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuServiceImpl implements MenuService {
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final IngredientRepository ingredientRepository;
    private final AllergenRepository allergenRepository;
    private final AllergenDetectionService detectionService;
    private final AuditService auditService;
    private final MenuMapper menuMapper;

    public MenuServiceImpl(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository,
                           IngredientRepository ingredientRepository, AllergenRepository allergenRepository,
                           AllergenDetectionService detectionService, AuditService auditService,
                           MenuMapper menuMapper) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.ingredientRepository = ingredientRepository;
        this.allergenRepository = allergenRepository;
        this.detectionService = detectionService;
        this.auditService = auditService;
        this.menuMapper = menuMapper;
    }

    // -------------------------------------------------------- public / customer

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> listActiveRestaurants() {
        return restaurantRepository.findByActiveTrueOrderByName().stream().map(menuMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getActiveRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId).filter(Restaurant::isActive)
                .map(menuMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemView> getMenuForCustomer(Long restaurantId, Long customerId) {
        List<MenuItem> items = menuItemRepository.findByRestaurantIdAndAvailableTrueOrderByName(restaurantId);
        Map<Long, List<WarningResponse>> warnings = customerId == null ? Collections.emptyMap()
                : detectionService.checkByItem(customerId, items.stream().map(MenuItem::getId).toList());
        return items.stream()
                .map(i -> new MenuItemView(menuMapper.toResponse(i), warnings.getOrDefault(i.getId(), List.of())))
                .toList();
    }

    // -------------------------------------------------------------- manager

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuForManager(CustomUserDetails actor) {
        return menuItemRepository.findByRestaurantIdOrderByName(requireRestaurant(actor)).stream()
                .map(menuMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemRequest getFormForEdit(CustomUserDetails actor, Long itemId) {
        MenuItem item = loadOwned(actor, itemId);
        MenuItemRequest form = new MenuItemRequest();
        form.setName(item.getName());
        form.setDescription(item.getDescription());
        form.setPrice(item.getPrice());
        form.setCuisine(item.getCuisine());
        form.setSpiceLevel(item.getSpiceLevel());
        form.setAvailable(item.isAvailable());
        form.setAllergenInfoComplete(item.isAllergenInfoComplete());
        form.setDietTags(new ArrayList<>(item.getDietTags()));
        form.setIngredientIds(item.getIngredients().stream().map(mi -> mi.getIngredient().getId())
                .collect(Collectors.toCollection(ArrayList::new)));
        form.setDeclaredAllergenIds(item.getAllergens().stream()
                .filter(a -> a.getSource() == AllergenSource.DECLARED).map(a -> a.getAllergen().getId())
                .collect(Collectors.toCollection(ArrayList::new)));
        form.setMayContainAllergenIds(item.getAllergens().stream()
                .filter(a -> a.getSource() == AllergenSource.MAY_CONTAIN).map(a -> a.getAllergen().getId())
                .collect(Collectors.toCollection(ArrayList::new)));
        return form;
    }

    @Override
    public Long save(CustomUserDetails actor, Long itemId, MenuItemRequest form) {
        Long restaurantId = requireRestaurant(actor);
        MenuItem item;
        if (itemId == null) {
            item = new MenuItem();
            item.setRestaurant(restaurantRepository.getReferenceById(restaurantId));
        } else {
            item = loadOwned(actor, itemId);
        }
        item.setName(form.getName().trim());
        item.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        item.setPrice(form.getPrice());
        item.setCuisine(form.getCuisine().trim());
        item.setSpiceLevel(form.getSpiceLevel());
        item.setAvailable(form.isAvailable());
        item.setAllergenInfoComplete(form.isAllergenInfoComplete());
        item.getDietTags().clear();
        if (form.getDietTags() != null) {
            item.getDietTags().addAll(form.getDietTags());
        }
        syncIngredients(item, form.getIngredientIds());
        syncAllergens(item, form.getDeclaredAllergenIds(), form.getMayContainAllergenIds());
        MenuItem saved = menuItemRepository.save(item);
        auditService.log(actor.getUsername(), AuditAction.MENU_CHANGED, "MenuItem", saved.getId(),
                (itemId == null ? "Created: " : "Updated: ") + saved.getName());
        return saved.getId();
    }

    @Override
    public void setAvailability(CustomUserDetails actor, Long itemId, boolean available) {
        MenuItem item = loadOwned(actor, itemId);
        item.setAvailable(available);
        menuItemRepository.save(item);
        auditService.log(actor.getUsername(), AuditAction.MENU_CHANGED, "MenuItem", itemId,
                (available ? "Made available: " : "Made unavailable: ") + item.getName());
    }

    // ---------------------------------------------------------------- admin

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> listAllRestaurants() {
        return restaurantRepository.findAll().stream().map(menuMapper::toResponse)
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name())).toList();
    }

    @Override
    public RestaurantResponse createRestaurant(RestaurantRequest request, String actorEmail) {
        Restaurant r = new Restaurant();
        r.setName(request.getName().trim());
        r.setCuisine(request.getCuisine().trim());
        r.setDescription(request.getDescription());
        r.setAddress(request.getAddress());
        r.setDeliveryFee(request.getDeliveryFee());
        Restaurant saved = restaurantRepository.save(r);
        auditService.log(actorEmail, AuditAction.MENU_CHANGED, "Restaurant", saved.getId(), "Restaurant created");
        return menuMapper.toResponse(saved);
    }

    @Override
    public void setRestaurantActive(Long restaurantId, boolean active, String actorEmail) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        r.setActive(active);
        restaurantRepository.save(r);
        auditService.log(actorEmail, AuditAction.MENU_CHANGED, "Restaurant", restaurantId,
                active ? "Restaurant activated" : "Restaurant deactivated");
    }

    // -------------------------------------------------------------- helpers

    private Long requireRestaurant(CustomUserDetails actor) {
        if (actor.getRestaurantId() == null) {
            throw new BusinessRuleException("Your account is not linked to a restaurant");
        }
        return actor.getRestaurantId();
    }

    private MenuItem loadOwned(CustomUserDetails actor, Long itemId) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        if (!requireRestaurant(actor).equals(item.getRestaurant().getId())) {
            auditService.log(actor.getUsername(), AuditAction.ACCESS_DENIED, "MenuItem", itemId,
                    "Attempted access to another restaurant's menu item");
            throw new AccessDeniedAppException("You cannot modify this menu item");
        }
        return item;
    }

    /** Diff instead of clear-and-reinsert, so the unique (item, ingredient) constraint is never violated. */
    private void syncIngredients(MenuItem item, List<Long> ids) {
        Set<Long> wanted = ids == null ? new HashSet<>() : new HashSet<>(ids);
        item.getIngredients().removeIf(mi -> !wanted.contains(mi.getIngredient().getId()));
        Set<Long> have = item.getIngredients().stream().map(mi -> mi.getIngredient().getId())
                .collect(Collectors.toSet());
        for (Long id : wanted) {
            if (!have.contains(id)) {
                Ingredient ingredient = ingredientRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + id));
                item.getIngredients().add(new MenuItemIngredient(item, ingredient));
            }
        }
    }

    private void syncAllergens(MenuItem item, List<Long> declaredIds, List<Long> mayContainIds) {
        Map<Long, AllergenSource> wanted = new HashMap<>();
        if (mayContainIds != null) {
            mayContainIds.forEach(id -> wanted.put(id, AllergenSource.MAY_CONTAIN));
        }
        if (declaredIds != null) {
            declaredIds.forEach(id -> wanted.put(id, AllergenSource.DECLARED)); // declared wins over may-contain
        }
        Set<Long> have = new HashSet<>();
        Iterator<MenuItemAllergen> it = item.getAllergens().iterator();
        while (it.hasNext()) {
            MenuItemAllergen existing = it.next();
            Long allergenId = existing.getAllergen().getId();
            AllergenSource source = wanted.get(allergenId);
            if (source == null) {
                it.remove();
            } else {
                existing.setSource(source);
                have.add(allergenId);
            }
        }
        for (Map.Entry<Long, AllergenSource> entry : wanted.entrySet()) {
            if (!have.contains(entry.getKey())) {
                Allergen allergen = allergenRepository.findById(entry.getKey())
                        .orElseThrow(() -> new ResourceNotFoundException("Allergen not found: " + entry.getKey()));
                item.getAllergens().add(new MenuItemAllergen(item, allergen, entry.getValue()));
            }
        }
    }
}
