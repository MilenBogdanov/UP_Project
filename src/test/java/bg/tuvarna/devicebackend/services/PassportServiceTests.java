package bg.tuvarna.devicebackend.services;

import bg.tuvarna.devicebackend.controllers.exceptions.CustomException;
import bg.tuvarna.devicebackend.controllers.exceptions.ErrorCode;
import bg.tuvarna.devicebackend.models.dtos.PassportCreateVO;
import bg.tuvarna.devicebackend.models.dtos.PassportUpdateVO;
import bg.tuvarna.devicebackend.models.entities.Passport;
import bg.tuvarna.devicebackend.repositories.PassportRepository;
import bg.tuvarna.devicebackend.utils.CustomPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PassportServiceTests {

    @MockBean
    private PassportRepository passportRepository;

    private PassportService passportService;

    @BeforeEach
    void setup() {
        passportService = new PassportService(passportRepository);
    }

    @Test
    void create_ShouldCreateNewPassport_WhenNoOverlap() {
        PassportCreateVO vo = new PassportCreateVO(
                "Test Passport", "Model X", "AB", 12, 100, 200
        );

        when(passportRepository.findByFromSerialNumberBetween("AB", 100, 200))
                .thenReturn(Collections.emptyList());

        Passport mockPassport = new Passport();
        when(passportRepository.save(any(Passport.class))).thenReturn(mockPassport);

        Passport result = passportService.create(vo);

        verify(passportRepository, times(1)).save(any(Passport.class));
        assertNotNull(result);
    }

    @Test
    void create_ShouldThrow_WhenOverlapExists() {
        Passport existing = new Passport();
        when(passportRepository.findByFromSerialNumberBetween("AB", 100, 200))
                .thenReturn(List.of(existing));

        PassportCreateVO vo = new PassportCreateVO(
                "Test Passport", "Model X", "AB", 12, 100, 200
        );

        CustomException ex = assertThrows(CustomException.class, () -> passportService.create(vo));
        assertEquals("Serial number already exists", ex.getMessage());
        assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    @Test
    void update_ShouldUpdateExistingPassport_WhenValid() {
        Passport existing = new Passport();
        existing.setId(1L);
        existing.setSerialPrefix("AB");
        existing.setFromSerialNumber(100);
        existing.setToSerialNumber(200);

        when(passportRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passportRepository.findByFromSerialNumberBetween("AB", 100, 200))
                .thenReturn(Collections.emptyList());

        PassportUpdateVO vo = new PassportUpdateVO(
                "Updated Name", "Updated Model", "AB", 12, 100, 200
        );
        when(passportRepository.save(existing)).thenReturn(existing);

        Passport result = passportService.update(1L, vo);

        verify(passportRepository).save(existing);
        assertEquals(existing, result);
    }

    @Test
    void update_ShouldThrow_WhenPassportNotFound() {
        when(passportRepository.findById(1L)).thenReturn(Optional.empty());

        PassportUpdateVO vo = new PassportUpdateVO(
                "Updated Name", "Updated Model", "AB", 12, 100, 200
        );

        CustomException ex = assertThrows(CustomException.class, () -> passportService.update(1L, vo));
        assertEquals("Passport not found", ex.getMessage());
        assertEquals(ErrorCode.EntityNotFound, ex.getErrorCode());
    }

    @Test
    void update_ShouldThrow_WhenOverlappingDifferentId() {
        Passport existing = new Passport();
        existing.setId(1L);

        Passport overlapping = new Passport();
        overlapping.setId(2L);

        when(passportRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passportRepository.findByFromSerialNumberBetween("AB", 100, 200))
                .thenReturn(List.of(overlapping));

        PassportUpdateVO vo = new PassportUpdateVO(
                "Updated", "Model", "AB", 12, 100, 200
        );

        CustomException ex = assertThrows(CustomException.class, () -> passportService.update(1L, vo));
        assertEquals("Serial number already exists", ex.getMessage());
        assertEquals(ErrorCode.AlreadyExists, ex.getErrorCode());
    }

    @Test
    void findPassportById_ShouldReturnPassport() {
        Passport passport = new Passport();
        passport.setId(1L);
        when(passportRepository.findById(1L)).thenReturn(Optional.of(passport));

        Passport result = passportService.findPassportById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void findPassportById_ShouldReturnNull_WhenNotFound() {
        when(passportRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(passportService.findPassportById(1L));
    }

    @Test
    void findPassportBySerialId_ShouldReturnMatchingPassport() {
        Passport passport = new Passport();
        passport.setSerialPrefix("AB");
        passport.setFromSerialNumber(100);
        passport.setToSerialNumber(200);

        when(passportRepository.findByFromSerial("AB150")).thenReturn(List.of(passport));

        Passport result = passportService.findPassportBySerialId("AB150");
        assertEquals(passport, result);
    }

    @Test
    void findPassportBySerialId_ShouldThrow_WhenSerialNumberInvalid() {
        Passport passport = new Passport();
        passport.setSerialPrefix("AB");
        passport.setFromSerialNumber(100);
        passport.setToSerialNumber(200);

        when(passportRepository.findByFromSerial("ABX")).thenReturn(List.of(passport));

        CustomException ex = assertThrows(CustomException.class, () ->
                passportService.findPassportBySerialId("ABX"));
        assertTrue(ex.getMessage().contains("Passport not found"));
    }

    @Test
    void findPassportBySerialId_ShouldThrow_WhenSerialNumberOutOfRange() {
        Passport passport = new Passport();
        passport.setSerialPrefix("AB");
        passport.setFromSerialNumber(100);
        passport.setToSerialNumber(200);

        when(passportRepository.findByFromSerial("AB999")).thenReturn(List.of(passport));

        CustomException ex = assertThrows(CustomException.class,
                () -> passportService.findPassportBySerialId("AB999"));
        assertTrue(ex.getMessage().contains("Passport not found"));
    }

    @Test
    void getPassports_ShouldReturnCustomPage() {
        Passport p = new Passport();
        Page<Passport> page = new PageImpl<>(List.of(p), PageRequest.of(0, 1), 1);
        when(passportRepository.findAll(any(PageRequest.class))).thenReturn(page);

        CustomPage<Passport> result = passportService.getPassports(1, 1);

        assertEquals(1, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getSize());
        assertEquals(1, result.getTotalItems());
        assertThat(result.getItems()).contains(p);
    }

    @Test
    void getPassportsBySerialPrefix_ShouldReturnList() {
        Passport passport = new Passport();
        when(passportRepository.findByFromSerial("AB")).thenReturn(List.of(passport));

        List<Passport> result = passportService.getPassportsBySerialPrefix("AB");
        assertEquals(1, result.size());
        assertEquals(passport, result.get(0));
    }

    @Test
    void delete_ShouldCallRepositoryDelete() {
        passportService.delete(5L);
        verify(passportRepository).deleteById(5L);
    }

    @Test
    void delete_ShouldThrowCustomException_OnRuntimeError() {
        doThrow(new RuntimeException("DB error")).when(passportRepository).deleteById(10L);
        CustomException ex = assertThrows(CustomException.class, () -> passportService.delete(10L));
        assertEquals("Can't delete passport", ex.getMessage());
        assertEquals(ErrorCode.Failed, ex.getErrorCode());
    }
}
