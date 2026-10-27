package grp3.java.booknet;

import grp3.java.booknet.dto.UserSessionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / should return index view with catalog books")
    void testHomePageLoads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("books"))
                .andExpect(model().attributeExists("loginDto"))
                .andExpect(model().attributeExists("registerDto"));
    }

    @Test
    @DisplayName("POST /register should validate, register user and redirect with success alert")
    void testRegisterSuccess() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Trần Thị Mai")
                        .param("email", "tranmai@library.edu.vn")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("termsAgree", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("alertMessage", "You registered successfully!"))
                .andExpect(flash().attribute("successMessage", "You registered successfully!"))
                .andExpect(flash().attribute("activeTab", "login"))
                .andExpect(flash().attribute("prefillEmail", "tranmai@library.edu.vn"));
    }

    @Test
    @DisplayName("POST /register with mismatched passwords should return error")
    void testRegisterPasswordMismatch() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Trần Thị Mai")
                        .param("email", "mismatch@library.edu.vn")
                        .param("password", "secret123")
                        .param("confirmPassword", "different456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", containsString("Mật khẩu xác nhận không khớp")))
                .andExpect(flash().attribute("activeTab", "signup"));
    }

    @Test
    @DisplayName("POST /login should authenticate and redirect with 'Welcome to home page!' alert")
    void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "student@library.edu.vn")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("alertMessage", "Welcome to home page!"))
                .andExpect(flash().attribute("successMessage", "Welcome to home page!"))
                .andExpect(request().sessionAttribute("currentUser", notNullValue()));
    }

    @Test
    @DisplayName("POST /login with invalid credentials should return error")
    void testLoginFailure() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "student@library.edu.vn")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", containsString("Email hoặc mật khẩu không chính xác")))
                .andExpect(flash().attribute("activeTab", "login"));
    }

    @Test
    @DisplayName("GET /logout should clear session and redirect to /")
    void testLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", UserSessionDto.builder().fullName("Test User").build());

        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
