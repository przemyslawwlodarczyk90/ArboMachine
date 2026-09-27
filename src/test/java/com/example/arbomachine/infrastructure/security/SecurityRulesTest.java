package com.example.arbomachine.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dziala BEZ profilu "test", wiec obowiazuje prawdziwy SecurityConfig
 * (PermitAllTestSecurityConfig przepuszcza wszystko). Podmieniona jest tylko baza - na H2 w pamieci.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:securitytest",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auth.jwt.secret=security-rules-test-secret"
})
@AutoConfigureMockMvc
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousUserCannotReachProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/arborist/panel")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ARBORIST")
    void arboristCannotManageOtherAccountsOrOpenOtherPanels() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/users/1")).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"newRole\":\"OFFICIAL\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/official/panel")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    void officialCannotManageOtherAccountsOrOpenOtherPanels() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/arborist/panel")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanManageAccountsButCannotOpenOtherRolePanels() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isOk());
        mockMvc.perform(get("/api/arborist/panel")).andExpect(status().isForbidden());
    }
}
