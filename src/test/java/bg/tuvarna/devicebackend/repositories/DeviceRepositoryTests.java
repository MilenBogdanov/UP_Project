package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DeviceRepositoryTests {

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();

        Device d1 = new Device();
        d1.setSerialNumber("ABC-001");
        deviceRepository.save(d1);

        Device d2 = new Device();
        d2.setSerialNumber("XYZ-002");
        deviceRepository.save(d2);

        Device d3 = new Device();
        d3.setSerialNumber("ABC-003");
        deviceRepository.save(d3);
    }

    @Test
    @DisplayName("getAllDevices → връща всички устройства без филтър")
    void getAllDevices_ShouldReturnAllDevices() {
        Page<Device> page = deviceRepository.getAllDevices(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(Device::getSerialNumber)
                .containsExactlyInAnyOrder("ABC-001", "XYZ-002", "ABC-003");
    }

    @Test
    @DisplayName("findAll → намира устройства по частично съвпадение на сериен номер")
    void findAll_ShouldFilterBySerialNumber() {
        Page<Device> page = deviceRepository.findAll("abc", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Device::getSerialNumber)
                .containsExactlyInAnyOrder("ABC-001", "ABC-003");
    }

    @Test
    @DisplayName("findAll → връща всички устройства, когато searchBy е null")
    void findAll_ShouldReturnAll_WhenSearchIsNull() {
        Page<Device> page = deviceRepository.findAll((String) null, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findAll → връща всички устройства, когато searchBy е празен низ")
    void findAll_ShouldReturnAll_WhenSearchIsEmpty() {
        Page<Device> page = deviceRepository.findAll("", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("deleteBySerialNumber → изтрива устройството успешно")
    @Transactional
    void deleteBySerialNumber_ShouldRemoveDevice() {
        deviceRepository.deleteBySerialNumber("XYZ-002");

        Page<Device> page = deviceRepository.getAllDevices(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Device::getSerialNumber)
                .doesNotContain("XYZ-002");
    }

    @Test
    @DisplayName("deleteBySerialNumber → не хвърля грешка при несъществуващ сериен номер")
    @Transactional
    void deleteBySerialNumber_ShouldDoNothing_WhenNotFound() {
        deviceRepository.deleteBySerialNumber("NOPE-999");

        Page<Device> page = deviceRepository.getAllDevices(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("getAllDevices → работи коректно с пагинация")
    void getAllDevices_ShouldSupportPagination() {
        Page<Device> page = deviceRepository.getAllDevices(PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(2);
    }
}