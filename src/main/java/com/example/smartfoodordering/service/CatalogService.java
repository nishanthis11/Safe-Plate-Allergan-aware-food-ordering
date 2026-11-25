package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.response.AllergenResponse;
import com.example.smartfoodordering.dto.response.IngredientResponse;
import com.example.smartfoodordering.repository.AllergenRepository;
import com.example.smartfoodordering.repository.IngredientRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only lookup lists used by forms. */
@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final AllergenRepository allergenRepository;
    private final IngredientRepository ingredientRepository;

    public CatalogService(AllergenRepository allergenRepository, IngredientRepository ingredientRepository) {
        this.allergenRepository = allergenRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public List<AllergenResponse> allergens() {
        return allergenRepository.findAll().stream()
                .map(a -> new AllergenResponse(a.getId(), a.getCode(), a.getName()))
                .sorted(Comparator.comparing(AllergenResponse::name))
                .toList();
    }

    public List<IngredientResponse> ingredients() {
        return ingredientRepository.findAll().stream()
                .map(i -> new IngredientResponse(i.getId(), i.getName()))
                .sorted(Comparator.comparing(IngredientResponse::name))
                .toList();
    }
}
