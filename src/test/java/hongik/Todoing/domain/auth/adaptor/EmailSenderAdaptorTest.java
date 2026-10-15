package hongik.Todoing.domain.auth.adaptor;

import hongik.Todoing.domain.auth.exception.EmailSendFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EmailSenderAdaptorTest {

    private static final String FROM_ADDRESS = "noreply@todoing.com";

    @Mock
    private JavaMailSender mailSender;

    private EmailSenderAdaptor emailSenderAdaptor;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        emailSenderAdaptor = new EmailSenderAdaptor(mailSender, FROM_ADDRESS);
    }

    @Test
    void sendVerificationCode_whenMailSenderSucceeds_shouldSendMessageWithFromAndCode() {
        // When
        emailSenderAdaptor.sendVerificationCode("user@example.com", "123456");

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo(FROM_ADDRESS);
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getText()).contains("123456");
    }

    @Test
    void sendVerificationCode_whenMailSenderThrows_shouldThrowEmailSendFailedException() {
        // Given
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        // When / Then
        assertThatThrownBy(() -> emailSenderAdaptor.sendVerificationCode("user@example.com", "123456"))
                .isInstanceOf(EmailSendFailedException.class);
    }
}
