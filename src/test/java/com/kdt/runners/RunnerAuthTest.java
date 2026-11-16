package com.kdt.runners;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RunnerAuthTest {
    private static final String BASE = "http://127.0.0.1:8080";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RunnerRepository runnerRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwt;
    private Runner leader;
    private Runner member;

    @BeforeEach
    void setUp() {
        runnerRepository.deleteAll();
        leader = saveRunner("leader", "Leader1234!", "크루장", RunnerRole.LEADER);
        member = saveRunner("member", "Member1234!", "러너", RunnerRole.MEMBER);
    }

    private Runner saveRunner(String loginId, String password, String nickname, RunnerRole role) {
        Runner runner = new Runner();
        runner.setLoginId(loginId);
        runner.setPasswordHash(passwordEncoder.encode(password));
        runner.setNickname(nickname);
        runner.setRole(role);
        return runnerRepository.save(runner);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String login(String loginId, String password) throws Exception {
        String response = mvc.perform(post(BASE + "/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(loginId, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("accessToken").asString();
    }

    @Test
    void signupCreatesMember() throws Exception {
        mvc.perform(post(BASE + "/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SignupRequest("runner01", "Runner1234!", "첫러너"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void signupWithRoleLeaderStillCreatesMember() throws Exception {
        mvc.perform(post(BASE + "/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"runner01\",\"password\":\"Runner1234!\",\"nickname\":\"러너\",\"role\":\"LEADER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void signupDuplicateLoginId() throws Exception {
        mvc.perform(post(BASE + "/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SignupRequest("member", "Runner1234!", "러너"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    void loginWrongPassword() throws Exception {
        mvc.perform(post(BASE + "/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("member", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void loginReturnsBearerToken() throws Exception {
        mvc.perform(post(BASE + "/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("member", "Member1234!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void meWithoutToken() throws Exception {
        mvc.perform(get(BASE + "/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_FAILED"));
    }

    @Test
    void memberCannotListRunners() throws Exception {
        mvc.perform(get(BASE + "/api/v1/runners")
                        .header("Authorization", "Bearer " + login("member", "Member1234!")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void leaderCanListRunners() throws Exception {
        mvc.perform(get(BASE + "/api/v1/runners")
                        .header("Authorization", "Bearer " + login("leader", "Leader1234!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(leader.getId().intValue()))
                .andExpect(jsonPath("$[1].id").value(member.getId().intValue()))
                .andExpect(jsonPath("$[2]").doesNotExist());
    }

    @Test
    void selfDeleteInvalidatesToken() throws Exception {
        String access = login("member", "Member1234!");
        mvc.perform(delete(BASE + "/api/v1/runners/" + member.getId())
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get(BASE + "/api/v1/auth/me")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_FAILED"));
    }

    @Test
    void loginSetsRefreshCookie() throws Exception {
        mvc.perform(post(BASE + "/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("member", "Member1234!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=604800")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test
    void refreshWithoutCookie() throws Exception {
        mvc.perform(post(BASE + "/api/v1/auth/refresh").header("Origin", BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_FAILED"));
    }

    @Test
    void logoutClearsCookieButAccessStillWorks() throws Exception {
        String access = jwt.createAccess(member.getId());
        String refresh = jwt.createRefresh(member.getId());
        mvc.perform(post(BASE + "/api/v1/auth/logout")
                        .header("Origin", BASE).cookie(new Cookie("refreshToken", refresh)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
        mvc.perform(get(BASE + "/api/v1/auth/me")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId().intValue()));
    }

    @Test
    void copiedRefreshStillWorksAfterCookieLogout() throws Exception {
        String copied = jwt.createRefresh(member.getId());
        mvc.perform(post(BASE + "/api/v1/auth/logout").header("Origin", BASE))
                .andExpect(status().isNoContent());
        mvc.perform(post(BASE + "/api/v1/auth/refresh").header("Origin", BASE)
                        .cookie(new Cookie("refreshToken", copied)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));
    }

    // 필수 13개 외에 숫자 역할이 거부되는지 확인한다.
    @Test
    void numericRoleIsInvalid() throws Exception {
        mvc.perform(patch(BASE + "/api/v1/runners/" + member.getId() + "/role")
                        .header("Authorization", "Bearer " + jwt.createAccess(leader.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}