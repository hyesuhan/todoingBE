package hongik.Todoing.domain.member.adaptor;

import hongik.Todoing.domain.member.domain.User;
import hongik.Todoing.domain.member.exception.MemberNotFoundException;
import hongik.Todoing.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberAdaptor {

    private final MemberRepository memberRepository;

    public User save(User user) {
        return memberRepository.save(user);
    }

    public User findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> MemberNotFoundException.EXCEPTION);
    }
}
