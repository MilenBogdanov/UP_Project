package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.execptions.CustomException;
import bg.tuvarna.devicebackend.controllers.execptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.ChangePasswordVO;
import bg.tuvarna.devicebackend.models.dtos.UserCreateVO;
import bg.tuvarna.devicebackend.models.dtos.UserUpdateVO;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.models.enums.UserRole;
import bg.tuvarna.devicebackend.repositories.UserRepository;
import bg.tuvarna.devicebackend.utils.CustomPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class UserServiceTests {

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private DeviceService deviceService;
    @Autowired
    private UserService userService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("ivan@abv.bg");
        user.setPhone("0888");
        user.setFullName("Ivan");
        user.setPassword("encoded");
        user.setRole(UserRole.USER);
    }

    // ---------- register() tests ----------

    @Test
    void register_ShouldThrow_WhenEmailTaken() {
        UserCreateVO vo = new UserCreateVO("Ivan", "123", "ivan@abv.bg", "0888", "Address", LocalDate.now(), "SN1");
        when(userRepository.getByEmail("ivan@abv.bg")).thenReturn(new User());
        assertThrows(CustomException.class, () -> userService.register(vo));
    }

    @Test
    void register_ShouldThrow_WhenPhoneTaken() {
        UserCreateVO vo = new UserCreateVO("Ivan", "123", "new@abv.bg", "0888", "Address", LocalDate.now(), "SN1");
        when(userRepository.getByEmail("new@abv.bg")).thenReturn(null);
        when(userRepository.getByPhone("0888")).thenReturn(new User());
        assertThrows(CustomException.class, () -> userService.register(vo));
    }

    @Test
    void register_ShouldDeleteUser_WhenDeviceServiceThrows() {
        UserCreateVO vo = new UserCreateVO("Ivan", "123", "new@abv.bg", "0999", "Address", LocalDate.now(), "SN1");
        when(userRepository.getByEmail(anyString())).thenReturn(null);
        when(userRepository.getByPhone(anyString())).thenReturn(null);
        when(passwordEncoder.encode("123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        doThrow(new CustomException("device error", ErrorCode.AlreadyExists))
                .when(deviceService).alreadyExist(anyString());

        assertThrows(CustomException.class, () -> userService.register(vo));
        verify(userRepository).delete(any());
    }

    @Test
    void register_ShouldWork_WhenAllGood() {
        UserCreateVO vo = new UserCreateVO("Ivan", "123", "new@abv.bg", "0999", "Address", LocalDate.now(), "SN1");
        when(userRepository.getByEmail(anyString())).thenReturn(null);
        when(userRepository.getByPhone(anyString())).thenReturn(null);
        when(passwordEncoder.encode("123")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        doNothing().when(deviceService).alreadyExist(anyString());
        doNothing().when(deviceService).registerDevice(anyString(), any(), any());

        userService.register(vo);
        verify(deviceService).registerDevice(eq("SN1"), any(), any());
    }

    // ---------- isEmailTaken / isPhoneTaken ----------

    @Test
    void isEmailTaken_ShouldReturnTrue_WhenExists() {
        when(userRepository.getByEmail("ivan@abv.bg")).thenReturn(user);
        assertTrue(userService.isEmailTaken("ivan@abv.bg"));
    }

    @Test
    void isPhoneTaken_ShouldReturnFalse_WhenNotExists() {
        when(userRepository.getByPhone("123")).thenReturn(null);
        assertFalse(userService.isPhoneTaken("123"));
    }

    // ---------- getUserById / getUserByUsername ----------

    @Test
    void getUserById_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        User found = userService.getUserById(1L);
        assertEquals("Ivan", found.getFullName());
    }

    @Test
    void getUserById_ShouldThrow_WhenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getUserByUsername_ShouldReturnUser() {
        when(userRepository.findByEmailOrPhone("ivan")).thenReturn(Optional.of(user));
        User found = userService.getUserByUsername("ivan");
        assertEquals("Ivan", found.getFullName());
    }

    @Test
    void getUserByUsername_ShouldThrow_WhenNotFound() {
        when(userRepository.findByEmailOrPhone("ivan")).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> userService.getUserByUsername("ivan"));
    }

    // ---------- getUsers() ----------

    @Test
    void getUsers_ShouldReturnSearchResults() {
        when(userRepository.searchBy(anyString(), any())).thenReturn(new PageImpl<>(List.of(user)));
        CustomPage<?> page = userService.getUsers("ivan", 1, 5);
        assertEquals(1, page.getTotalItems());
        assertEquals(1, page.getCurrentPage());
    }

    @Test
    void getUsers_ShouldReturnAll_WhenSearchByIsNull() {
        when(userRepository.getAllUsers(any())).thenReturn(new PageImpl<>(List.of(user)));
        CustomPage<?> page = userService.getUsers(null, 1, 5);
        assertEquals(1, page.getTotalItems());
    }

    // ---------- updateUser() ----------

    @Test
    void updateUser_ShouldThrow_WhenAdmin() {
        user.setRole(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateVO vo = new UserUpdateVO(1L, "Name", "Addr", "Phone", "Mail");
        assertThrows(CustomException.class, () -> userService.updateUser(vo));
    }

    @Test
    void updateUser_ShouldThrow_WhenEmailTakenByOther() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.getByEmail("new@abv.bg")).thenReturn(new User());

        UserUpdateVO vo = new UserUpdateVO(1L, "Ivan", "Addr", "0888", "new@abv.bg");
        assertThrows(CustomException.class, () -> userService.updateUser(vo));
    }

    @Test
    void updateUser_ShouldThrow_WhenPhoneTakenByOther() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.getByEmail(any())).thenReturn(null);
        when(userRepository.getByPhone("0999")).thenReturn(new User());

        UserUpdateVO vo = new UserUpdateVO(1L, "Ivan", "Addr", "0999", "ivan@abv.bg");
        assertThrows(CustomException.class, () -> userService.updateUser(vo));
    }

    @Test
    void updateUser_ShouldUpdateSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.getByEmail(any())).thenReturn(null);
        when(userRepository.getByPhone(any())).thenReturn(null);

        UserUpdateVO vo = new UserUpdateVO(1L, "New", "Addr", "0888", "ivan@abv.bg");
        userService.updateUser(vo);
        verify(userRepository).save(any());
    }

    // ---------- updatePassword() ----------

    @Test
    void updatePassword_ShouldThrow_WhenAdmin() {
        user.setRole(UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThrows(CustomException.class, () ->
                userService.updatePassword(1L, new ChangePasswordVO("old", "new")));
    }

    @Test
    void updatePassword_ShouldThrow_WhenOldPasswordWrong() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        assertThrows(CustomException.class, () ->
                userService.updatePassword(1L, new ChangePasswordVO("old", "new")));
    }

    @Test
    void updatePassword_ShouldUpdate_WhenOldPasswordMatches() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encodedNew");

        userService.updatePassword(1L, new ChangePasswordVO("old", "new"));
        verify(userRepository).save(any());
    }
}