package hongik.Todoing.domain.verification.Adaptor;

import hongik.Todoing.common.annotation.Adaptor;
import hongik.Todoing.domain.verification.domain.Verification;
import hongik.Todoing.domain.verification.exception.VerificationErrorCode;
import hongik.Todoing.domain.verification.exception.VerificationException;
import hongik.Todoing.domain.verification.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class VerificationAdaptor {

    private final VerificationRepository verificationRepository;

    public Verification save(Verification verification){
        return verificationRepository.save(verification);
    }

    public Verification findById(Long verificationId){
        return verificationRepository
                .findById(verificationId)
                .orElseThrow(() -> new VerificationException(VerificationErrorCode.VERIFICATION_NOT_FOUND));
    }
}
