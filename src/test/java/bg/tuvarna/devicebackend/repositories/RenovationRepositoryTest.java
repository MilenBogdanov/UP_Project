package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.Device;
import bg.tuvarna.devicebackend.models.entities.Renovation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RenovationRepositoryTest {

    @Autowired
    private RenovationRepository renovationRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    private Device device;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setSerialNumber("SN12345");
        deviceRepository.save(device);
    }

    @Test
    @DisplayName("save() и findById() трябва да запазват и намират Renovation с Device")
    void testSaveAndFindById() {
        Renovation renovation = new Renovation();
        renovation.setDescription("New paint");
        renovation.setRenovationDate(LocalDate.of(2025, 10, 18));
        renovation.setDevice(device);

        Renovation saved = renovationRepository.save(renovation);

        Optional<Renovation> found = renovationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("New paint");
        assertThat(found.get().getRenovationDate()).isEqualTo(LocalDate.of(2025, 10, 18));
        assertThat(found.get().getDevice().getSerialNumber()).isEqualTo("SN12345");
    }

    @Test
    @DisplayName("findAll() трябва да връща всички записи")
    void testFindAll() {
        Renovation r1 = new Renovation();
        r1.setDescription("Renovation 1");
        r1.setRenovationDate(LocalDate.now());
        r1.setDevice(device);

        Renovation r2 = new Renovation();
        r2.setDescription("Renovation 2");
        r2.setRenovationDate(LocalDate.now());
        r2.setDevice(device);

        renovationRepository.save(r1);
        renovationRepository.save(r2);

        List<Renovation> all = renovationRepository.findAll();
        assertThat(all).hasSize(2);
        assertThat(all)
                .extracting(Renovation::getDescription)
                .containsExactlyInAnyOrder("Renovation 1", "Renovation 2");
    }

    @Test
    @DisplayName("delete() и deleteById() трябва да изтриват успешно Renovation")
    void testDeleteAndDeleteById() {
        Renovation renovation = new Renovation();
        renovation.setDescription("To Delete");
        renovation.setRenovationDate(LocalDate.now());
        renovation.setDevice(device);
        renovationRepository.save(renovation);

        Long id = renovation.getId();
        assertThat(renovationRepository.existsById(id)).isTrue();

        // Изтриване по обект
        renovationRepository.delete(renovation);
        assertThat(renovationRepository.existsById(id)).isFalse();

        // Създаваме нов запис и изтриваме по ID
        Renovation another = new Renovation();
        another.setDescription("Second Delete");
        another.setRenovationDate(LocalDate.now());
        another.setDevice(device);
        renovationRepository.save(another);
        Long id2 = another.getId();

        renovationRepository.deleteById(id2);
        assertThat(renovationRepository.existsById(id2)).isFalse();
    }

    @Test
    @DisplayName("count() трябва да връща точния брой записи")
    void testCount() {
        Renovation r1 = new Renovation();
        r1.setDescription("R1");
        renovationRepository.save(r1);

        Renovation r2 = new Renovation();
        r2.setDescription("R2");
        renovationRepository.save(r2);

        assertThat(renovationRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Getter и Setter методи на Renovation трябва да работят коректно")
    void testEntityGettersAndSetters() {
        Renovation renovation = new Renovation();
        renovation.setId(10L);
        renovation.setDescription("Test description");
        renovation.setRenovationDate(LocalDate.of(2025, 10, 18));
        renovation.setDevice(device);

        assertThat(renovation.getId()).isEqualTo(10L);
        assertThat(renovation.getDescription()).isEqualTo("Test description");
        assertThat(renovation.getRenovationDate()).isEqualTo(LocalDate.of(2025, 10, 18));
        assertThat(renovation.getDevice()).isEqualTo(device);
    }
}