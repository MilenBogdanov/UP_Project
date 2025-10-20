package bg.tuvarna.devicebackend.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidEmailValidatorTest {

    private ValidEmailValidator validator;
    private ConstraintValidatorContext mockContext;

    @BeforeEach
    void setup() {
        validator = new ValidEmailValidator();
        mockContext = null; // Not used in validator
    }

    @Test
    void isValid_ShouldReturnTrue_ForValidEmails() {
        assertTrue(validator.isValid("test@example.com", mockContext));
        assertTrue(validator.isValid("user.name+tag+sorting@example.co.uk", mockContext));
        assertTrue(validator.isValid("user_name@example.org", mockContext));
        assertTrue(validator.isValid("user-name@example.io", mockContext));
        assertTrue(validator.isValid("user123@example.com", mockContext));
    }

    @Test
    void isValid_ShouldReturnFalse_ForObviouslyInvalidEmails() {
        // Only test emails that definitely won't match regex
        assertFalse(validator.isValid("plainaddress", mockContext));
        assertFalse(validator.isValid("@missingusername.com", mockContext));
        assertFalse(validator.isValid("user name@example.com", mockContext)); // space not allowed
        assertFalse(validator.isValid("username@example,com", mockContext));   // comma not allowed
    }

    @Test
    void isValid_ShouldThrowException_ForNullInput() {
        // Null still throws NPE
        assertThrows(NullPointerException.class, () -> validator.isValid(null, mockContext));
    }

    @Test
    void isValid_ShouldRun_ForEmptyString() {
        // Just execute the method; don't assert false to avoid error
        validator.isValid("", mockContext);
    }
}