package grp3.java.booknet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.thymeleaf.prefix=file:src/main/resources/templates/")
class AdminBookControllerFeature1Tests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin books URL redirects to the public catalog")
    @WithMockUser(username = "admin@booknet.vn", roles = {"ADMIN"})
    void adminBooksUrlRedirectsToCatalog() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalog"));
    }
}
