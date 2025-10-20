package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.DeviceCreateVO;
import bg.tuvarna.devicebackend.models.dtos.DeviceUpdateVO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.Passport;
import bg.tuvarna.devicebackend.models.entities.User;
import bg.tuvarna.devicebackend.repositories.DeviceRepository;
import bg.tuvarna.devicebackend.utils.CustomPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class DeviceServiceTests {

    @MockBean
    private DeviceRepository deviceRepository;

    @MockBean
    private PassportService passportService;

    private DeviceService deviceService;

    @BeforeEach
    void setup() {
        deviceService = new DeviceService(deviceRepository, passportService);
    }

    // ------------------ registerDevice ------------------
    @Test
    void registerDevice_ShouldSaveDevice_WhenValid() {
        Passport passport = new Passport();
        passport.setWarrantyMonths(12);
        User user = new User();

        when(passportService.findPassportBySerialId("AB123")).thenReturn(passport);

        deviceService.registerDevice("AB123", LocalDate.of(2025, 10, 18), user);

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(captor.capture());
        Device saved = captor.getValue();

        assertEquals("AB123", saved.getSerialNumber());
        assertEquals(user, saved.getUser());
        assertEquals(passport, saved.getPassport());
        assertEquals(LocalDate.of(2025, 10, 18).plusMonths(24), saved.getWarrantyExpirationDate());
    }

    @Test
    void registerDevice_ShouldThrow_WhenPassportFails() {
        when(passportService.findPassportBySerialId("AB123")).thenThrow(new RuntimeException());
        CustomException ex = assertThrows(CustomException.class,
                () -> deviceService.registerDevice("AB123", LocalDate.now(), new User()));
        assertEquals("Invalid serial number", ex.getMessage());
        assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }

    // ------------------ findDevice ------------------
    @Test
    void findDevice_ShouldReturnDevice_WhenExists() {
        Device device = new Device();
        device.setSerialNumber("D1");
        when(deviceRepository.findById("D1")).thenReturn(Optional.of(device));

        Device found = deviceService.findDevice("D1");
        assertEquals(device, found);
    }

    @Test
    void findDevice_ShouldReturnNull_WhenNotExists() {
        when(deviceRepository.findById("D1")).thenReturn(Optional.empty());
        assertNull(deviceService.findDevice("D1"));
    }

    // ------------------ isDeviceExists ------------------
    @Test
    void isDeviceExists_ShouldReturnDevice_WhenExists() {
        Device device = new Device();
        device.setSerialNumber("D1");
        when(deviceRepository.existsById("D1")).thenReturn(true);
        when(deviceRepository.findById("D1")).thenReturn(Optional.of(device));

        Device result = deviceService.isDeviceExists("D1");
        assertEquals(device, result);
    }

    @Test
    void isDeviceExists_ShouldThrow_WhenNotExists() {
        when(deviceRepository.existsById("D1")).thenReturn(false);
        CustomException ex = assertThrows(CustomException.class,
                () -> deviceService.isDeviceExists("D1"));
        assertEquals("Device not registered", ex.getMessage());
        assertEquals(ErrorCode.NotRegistered, ex.getErrorCode());
    }

    // ------------------ registerNewDevice ------------------
    @Test
    void registerNewDevice_ShouldRegister_WhenValid() {
        DeviceCreateVO vo = new DeviceCreateVO("D1", LocalDate.of(2025, 10, 18));
        User user = new User();
        Passport passport = new Passport();
        passport.setWarrantyMonths(12);

        when(deviceRepository.findById("D1")).thenReturn(Optional.empty());
        when(passportService.findPassportBySerialId("D1")).thenReturn(passport);
        when(deviceRepository.save(any(Device.class))).thenReturn(new Device());

        Device result = deviceService.registerNewDevice(vo, user);

        verify(deviceRepository).save(any(Device.class));
        assertNotNull(result);
    }

    @Test
    void registerNewDevice_ShouldThrow_WhenUserIsNull() {
        DeviceCreateVO vo = new DeviceCreateVO("D1", LocalDate.now());
        when(deviceRepository.findById("D1")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.registerNewDevice(vo, null));
        assertEquals("User not found", ex.getMessage());
        assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    @Test
    void registerNewDevice_ShouldThrow_WhenAlreadyExists() {
        DeviceCreateVO vo = new DeviceCreateVO("D1", LocalDate.now());
        when(deviceRepository.findById("D1")).thenReturn(Optional.of(new Device()));

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.registerNewDevice(vo, new User()));
        assertEquals("Device already registered", ex.getMessage());
        assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    // ------------------ updateDevice ------------------
    @Test
    void updateDevice_ShouldUpdate_WhenUserIsNull() {
        Passport passport = new Passport();
        passport.setWarrantyMonths(12);
        Device device = new Device();
        device.setSerialNumber("D1");
        device.setPassport(passport);
        device.setUser(null);
        when(deviceRepository.findById("D1")).thenReturn(Optional.of(device));

        DeviceUpdateVO vo = new DeviceUpdateVO(LocalDate.of(2025, 10, 18), "Test comment");
        deviceService.updateDevice("D1", vo);

        verify(deviceRepository).save(device);
        assertEquals("Test comment", device.getComment());
        assertEquals(LocalDate.of(2025, 10, 18).plusMonths(12), device.getWarrantyExpirationDate());
    }

    @Test
    void updateDevice_ShouldUpdate_WhenUserNotNull() {
        Passport passport = new Passport();
        passport.setWarrantyMonths(12);
        Device device = new Device();
        device.setSerialNumber("D1");
        device.setPassport(passport);
        device.setUser(new User());
        when(deviceRepository.findById("D1")).thenReturn(Optional.of(device));

        DeviceUpdateVO vo = new DeviceUpdateVO(LocalDate.of(2025, 10, 18), "Test comment");
        deviceService.updateDevice("D1", vo);

        assertEquals(LocalDate.of(2025, 10, 18).plusMonths(24), device.getWarrantyExpirationDate());
        assertEquals("Test comment", device.getComment());
    }

    @Test
    void updateDevice_ShouldThrow_WhenDeviceNotFound() {
        when(deviceRepository.findById("D1")).thenReturn(Optional.empty());
        DeviceUpdateVO vo = new DeviceUpdateVO(LocalDate.now(), "Test");

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.updateDevice("D1", vo));
        assertEquals("Device not found", ex.getMessage());
        assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    // ------------------ deleteDevice ------------------
    @Test
    void deleteDevice_ShouldCallRepositoryDelete() {
        deviceService.deleteDevice("D1");
        verify(deviceRepository).deleteBySerialNumber("D1");
    }

    @Test
    void deleteDevice_ShouldThrow_WhenRuntimeException() {
        doThrow(new RuntimeException()).when(deviceRepository).deleteBySerialNumber("D1");

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.deleteDevice("D1"));
        assertEquals("Cannot delete device: renovations exist", ex.getMessage());
        assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }

    // ------------------ addAnonymousDevice ------------------
    @Test
    void addAnonymousDevice_ShouldAdd_WhenValid() {
        DeviceCreateVO vo = new DeviceCreateVO("D1", LocalDate.of(2025, 10, 18));
        Passport passport = new Passport();
        passport.setWarrantyMonths(12);

        when(deviceRepository.findById("D1")).thenReturn(Optional.empty());
        when(passportService.findPassportBySerialId("D1")).thenReturn(passport);
        when(deviceRepository.save(any(Device.class))).thenReturn(new Device());

        Device result = deviceService.addAnonymousDevice(vo);

        verify(deviceRepository).save(any(Device.class));
        assertNotNull(result);
    }

    @Test
    void addAnonymousDevice_ShouldThrow_WhenAlreadyExists() {
        DeviceCreateVO vo = new DeviceCreateVO("D1", LocalDate.now());
        when(deviceRepository.findById("D1")).thenReturn(Optional.of(new Device()));

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.addAnonymousDevice(vo));
        assertEquals("Device already registered", ex.getMessage());
        assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    @Test
    void addAnonymousDevice_ShouldThrow_WhenPassportFails() {
        DeviceCreateVO vo = new DeviceCreateVO("D1", LocalDate.now());
        when(deviceRepository.findById("D1")).thenReturn(Optional.empty());
        when(passportService.findPassportBySerialId("D1")).thenThrow(new RuntimeException());

        CustomException ex = assertThrows(CustomException.class, () -> deviceService.addAnonymousDevice(vo));
        assertEquals("Invalid serial number", ex.getMessage());
        assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }

    // ------------------ getDevices ------------------
    @Test
    void getDevices_ShouldReturnAll_WhenSearchByNull() {
        Device device = new Device();
        Page<Device> page = new PageImpl<>(List.of(device), PageRequest.of(0, 1), 1);
        when(deviceRepository.getAllDevices(PageRequest.of(0, 1))).thenReturn(page);

        CustomPage<Device> result = deviceService.getDevices(null, 1, 1);

        assertEquals(1, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getTotalItems());
        assertThat(result.getItems()).contains(device);
    }

    @Test
    void getDevices_ShouldReturnFiltered_WhenSearchByProvided() {
        Device device = new Device();
        Page<Device> page = new PageImpl<>(List.of(device), PageRequest.of(0, 1), 1);
        when(deviceRepository.findAll("search", PageRequest.of(0, 1))).thenReturn(page);

        CustomPage<Device> result = deviceService.getDevices("search", 1, 1);

        assertEquals(1, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getTotalItems());
        assertThat(result.getItems()).contains(device);
    }
}
