package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.models.dtos.RenovationCreateVO;
import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.Renovation;
import bg.tuvarna.devicebackend.repositories.RenovationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class RenovationServiceTests {

    @MockBean
    private RenovationRepository renovationRepository;

    @MockBean
    private DeviceService deviceService;

    @Autowired
    private RenovationService renovationService;

    private Device device;

    @BeforeEach
    void setup() {
        device = new Device();
        device.setSerialNumber("SN-001");
    }

    @Test
    void save_ShouldCreateRenovationSuccessfully() {
        // Arrange
        RenovationCreateVO vo = new RenovationCreateVO(
                "SN-001",
                "Changed filter",
                LocalDate.of(2025, 10, 18)
        );

        when(deviceService.isDeviceExists("SN-001")).thenReturn(device);
        when(renovationRepository.save(any(Renovation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        renovationService.save(vo);

        // Assert
        ArgumentCaptor<Renovation> captor = ArgumentCaptor.forClass(Renovation.class);
        verify(renovationRepository, times(1)).save(captor.capture());
        Renovation saved = captor.getValue();

        assertEquals("Changed filter", saved.getDescription());
        assertEquals(LocalDate.of(2025, 10, 18), saved.getRenovationDate());
        assertEquals(device, saved.getDevice());
    }

    @Test
    void save_ShouldCallDeviceServiceBeforeSaving() {
        RenovationCreateVO vo = new RenovationCreateVO(
                "SN-002",
                "Replaced motor",
                LocalDate.now()
        );

        when(deviceService.isDeviceExists("SN-002")).thenReturn(device);
        renovationService.save(vo);

        // Проверяваме, че deviceService е извикан ПРЕДИ записването
        verify(deviceService, times(1)).isDeviceExists("SN-002");
        verify(renovationRepository, times(1)).save(any(Renovation.class));
    }
}