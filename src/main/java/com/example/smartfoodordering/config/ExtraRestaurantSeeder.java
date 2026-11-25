package com.example.smartfoodordering.config;

import com.example.smartfoodordering.entity.Allergen;
import com.example.smartfoodordering.entity.Ingredient;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.MenuItemAllergen;
import com.example.smartfoodordering.entity.MenuItemIngredient;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.enums.AllergenSource;
import com.example.smartfoodordering.enums.DietType;
import com.example.smartfoodordering.enums.IngredientCategory;
import com.example.smartfoodordering.enums.SpiceLevel;
import com.example.smartfoodordering.repository.AllergenRepository;
import com.example.smartfoodordering.repository.IngredientRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo data only: adds extra restaurants (with fully labelled menus) so the storefront looks populated.
 * It runs once the application is ready, after {@link DataSeeder}, and adds only restaurants that do not exist
 * yet (matched by name), so it is safe on both a fresh and an existing database.
 * Disable together with the main seeder using app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class ExtraRestaurantSeeder {

    private static final Set<DietType> NONE = Set.of();
    private static final Set<DietType> VEG = Set.of(DietType.VEGETARIAN);
    private static final Set<DietType> VEG_GF = Set.of(DietType.VEGETARIAN, DietType.GLUTEN_FREE);
    private static final Set<DietType> VEGAN_ALL = Set.of(DietType.VEGETARIAN, DietType.VEGAN,
            DietType.GLUTEN_FREE, DietType.LACTOSE_FREE);

    private final AllergenRepository allergenRepository;
    private final IngredientRepository ingredientRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public ExtraRestaurantSeeder(AllergenRepository allergenRepository, IngredientRepository ingredientRepository,
                                 RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.allergenRepository = allergenRepository;
        this.ingredientRepository = ingredientRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (allergenRepository.count() == 0) {
            return; // reference data missing: nothing to attach menus to
        }
        Map<String, Allergen> allergens = new HashMap<>();
        allergenRepository.findAll().forEach(a -> allergens.put(a.getCode(), a));
        Map<String, Ingredient> ingredients = ensureIngredients(allergens);
        Set<String> existing = restaurantRepository.findAll().stream()
                .map(r -> r.getName().toLowerCase()).collect(Collectors.toSet());

        add(existing, "Madurai Meals House", "South Indian", "South Indian",
                "Traditional Tamil meals, tiffin and Chettinad specials cooked fresh every day.",
                "8 Salem Road, Tiruchengode", "25.00", ingredients, allergens, List.of(
                dish("Idli Sambar (3 pcs)", "Steamed rice-lentil idlis with sambar and coconut chutney.", "80.00",
                        SpiceLevel.MILD, true, List.of("Idli batter", "Lentils", "Mixed vegetables", "Coconut", "Mustard seeds"),
                        List.of("MUSTARD"), List.of(), VEGAN_ALL),
                dish("Ghee Podi Dosa", "Crisp dosa brushed with ghee and spicy lentil gunpowder.", "110.00",
                        SpiceLevel.MEDIUM, true, List.of("Idli batter", "Ghee", "Lentils", "Sesame oil"),
                        List.of("MILK", "SESAME"), List.of(), VEG_GF),
                dish("Veg Meals", "Rice, sambar, rasam, two poriyals, curd and papad.", "160.00",
                        SpiceLevel.MILD, true, List.of("Basmati rice", "Lentils", "Mixed vegetables", "Yogurt", "Mustard seeds"),
                        List.of("MILK", "MUSTARD"), List.of(), VEG_GF),
                dish("Curd Rice", "Cooling yogurt rice tempered with mustard and curry leaves.", "90.00",
                        SpiceLevel.NONE, true, List.of("Basmati rice", "Yogurt", "Mustard seeds"),
                        List.of("MILK", "MUSTARD"), List.of(), VEG_GF),
                dish("Chettinad Chicken Curry", "Pepper-forward chicken curry in a roasted coconut masala.", "260.00",
                        SpiceLevel.HOT, true, List.of("Chicken", "Onion", "Tomato", "Coconut", "Mustard seeds"),
                        List.of("MUSTARD"), List.of(), NONE),
                dish("Prawn Thokku", "Prawns slow-cooked in tamarind and garlic masala.", "340.00",
                        SpiceLevel.HOT, true, List.of("Prawn", "Onion", "Garlic", "Tamarind sauce"),
                        List.of("SHELLFISH"), List.of(), NONE)));

        add(existing, "Dragon Wok", "Chinese", "Chinese",
                "Indo-Chinese favourites from a roaring wok: noodles, fried rice and crispy starters.",
                "21 Bus Stand Road, Tiruchengode", "35.00", ingredients, allergens, List.of(
                dish("Veg Hakka Noodles", "Wok-tossed noodles with crunchy vegetables and garlic.", "190.00",
                        SpiceLevel.MEDIUM, true, List.of("Wheat noodles", "Mixed vegetables", "Soy sauce", "Garlic"),
                        List.of("GLUTEN", "SOY"), List.of("EGG"), VEG),
                dish("Chilli Chicken (Dry)", "Crispy chicken tossed with peppers, onion and chilli sauce.", "260.00",
                        SpiceLevel.HOT, true, List.of("Chicken", "Egg", "Wheat flour", "Soy sauce", "Onion", "Garlic"),
                        List.of("EGG", "GLUTEN", "SOY"), List.of(), NONE),
                dish("Veg Manchurian", "Vegetable dumplings in a tangy garlic-soy gravy.", "210.00",
                        SpiceLevel.MEDIUM, true, List.of("Mixed vegetables", "Wheat flour", "Soy sauce", "Garlic"),
                        List.of("GLUTEN", "SOY"), List.of(), VEG),
                dish("Prawn Fried Rice", "Smoky egg fried rice with juicy prawns.", "290.00",
                        SpiceLevel.MEDIUM, true, List.of("Basmati rice", "Prawn", "Egg", "Soy sauce", "Garlic"),
                        List.of("SHELLFISH", "EGG", "SOY", "GLUTEN"), List.of(), NONE),
                dish("Sesame Tofu Stir-fry", "Tofu and vegetables in a toasted sesame glaze.", "230.00",
                        SpiceLevel.MILD, true, List.of("Tofu", "Mixed vegetables", "Sesame oil", "Soy sauce", "Garlic"),
                        List.of("SOY", "SESAME", "GLUTEN"), List.of(), Set.of(DietType.VEGETARIAN, DietType.VEGAN)),
                dish("Garlic Mushroom", "Button mushrooms in a garlic pepper sauce.", "220.00",
                        SpiceLevel.MILD, false, List.of("Mushroom", "Garlic", "Soy sauce"),
                        List.of("SOY"), List.of(), VEG)));

        add(existing, "Tuscan Oven", "Italian", "Italian",
                "Wood-fired pizzas and handmade pasta in a cosy, family-friendly setting.",
                "15 Park Avenue, Tiruchengode", "40.00", ingredients, allergens, List.of(
                dish("Margherita Pizza", "Tomato, mozzarella and basil on a thin wood-fired base.", "320.00",
                        SpiceLevel.NONE, true, List.of("Pizza dough", "Tomato", "Cheese"),
                        List.of("GLUTEN", "MILK"), List.of(), VEG),
                dish("Penne Arrabbiata", "Penne in a spicy garlic and tomato sauce.", "280.00",
                        SpiceLevel.MEDIUM, true, List.of("Pasta", "Tomato", "Garlic"),
                        List.of("GLUTEN"), List.of("EGG"), Set.of(DietType.VEGETARIAN, DietType.VEGAN, DietType.LACTOSE_FREE)),
                dish("Cashew Pesto Pasta", "Basil-cashew pesto with parmesan and fresh pasta.", "340.00",
                        SpiceLevel.NONE, true, List.of("Pasta", "Pesto", "Cashew", "Cheese"),
                        List.of("GLUTEN", "TREE_NUT", "MILK"), List.of(), VEG),
                dish("Cheesy Garlic Bread", "Toasted loaf with garlic butter and melted cheese.", "160.00",
                        SpiceLevel.NONE, true, List.of("Pizza dough", "Garlic", "Butter", "Cheese"),
                        List.of("GLUTEN", "MILK"), List.of(), VEG),
                dish("Chicken Alfredo", "Creamy parmesan sauce with grilled chicken and pasta.", "380.00",
                        SpiceLevel.NONE, true, List.of("Pasta", "Chicken", "Cream", "Cheese"),
                        List.of("GLUTEN", "MILK"), List.of(), NONE),
                dish("Tiramisu", "Coffee-soaked sponge layered with mascarpone cream.", "220.00",
                        SpiceLevel.NONE, true, List.of("Egg", "Cream", "Sugar", "Coffee", "Wheat flour"),
                        List.of("EGG", "MILK", "GLUTEN"), List.of(), VEG)));

        add(existing, "Biryani Junction", "Hyderabadi", "Hyderabadi",
                "Slow-cooked dum biryanis sealed and steamed, served with raita and salan.",
                "6 Rice Mill Road, Tiruchengode", "30.00", ingredients, allergens, List.of(
                dish("Chicken Dum Biryani", "Aromatic basmati layered with marinated chicken.", "290.00",
                        SpiceLevel.HOT, true, List.of("Basmati rice", "Chicken", "Onion", "Yogurt", "Ghee"),
                        List.of("MILK"), List.of(), NONE),
                dish("Mutton Biryani", "Tender mutton cooked on dum with saffron rice.", "360.00",
                        SpiceLevel.HOT, true, List.of("Basmati rice", "Mutton", "Onion", "Yogurt", "Ghee"),
                        List.of("MILK"), List.of(), NONE),
                dish("Veg Dum Biryani", "Seasonal vegetables and basmati sealed and steamed.", "220.00",
                        SpiceLevel.MEDIUM, true, List.of("Basmati rice", "Mixed vegetables", "Onion", "Yogurt", "Ghee"),
                        List.of("MILK"), List.of(), VEG),
                dish("Egg Biryani", "Boiled eggs tucked into spiced basmati rice.", "230.00",
                        SpiceLevel.MEDIUM, true, List.of("Basmati rice", "Egg", "Onion", "Yogurt", "Ghee"),
                        List.of("EGG", "MILK"), List.of(), VEG),
                dish("Mirchi Ka Salan", "Green chillies in a peanut-sesame-tamarind gravy.", "140.00",
                        SpiceLevel.HOT, true, List.of("Peanut sauce", "Sesame oil", "Tamarind sauce", "Onion"),
                        List.of("PEANUT", "SESAME"), List.of(), VEGAN_ALL),
                dish("Burani Raita", "Whisked yogurt with onion and cumin.", "60.00",
                        SpiceLevel.NONE, true, List.of("Yogurt", "Onion"),
                        List.of("MILK"), List.of(), VEG_GF)));

        add(existing, "Burger Barn", "American", "American",
                "Smash burgers, loaded fries and thick shakes made to order.",
                "9 College Road, Tiruchengode", "25.00", ingredients, allergens, List.of(
                dish("Classic Chicken Burger", "Crispy chicken, tomato, cheese and house mayo in a toasted bun.", "210.00",
                        SpiceLevel.MILD, true, List.of("Burger bun", "Chicken", "Mayonnaise", "Tomato", "Cheese"),
                        List.of("GLUTEN", "SESAME", "EGG", "MILK"), List.of(), NONE),
                dish("Crispy Veg Burger", "Potato-vegetable patty with lettuce and mayo.", "170.00",
                        SpiceLevel.MILD, true, List.of("Burger bun", "Potato", "Mixed vegetables", "Mayonnaise"),
                        List.of("GLUTEN", "SESAME", "EGG"), List.of(), VEG),
                dish("Mushroom Swiss Burger", "Sauteed mushrooms and melted cheese on a soft bun.", "230.00",
                        SpiceLevel.MILD, true, List.of("Burger bun", "Mushroom", "Cheese", "Mayonnaise"),
                        List.of("GLUTEN", "SESAME", "MILK", "EGG"), List.of(), VEG),
                dish("Peri Peri Fries", "Skin-on fries dusted with peri peri seasoning.", "120.00",
                        SpiceLevel.MEDIUM, true, List.of("Potato"),
                        List.of(), List.of("GLUTEN"), Set.of(DietType.VEGETARIAN, DietType.VEGAN)),
                dish("Peanut Butter Shake", "Thick milkshake blended with roasted peanut butter.", "180.00",
                        SpiceLevel.NONE, true, List.of("Milk", "Peanut butter", "Sugar"),
                        List.of("MILK", "PEANUT"), List.of(), VEG),
                dish("Garden Salad Bowl", "Crisp greens with a honey-lemon dressing.", "150.00",
                        SpiceLevel.NONE, true, List.of("Mixed vegetables", "Honey"),
                        List.of(), List.of(), Set.of(DietType.VEGETARIAN, DietType.GLUTEN_FREE, DietType.LACTOSE_FREE))));

        add(existing, "Sweet Tooth Cafe", "Desserts & Cafe", "Dessert",
                "Indian sweets, fresh bakes and freshly brewed coffee all day.",
                "2 Fort Road, Tiruchengode", "20.00", ingredients, allergens, List.of(
                dish("Gulab Jamun (2 pcs)", "Soft milk dumplings soaked in cardamom syrup.", "90.00",
                        SpiceLevel.NONE, true, List.of("Milk", "Wheat flour", "Sugar"),
                        List.of("MILK", "GLUTEN"), List.of(), VEG),
                dish("Chocolate Brownie", "Fudgy chocolate brownie served warm.", "140.00",
                        SpiceLevel.NONE, true, List.of("Chocolate", "Egg", "Wheat flour", "Butter", "Sugar"),
                        List.of("EGG", "MILK", "GLUTEN"), List.of("TREE_NUT", "PEANUT"), VEG),
                dish("Cold Coffee", "Chilled coffee blended with milk and ice.", "120.00",
                        SpiceLevel.NONE, true, List.of("Milk", "Coffee", "Sugar"),
                        List.of("MILK"), List.of(), VEG_GF),
                dish("Mango Lassi", "Yogurt blended with ripe mango.", "100.00",
                        SpiceLevel.NONE, true, List.of("Mango", "Yogurt", "Sugar"),
                        List.of("MILK"), List.of(), VEG_GF),
                dish("Millet Jaggery Cookies", "Crumbly millet cookies sweetened with jaggery.", "90.00",
                        SpiceLevel.NONE, true, List.of("Millet", "Sugar"),
                        List.of(), List.of("TREE_NUT"), Set.of(DietType.VEGETARIAN, DietType.VEGAN, DietType.LACTOSE_FREE)),
                dish("Badam Halwa", "Slow-roasted almond halwa finished with ghee.", "160.00",
                        SpiceLevel.NONE, true, List.of("Almond", "Ghee", "Sugar"),
                        List.of("TREE_NUT", "MILK"), List.of(), VEG_GF)));
    }

    // ---------------------------------------------------------------- reference data

    private Map<String, Ingredient> ensureIngredients(Map<String, Allergen> allergens) {
        Map<String, Ingredient> map = new HashMap<>();
        ingredientRepository.findAll().forEach(i -> map.put(i.getName().toLowerCase(), i));
        ing(map, allergens, "Basmati rice", IngredientCategory.GRAIN, List.of());
        ing(map, allergens, "Idli batter", IngredientCategory.GRAIN, List.of());
        ing(map, allergens, "Coconut", IngredientCategory.FRUIT, List.of());
        ing(map, allergens, "Mustard seeds", IngredientCategory.SPICE, List.of("MUSTARD"));
        ing(map, allergens, "Yogurt", IngredientCategory.DAIRY, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Ghee", IngredientCategory.DAIRY, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Cheese", IngredientCategory.DAIRY, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Cream", IngredientCategory.DAIRY, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Milk", IngredientCategory.DAIRY, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Egg", IngredientCategory.OTHER, List.of("EGG"), DietType.VEGAN, DietType.JAIN);
        ing(map, allergens, "Mayonnaise", IngredientCategory.OTHER, List.of("EGG"), DietType.VEGAN);
        ing(map, allergens, "Tomato", IngredientCategory.VEGETABLE, List.of());
        ing(map, allergens, "Potato", IngredientCategory.VEGETABLE, List.of(), DietType.JAIN);
        ing(map, allergens, "Mutton", IngredientCategory.MEAT, List.of(), DietType.VEGETARIAN, DietType.VEGAN, DietType.JAIN);
        ing(map, allergens, "Pasta", IngredientCategory.GRAIN, List.of("GLUTEN"), DietType.GLUTEN_FREE);
        ing(map, allergens, "Wheat noodles", IngredientCategory.GRAIN, List.of("GLUTEN"), DietType.GLUTEN_FREE);
        ing(map, allergens, "Pizza dough", IngredientCategory.GRAIN, List.of("GLUTEN"), DietType.GLUTEN_FREE);
        ing(map, allergens, "Burger bun", IngredientCategory.GRAIN, List.of("GLUTEN", "SESAME"), DietType.GLUTEN_FREE);
        ing(map, allergens, "Pesto", IngredientCategory.OTHER, List.of("TREE_NUT", "MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Peanut butter", IngredientCategory.NUT, List.of("PEANUT"));
        ing(map, allergens, "Almond", IngredientCategory.NUT, List.of("TREE_NUT"));
        ing(map, allergens, "Tofu", IngredientCategory.LEGUME, List.of("SOY"));
        ing(map, allergens, "Chocolate", IngredientCategory.SWEETENER, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Sugar", IngredientCategory.SWEETENER, List.of());
        ing(map, allergens, "Coffee", IngredientCategory.OTHER, List.of());
        ing(map, allergens, "Mango", IngredientCategory.FRUIT, List.of());
        return map;
    }

    private void ing(Map<String, Ingredient> map, Map<String, Allergen> allergens, String name,
                     IngredientCategory category, List<String> allergenCodes, DietType... incompatible) {
        if (map.containsKey(name.toLowerCase())) {
            return;
        }
        Ingredient ingredient = new Ingredient(name, category);
        for (String code : allergenCodes) {
            ingredient.getCommonAllergens().add(allergens.get(code));
        }
        ingredient.getIncompatibleDiets().addAll(List.of(incompatible));
        map.put(name.toLowerCase(), ingredientRepository.save(ingredient));
    }

    // ---------------------------------------------------------------- restaurants and dishes

    private record Dish(String name, String description, String price, SpiceLevel spice, boolean complete,
                        List<String> ingredients, List<String> declared, List<String> mayContain,
                        Set<DietType> diets) {
    }

    private static Dish dish(String name, String description, String price, SpiceLevel spice, boolean complete,
                             List<String> ingredients, List<String> declared, List<String> mayContain,
                             Set<DietType> diets) {
        return new Dish(name, description, price, spice, complete, ingredients, declared, mayContain, diets);
    }

    private void add(Set<String> existing, String name, String cuisine, String dishCuisine, String description,
                     String address, String deliveryFee, Map<String, Ingredient> ingredients,
                     Map<String, Allergen> allergens, List<Dish> dishes) {
        if (existing.contains(name.toLowerCase())) {
            return;
        }
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setCuisine(cuisine);
        r.setDescription(description);
        r.setAddress(address);
        r.setDeliveryFee(new BigDecimal(deliveryFee));
        r = restaurantRepository.save(r);
        for (Dish d : dishes) {
            MenuItem m = new MenuItem();
            m.setRestaurant(r);
            m.setName(d.name());
            m.setDescription(d.description());
            m.setPrice(new BigDecimal(d.price()));
            m.setCuisine(dishCuisine);
            m.setSpiceLevel(d.spice());
            m.setAllergenInfoComplete(d.complete());
            m.getDietTags().addAll(new HashSet<>(d.diets()));
            for (String n : d.ingredients()) {
                m.getIngredients().add(new MenuItemIngredient(m, ingredients.get(n.toLowerCase())));
            }
            for (String code : d.declared()) {
                m.getAllergens().add(new MenuItemAllergen(m, allergens.get(code), AllergenSource.DECLARED));
            }
            for (String code : d.mayContain()) {
                m.getAllergens().add(new MenuItemAllergen(m, allergens.get(code), AllergenSource.MAY_CONTAIN));
            }
            menuItemRepository.save(m);
        }
    }
}
