package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .fullName("Gosho Petrov")
                .address("Sofia")
                .email("gosho@abv.bg")
                .phone("0888123456")
                .password("1234")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);

        // втори потребител, който е ADMIN
        User admin = User.builder()
                .fullName("Admin User")
                .email("admin@site.com")
                .phone("0999999999")
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("getByEmail → намира по имейл")
    void testGetByEmail() {
        User found = userRepository.getByEmail("gosho@abv.bg");
        assertThat(found).isNotNull();
        assertThat(found.getFullName()).isEqualTo("Gosho Petrov");
    }

    @Test
    @DisplayName("getByPhone → намира по телефон")
    void testGetByPhone() {
        User found = userRepository.getByPhone("0888123456");
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("gosho@abv.bg");
    }

    @Test
    @DisplayName("findByEmailOrPhone → намира по имейл или телефон")
    void testFindByEmailOrPhone() {
        Optional<User> foundByEmail = userRepository.findByEmailOrPhone("gosho@abv.bg");
        Optional<User> foundByPhone = userRepository.findByEmailOrPhone("0888123456");
        Optional<User> notFound = userRepository.findByEmailOrPhone("nonexistent");

        assertThat(foundByEmail).isPresent();
        assertThat(foundByPhone).isPresent();
        assertThat(notFound).isNotPresent();
    }

    @Test
    @DisplayName("searchBy → намира по име")
    void userFindBySearchName() {
        Page<User> page = userRepository.searchBy("gosho", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getPhone()).isEqualTo("0888123456");
    }

    @Test
    @DisplayName("searchBy → намира по телефон")
    void userFindBySearchPhone() {
        Page<User> page = userRepository.searchBy("0888123456", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getFullName()).isEqualTo("Gosho Petrov");
    }

    @Test
    @DisplayName("searchBy → връща всички когато searchBy е null")
    void searchBy_ShouldReturnAll_WhenNull() {
        Page<User> page = userRepository.searchBy(null, Pageable.ofSize(10));
        // връща само не-ADMIN потребители
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAllUsers → връща всички не-ADMIN потребители")
    void getAllUsers_ShouldReturnNonAdminsOnly() {
        Page<User> page = userRepository.getAllUsers(Pageable.ofSize(10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("UserDetails интерфейс методи → трябва да връщат правилни стойности")
    void userDetailsMethods_ShouldWorkCorrectly() {
        assertThat(user.getUsername()).isEqualTo("gosho@abv.bg");
        assertThat(user.getPassword()).isEqualTo("1234");
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getAuthorities()).extracting("authority")
                .containsExactly("USER");
    }
}