package hongik.Todoing.domain.auth.adaptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailSenderAdaptor {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailSenderAdaptor(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    // 요청 스레드를 SMTP 응답까지 붙잡아두지 않도록 발송만 비동기로 분리.
    // 실패해도 이미 응답이 나간 뒤라 호출자에게 알릴 수 없어 여기서 로그만 남긴다.
    @Async("emailTaskExecutor")
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[투둥이] 이메일 인증번호");
        message.setText("인증번호: " + code + " (1분 이내 입력)");

        try {
            mailSender.send(message);
            log.info("[EMAIL_VERIFY] event=SEND_SUCCESS email={}", to);
        } catch (MailException e) {
            log.warn("[EMAIL_VERIFY] event=SEND_FAILED email={}", to, e);
        }
    }
}
