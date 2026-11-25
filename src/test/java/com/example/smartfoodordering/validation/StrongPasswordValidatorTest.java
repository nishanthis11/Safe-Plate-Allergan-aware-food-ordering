package com.example.smartfoodordering.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrongPasswordValidatorTest {
    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsPasswordWithAllCharacterClasses() {
        assertThat(validator.isValid("Demo@12345", null)).isTrue();
        assertThat(validator.isValid("short1A!", null)).isTrue();
    }

    @Test
    void rejectsWeakPasswords() {
        assertThat(validator.isValid(null, null)).isFalse();
        assertThat(validator.isValid("alllowercase1!", null)).isFalse();
        assertThat(validator.isValid("NoDigits!!", null)).isFalse();
        assertThat(validator.isValid("NoSpecial123", null)).isFalse();
        assertThat(validator.isValid("Sh0rt!", null)).isFalse();
    }
}
