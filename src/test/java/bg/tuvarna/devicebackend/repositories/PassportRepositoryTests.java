package bg.tuvarna.devicebackend.repositories;

import bg.tuvarna.devicebackend.models.entities.Passport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PassportRepositoryTests {

    @Autowired
    private PassportRepository passportRepository;

    private Passport p1;
    private Passport p2;
    private Passport p3;

    @BeforeEach
    void setUp() {
        passportRepository.deleteAll();

        p1 = new Passport();
        p1.setSerialPrefix("AB");
        p1.setFromSerialNumber(1000);
        p1.setToSerialNumber(2000);
        passportRepository.save(p1);

        p2 = new Passport();
        p2.setSerialPrefix("AB");
        p2.setFromSerialNumber(2001);
        p2.setToSerialNumber(3000);
        passportRepository.save(p2);

        p3 = new Passport();
        p3.setSerialPrefix("CD");
        p3.setFromSerialNumber(1000);
        p3.setToSerialNumber(2000);
        passportRepository.save(p3);
    }

    @Test
    @DisplayName("findByFromSerialNumberBetween → намира паспорти в диапазона по prefix и от/до сериен номер")
    void findByFromSerialNumberBetween_ShouldReturnPassportsInRange() {
        List<Passport> results = passportRepository.findByFromSerialNumberBetween("AB", 1500, 2500);

        assertThat(results)
                .hasSize(2)
                .extracting(Passport::getSerialPrefix)
                .containsExactlyInAnyOrder("AB", "AB");
    }

    @Test
    @DisplayName("findByFromSerialNumberBetween → не връща нищо при несъвпадение")
    void findByFromSerialNumberBetween_ShouldReturnEmpty_WhenNoMatch() {
        List<Passport> results = passportRepository.findByFromSerialNumberBetween("ZZ", 100, 999);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByFromSerial → намира по съвпадение на префикс")
    void findByFromSerial_ShouldReturnPassportsMatchingPrefix() {
        List<Passport> results = passportRepository.findByFromSerial("AB-12345");

        assertThat(results)
                .hasSize(2)
                .allMatch(p -> p.getSerialPrefix().equals("AB"));
    }

    @Test
    @DisplayName("findByFromSerial → не връща нищо при липса на съвпадение")
    void findByFromSerial_ShouldReturnEmpty_WhenNoMatch() {
        List<Passport> results = passportRepository.findByFromSerial("ZZ-00000");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByFromSerialNumberBetween → връща точно едно съвпадение при гранични стойности")
    void findByFromSerialNumberBetween_ShouldReturnSingleMatch_WhenOnBoundary() {
        List<Passport> results = passportRepository.findByFromSerialNumberBetween("AB", 2000, 2000);
        assertThat(results)
                .hasSize(1)
                .first()
                .extracting(Passport::getFromSerialNumber)
                .isEqualTo(1000);
    }
}