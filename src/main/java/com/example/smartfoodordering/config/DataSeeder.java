package com.example.smartfoodordering.config;

import com.example.smartfoodordering.entity.Allergen;
import com.example.smartfoodordering.entity.CustomerAllergy;
import com.example.smartfoodordering.entity.CustomerFoodProfile;
import com.example.smartfoodordering.entity.DeliveryAddress;
import com.example.smartfoodordering.entity.DietaryPreference;
import com.example.smartfoodordering.entity.Ingredient;
import com.example.smartfoodordering.entity.MenuItem;
import com.example.smartfoodordering.entity.MenuItemAllergen;
import com.example.smartfoodordering.entity.MenuItemIngredient;
import com.example.smartfoodordering.entity.Restaurant;
import com.example.smartfoodordering.entity.Role;
import com.example.smartfoodordering.entity.User;
import com.example.smartfoodordering.enums.AllergenSource;
import com.example.smartfoodordering.enums.AllergySeverity;
import com.example.smartfoodordering.enums.DietType;
import com.example.smartfoodordering.enums.IngredientCategory;
import com.example.smartfoodordering.enums.RoleName;
import com.example.smartfoodordering.enums.SpiceLevel;
import com.example.smartfoodordering.repository.AllergenRepository;
import com.example.smartfoodordering.repository.CustomerFoodProfileRepository;
import com.example.smartfoodordering.repository.DeliveryAddressRepository;
import com.example.smartfoodordering.repository.IngredientRepository;
import com.example.smartfoodordering.repository.MenuItemRepository;
import com.example.smartfoodordering.repository.RestaurantRepository;
import com.example.smartfoodordering.repository.RoleRepository;
import com.example.smartfoodordering.repository.UserRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads reference data (roles, allergens, ingredients) and a small demo dataset on an empty database.
 * Disable with app.seed.enabled=false. Demo accounts use the fixed password below: change or disable in production.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {
    static final String DEMO_PASSWORD = "Demo@12345";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AllergenRepository allergenRepository;
    private final IngredientRepository ingredientRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CustomerFoodProfileRepository profileRepository;
    private final DeliveryAddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public DataSeeder(RoleRepository roleRepository, UserRepository userRepository,
                      AllergenRepository allergenRepository, IngredientRepository ingredientRepository,
                      RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository,
                      CustomerFoodProfileRepository profileRepository, DeliveryAddressRepository addressRepository,
                      PasswordEncoder passwordEncoder, @Value("${app.seed.admin-password}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.allergenRepository = allergenRepository;
        this.ingredientRepository = ingredientRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.profileRepository = profileRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepository.count() > 0) {
            return; // already initialised
        }
        Map<RoleName, Role> roles = new HashMap<>();
        for (RoleName name : RoleName.values()) {
            roles.put(name, roleRepository.save(new Role(name)));
        }
        Map<String, Allergen> allergens = seedAllergens();
        Map<String, Ingredient> ingredients = seedIngredients(allergens);
        Restaurant greenLeaf = restaurant("Green Leaf Kitchen", "Thai",
                "Fresh Thai and South Indian dishes with clear allergen labelling.", "12 Garden Road, Tiruchengode");
        Restaurant spiceRoute = restaurant("Spice Route", "North Indian",
                "Classic North Indian curries and grills.", "4 Market Street, Tiruchengode");
        seedMenu(greenLeaf, spiceRoute, ingredients, allergens);

        user("admin@smartfood.local", "Site Admin", adminPassword, roles.get(RoleName.ROLE_ADMIN), null);
        user("manager@smartfood.local", "Green Leaf Manager", DEMO_PASSWORD,
                roles.get(RoleName.ROLE_RESTAURANT_MANAGER), greenLeaf);
        user("kitchen@smartfood.local", "Green Leaf Chef", DEMO_PASSWORD, roles.get(RoleName.ROLE_KITCHEN_STAFF), greenLeaf);
        User customer = user("customer@smartfood.local", "Priya Sharma", DEMO_PASSWORD,
                roles.get(RoleName.ROLE_CUSTOMER), null);
        seedCustomerProfile(customer, allergens);
    }

    private Map<String, Allergen> seedAllergens() {
        Map<String, Allergen> map = new HashMap<>();
        String[][] data = {
                {"PEANUT", "Peanut"}, {"TREE_NUT", "Tree nuts"}, {"MILK", "Milk"}, {"EGG", "Egg"},
                {"GLUTEN", "Wheat / gluten"}, {"SOY", "Soy"}, {"FISH", "Fish"}, {"SHELLFISH", "Shellfish"},
                {"SESAME", "Sesame"}, {"MUSTARD", "Mustard"}};
        for (String[] row : data) {
            map.put(row[0], allergenRepository.save(new Allergen(row[0], row[1], null)));
        }
        return map;
    }

    private Map<String, Ingredient> seedIngredients(Map<String, Allergen> allergens) {
        Map<String, Ingredient> map = new HashMap<>();
        ing(map, allergens, "Chicken", IngredientCategory.MEAT, List.of(), DietType.VEGETARIAN, DietType.VEGAN, DietType.JAIN);
        ing(map, allergens, "Peanut sauce", IngredientCategory.OTHER, List.of("PEANUT"));
        ing(map, allergens, "Rice noodles", IngredientCategory.GRAIN, List.of());
        ing(map, allergens, "Mixed vegetables", IngredientCategory.VEGETABLE, List.of());
        ing(map, allergens, "Tamarind sauce", IngredientCategory.OTHER, List.of());
        ing(map, allergens, "Soy sauce", IngredientCategory.OTHER, List.of("SOY", "GLUTEN"), DietType.GLUTEN_FREE);
        ing(map, allergens, "Millet", IngredientCategory.GRAIN, List.of());
        ing(map, allergens, "Quinoa", IngredientCategory.GRAIN, List.of());
        ing(map, allergens, "Sesame oil", IngredientCategory.OIL, List.of("SESAME"));
        ing(map, allergens, "Paneer", IngredientCategory.DAIRY, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Butter", IngredientCategory.DAIRY, List.of("MILK"), DietType.VEGAN, DietType.LACTOSE_FREE);
        ing(map, allergens, "Onion", IngredientCategory.VEGETABLE, List.of(), DietType.JAIN);
        ing(map, allergens, "Garlic", IngredientCategory.VEGETABLE, List.of(), DietType.JAIN);
        ing(map, allergens, "Fish", IngredientCategory.SEAFOOD, List.of("FISH"), DietType.VEGETARIAN, DietType.VEGAN, DietType.JAIN);
        ing(map, allergens, "Prawn", IngredientCategory.SEAFOOD, List.of("SHELLFISH"), DietType.VEGETARIAN, DietType.VEGAN, DietType.JAIN);
        ing(map, allergens, "Lentils", IngredientCategory.LEGUME, List.of());
        ing(map, allergens, "Mushroom", IngredientCategory.VEGETABLE, List.of());
        ing(map, allergens, "Coriander", IngredientCategory.SPICE, List.of());
        ing(map, allergens, "Wheat flour", IngredientCategory.GRAIN, List.of("GLUTEN"), DietType.GLUTEN_FREE);
        ing(map, allergens, "Cashew", IngredientCategory.NUT, List.of("TREE_NUT"));
        ing(map, allergens, "Honey", IngredientCategory.SWEETENER, List.of(), DietType.VEGAN);
        return map;
    }

    private void ing(Map<String, Ingredient> map, Map<String, Allergen> allergens, String name,
                     IngredientCategory category, List<String> allergenCodes, DietType... incompatible) {
        Ingredient ingredient = new Ingredient(name, category);
        for (String code : allergenCodes) {
            ingredient.getCommonAllergens().add(allergens.get(code));
        }
        ingredient.getIncompatibleDiets().addAll(List.of(incompatible));
        map.put(name, ingredientRepository.save(ingredient));
    }

    private Restaurant restaurant(String name, String cuisine, String description, String address) {
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setCuisine(cuisine);
        r.setDescription(description);
        r.setAddress(address);
        return restaurantRepository.save(r);
    }

    private void seedMenu(Restaurant greenLeaf, Restaurant spiceRoute, Map<String, Ingredient> ing,
                          Map<String, Allergen> al) {
        item(greenLeaf, "Chicken Satay with Peanut Sauce", "Grilled skewers with house peanut sauce.", "260.00",
                "Thai", SpiceLevel.MEDIUM, true, List.of("Chicken", "Peanut sauce"), List.of("PEANUT"), List.of(),
                Set.of(), ing, al);
        item(greenLeaf, "Veg Pad Thai", "Rice noodles, vegetables and tamarind sauce.", "240.00", "Thai",
                SpiceLevel.MILD, true, List.of("Rice noodles", "Mixed vegetables", "Tamarind sauce", "Soy sauce"),
                List.of("SOY"), List.of("PEANUT"), Set.of(DietType.VEGETARIAN), ing, al);
        item(greenLeaf, "Millet Khichdi", "Comforting millet and vegetable one-pot.", "180.00", "South Indian",
                SpiceLevel.MILD, true, List.of("Millet", "Mixed vegetables"), List.of(), List.of(),
                Set.of(DietType.VEGETARIAN, DietType.VEGAN, DietType.GLUTEN_FREE, DietType.LACTOSE_FREE), ing, al);
        item(greenLeaf, "Quinoa Veggie Bowl", "Quinoa, roasted vegetables, sesame dressing.", "210.00", "Healthy",
                SpiceLevel.NONE, true, List.of("Quinoa", "Mixed vegetables", "Sesame oil"), List.of("SESAME"), List.of(),
                Set.of(DietType.VEGETARIAN, DietType.VEGAN, DietType.GLUTEN_FREE, DietType.LACTOSE_FREE), ing, al);
        item(spiceRoute, "Paneer Butter Masala", "Paneer in a creamy tomato gravy.", "280.00", "North Indian",
                SpiceLevel.MEDIUM, true, List.of("Paneer", "Butter", "Onion"), List.of("MILK"), List.of(),
                Set.of(DietType.VEGETARIAN), ing, al);
        item(spiceRoute, "Grilled Fish Tikka", "Marinated fish grilled in the tandoor.", "320.00", "North Indian",
                SpiceLevel.HOT, false, List.of("Fish"), List.of("FISH"), List.of(), Set.of(), ing, al);
        item(spiceRoute, "Dal Tadka", "Yellow lentils tempered with garlic.", "170.00", "North Indian",
                SpiceLevel.MILD, true, List.of("Lentils", "Garlic"), List.of(), List.of(),
                Set.of(DietType.VEGETARIAN, DietType.VEGAN, DietType.GLUTEN_FREE, DietType.LACTOSE_FREE), ing, al);
        item(spiceRoute, "Prawn Masala", "Prawns in a spiced onion gravy.", "340.00", "North Indian",
                SpiceLevel.HOT, true, List.of("Prawn", "Onion"), List.of("SHELLFISH"), List.of(), Set.of(), ing, al);
    }

    private void item(Restaurant restaurant, String name, String description, String price, String cuisine,
                      SpiceLevel spice, boolean complete, List<String> ingredientNames, List<String> declared,
                      List<String> mayContain, Set<DietType> diets, Map<String, Ingredient> ing,
                      Map<String, Allergen> al) {
        MenuItem m = new MenuItem();
        m.setRestaurant(restaurant);
        m.setName(name);
        m.setDescription(description);
        m.setPrice(new BigDecimal(price));
        m.setCuisine(cuisine);
        m.setSpiceLevel(spice);
        m.setAllergenInfoComplete(complete);
        m.getDietTags().addAll(diets);
        for (String n : ingredientNames) {
            m.getIngredients().add(new MenuItemIngredient(m, ing.get(n)));
        }
        for (String code : declared) {
            m.getAllergens().add(new MenuItemAllergen(m, al.get(code), AllergenSource.DECLARED));
        }
        for (String code : mayContain) {
            m.getAllergens().add(new MenuItemAllergen(m, al.get(code), AllergenSource.MAY_CONTAIN));
        }
        menuItemRepository.save(m);
    }

    private User user(String email, String fullName, String password, Role role, Restaurant restaurant) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.getRoles().add(role);
        u.setRestaurant(restaurant);
        return userRepository.save(u);
    }

    private void seedCustomerProfile(User customer, Map<String, Allergen> allergens) {
        CustomerFoodProfile profile = new CustomerFoodProfile();
        profile.setUser(customer);
        profile.setMaxSpiceLevel(SpiceLevel.MEDIUM);
        profile.setConfirmedComplete(true);
        profile.getFavoriteCuisines().addAll(Set.of("Thai", "South Indian"));
        CustomerAllergy peanut = new CustomerAllergy();
        peanut.setProfile(profile);
        peanut.setAllergen(allergens.get("PEANUT"));
        peanut.setSeverity(AllergySeverity.SEVERE);
        profile.getAllergies().add(peanut);
        profile.getDietaryPreferences().add(new DietaryPreference(profile, DietType.LACTOSE_FREE, true));
        profileRepository.save(profile);

        DeliveryAddress address = new DeliveryAddress();
        address.setUser(customer);
        address.setLabel("Home");
        address.setLine1("22 Lake View Street");
        address.setCity("Tiruchengode");
        address.setState("Tamil Nadu");
        address.setPostalCode("637211");
        address.setContactPhone("9876543210");
        address.setDefaultAddress(true);
        addressRepository.save(address);
    }
}
