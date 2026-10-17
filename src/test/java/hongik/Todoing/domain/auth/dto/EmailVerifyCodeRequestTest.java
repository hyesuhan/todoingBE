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

class EmailVerifyCodeRequestTest {

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
    void validate_whenCodeBlank_shouldHaveViolation() {
        Set<ConstraintViolation<EmailVerifyCodeRequest>> violations =
                validator.validate(new EmailVerifyCodeRequest("user@example.com", " "));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_whenEmailBlank_shouldHaveViolation() {
        Set<ConstraintViolation<EmailVerifyCodeRequest>> violations =
                validator.validate(new EmailVerifyCodeRequest(" ", "123456"));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_whenEmailAndCodeValid_shouldHaveNoViolation() {
        Set<ConstraintViolation<EmailVerifyCodeRequest>> violations =
                validator.validate(new EmailVerifyCodeRequest("user@example.com", "123456"));

        assertThat(violations).isEmpty();
    }
}
