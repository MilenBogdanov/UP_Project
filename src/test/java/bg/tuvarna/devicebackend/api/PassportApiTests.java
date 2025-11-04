package bg.tuvarna.devicebackend.api;

import bg.tuvarna.devicebackend.controllers.exceptions.ErrorResponse;
import bg.tuvarna.devicebackend.models.entities.Passport;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.repositories.PassportRepository;
import bg.tuvarna.devicebackend.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PassportApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PassportRepository passportRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        User admin = User.builder()
                .fullName("Admin")
                .email("admin@abv.bg")
                .password(passwordEncoder.encode("Admin123$"))
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);

        MvcResult login = mvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "admin@abv.bg",
                              "password": "Admin123$"
                            }
                        """))
                .andExpect(status().isOk())
                .andReturn();

        jwtToken = mapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
        assertNotNull(jwtToken);
    }

    @AfterEach
    void tearDown() {
        passportRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createPassportSuccess() throws Exception {
        String body = """
            {
              "name": "Passport A",
              "model": "ModelX",
              "serialPrefix": "PA",
              "fromSerialNumber": 100,
              "toSerialNumber": 200,
              "warrantyMonths": 12
            }
        """;

        mvc.perform(post("/api/v1/passports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Passport A"))
                .andExpect(jsonPath("$.serialPrefix").value("PA"));
    }

    @Test
    void createPassportAlreadyExists() throws Exception {
        Passport passport = Passport.builder()
                .name("Existing")
                .model("M1")
                .serialPrefix("EX")
                .fromSerialNumber(1)
                .toSerialNumber(10)
                .warrantyMonths(12)
                .build();
        passportRepository.save(passport);

        String body = """
            {
              "name": "Existing",
              "model": "M1",
              "serialPrefix": "EX",
              "fromSerialNumber": 1,
              "toSerialNumber": 10,
              "warrantyMonths": 12
            }
        """;

        MvcResult result = mvc.perform(post("/api/v1/passports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtToken)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse error = mapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
        assertEquals("Serial number already exists", error.getError());
    }

    @Test
    void getPassportsReturnsOk() throws Exception {
        mvc.perform(get("/api/v1/passports")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void deletePassportSuccess() throws Exception {
        Passport passport = Passport.builder()
                .name("ToDelete")
                .model("DelModel")
                .serialPrefix("TD")
                .fromSerialNumber(1)
                .toSerialNumber(5)
                .warrantyMonths(6)
                .build();
        passport = passportRepository.save(passport);

        mvc.perform(delete("/api/v1/passports/" + passport.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        assertFalse(passportRepository.existsById(passport.getId()));
    }

    @Test
    void getPassportBySerialIdSuccess() throws Exception {
        Passport passport = Passport.builder()
                .name("SerialTest")
                .model("SModel")
                .serialPrefix("ST")
                .fromSerialNumber(100)
                .toSerialNumber(200)
                .warrantyMonths(12)
                .build();
        passport = passportRepository.save(passport);

        mvc.perform(get("/api/v1/passports/getBySerialId/ST150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SerialTest"))
                .andExpect(jsonPath("$.model").value("SModel"));
    }
}