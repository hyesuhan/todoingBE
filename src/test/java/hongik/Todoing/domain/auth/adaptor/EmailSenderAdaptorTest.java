package hongik.Todoing.domain.auth.adaptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    void sendVerificationCode_whenMailSenderThrows_shouldSwallowExceptionSinceCallerAlreadyMovedOn() {
        // Given
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        // When / Then - 비동기 호출이라 실패를 호출자에게 던질 수 없음. 내부에서 삼키고 로그만 남긴다.
        assertThatCode(() -> emailSenderAdaptor.sendVerificationCode("user@example.com", "123456"))
                .doesNotThrowAnyException();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
