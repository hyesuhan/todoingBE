package hongik.Todoing.domain.auth.adaptor;

import hongik.Todoing.domain.auth.exception.EmailSendFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSenderAdaptor {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailSenderAdaptor(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[투둥이] 이메일 인증번호");
        message.setText("인증번호: " + code + " (1분 이내 입력)");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw EmailSendFailedException.EXCEPTION;
        }
    }
}
