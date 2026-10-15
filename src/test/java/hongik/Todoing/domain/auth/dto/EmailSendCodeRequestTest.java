package hongik.Todoing.domain.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSendCodeRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validate_whenEmailBlank_shouldHaveViolation() {
        Set<ConstraintViolation<EmailSendCodeRequest>> violations =
                validator.validate(new EmailSendCodeRequest(" "));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_whenEmailNotAnEmailFormat_shouldHaveViolation() {
        Set<ConstraintViolation<EmailSendCodeRequest>> violations =
                validator.validate(new EmailSendCodeRequest("not-an-email"));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_whenEmailValid_shouldHaveNoViolation() {
        Set<ConstraintViolation<EmailSendCodeRequest>> violations =
                validator.validate(new EmailSendCodeRequest("user@example.com"));

        assertThat(violations).isEmpty();
    }
}
