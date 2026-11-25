package com.example.smartfoodordering;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Boots the whole application on H2: proves the context, JPA mappings, templates and URL rules fit together. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicPagesAreReachableWithoutLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void anonymousUserIsRedirectedToLoginForProtectedPages() throws Exception {
        mockMvc.perform(get("/cart")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/profile")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "customer@example.com", authorities = "ROLE_CUSTOMER")
    void customerCannotOpenAdminManagerOrKitchenAreas() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
        mockMvc.perform(get("/manager")).andExpect(status().isForbidden());
        mockMvc.perform(get("/kitchen")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef@example.com", authorities = "ROLE_KITCHEN_STAFF")
    void kitchenStaffCannotOpenCustomerOrAdminAreas() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
        mockMvc.perform(get("/cart")).andExpect(status().isForbidden());
        mockMvc.perform(get("/profile")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer@example.com", authorities = "ROLE_CUSTOMER")
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/profile/preferences")).andExpect(status().isForbidden());
        mockMvc.perform(post("/cart/clear")).andExpect(status().isForbidden());
    }
}
