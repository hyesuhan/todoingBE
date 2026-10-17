package hongik.Todoing.domain.auth.adaptor;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    // Redis Streams 컨슈머(EmailStreamListener) 스레드에서 호출됨 - 여기선 동기로 보내고,
    // 실패하면 그대로 예외를 던져서 컨슈머가 ack를 안 하도록(재시도 대상으로 남도록) 한다.
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("[투둥이] 이메일 인증번호");
        message.setText("인증번호: " + code + " (10분 이내 입력)");

        mailSender.send(message);
    }
}
