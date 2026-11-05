package hongik.Todoing.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import hongik.Todoing.TodoingApplication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Locks in Controller -> Service -> Repository 단방향 의존성 (spring-api-rules §1).
 *
 * Rule A/C는 {@code hongik.Todoing.domain.verification} 패키지를 명시적으로 제외한다 — 이는 새로
 * 발견한 위반을 숨기기 위한 임시 조치가 아니라, 이 플랜의 스펙({@code .claude/docs/specs/layered-architecture-cleanup.md})
 * Q2에서 이미 결정된 범위를 그대로 반영한 것이다: verification 도메인은 삭제 예정이라 구조 정리 대상에서
 * 통째로 제외됐고, 그 결과 {@code VerificationService}가 {@code PassRepository}/{@code TodoRepository}/
 * {@code LabelRepository}(다른 도메인 repository)를, {@code VerificationAdaptor}가 {@code VerificationRepository}를
 * 여전히 직접 의존한다. 도메인이 삭제되면 이 예외도 함께 삭제해야 한다.
 */
@AnalyzeClasses(packagesOf = TodoingApplication.class)
public class LayeredArchitectureTest {

    private static final String VERIFICATION_DOMAIN_PACKAGE = "hongik.Todoing.domain.verification..";
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("hongik\\.Todoing\\.domain\\.([^.]+)\\..*");

    // Rule A: service/facade 클래스는 다른 도메인의 repository 패키지에 의존할 수 없다 (자기 도메인 repository는 허용).
    // 주의: 이 custom ArchCondition은 "위반 시 violated"를 직접 표현하므로 classes()를 써야 한다 —
    // noClasses()는 긍정형 조건(dependOnClassesThat 등)을 음화(not)하는 용도라서, 이미 음성으로 쓴
    // 커스텀 조건을 noClasses()에 그대로 끼우면 결과가 다시 반전되어 위반이 전혀 잡히지 않는다
    // (Task 12 회귀 확인 중 직접 재현해서 발견한 버그 — 최초 작성 시 noClasses()를 썼다가 교체함).
    @ArchTest
    static final ArchRule services_and_facades_must_not_access_other_domains_repositories =
            classes()
                    .that().resideInAnyPackage("..service..", "..facade..")
                    .and().resideOutsideOfPackage(VERIFICATION_DOMAIN_PACKAGE)
                    .should(dependOnAnotherDomainsRepository())
                    .because("Controller -> Service -> Repository는 같은 도메인 내에서만 허용되고, "
                            + "다른 도메인의 repository는 그 도메인의 QueryService를 거쳐야 한다 (spring-api-rules §1)");

    // Rule B: controller 클래스는 어떤 repository 패키지에도 의존할 수 없다 (같은 도메인이어도 금지).
    @ArchTest
    static final ArchRule controllers_must_not_access_any_repository =
            noClasses()
                    .that().resideInAnyPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("Controller는 항상 Service를 거쳐야 하고 Repository를 직접 의존해서는 안 된다 (spring-api-rules §1)");

    // Rule C: adaptor 클래스(외부 시스템 연동 격리용)는 repository 패키지에 의존할 수 없다.
    // verification 패키지는 대소문자가 섞여 있어(Adaptor) 두 표기 모두 매칭한다.
    @ArchTest
    static final ArchRule adaptors_must_not_access_any_repository =
            noClasses()
                    .that().resideInAnyPackage("..adaptor..", "..Adaptor..")
                    .and().resideOutsideOfPackage(VERIFICATION_DOMAIN_PACKAGE)
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("adaptor는 외부 시스템(RestTemplate/JavaMailSender 등) 연동 격리 전용이며, "
                            + "내부 Repository를 감싸는 가짜 레이어가 되어서는 안 된다 (spring-api-rules §10)");

    private static ArchCondition<JavaClass> dependOnAnotherDomainsRepository() {
        return new ArchCondition<JavaClass>("depend on another domain's repository") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originDomain = domainOf(origin.getPackageName());
                if (originDomain == null) {
                    return;
                }
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    if (!target.getPackageName().contains(".repository")) {
                        return;
                    }
                    String targetDomain = domainOf(target.getPackageName());
                    if (targetDomain != null && !targetDomain.equals(originDomain)) {
                        events.add(SimpleConditionEvent.violated(origin,
                                origin.getFullName() + " depends on " + target.getFullName()
                                        + " (domain '" + targetDomain + "', but class belongs to domain '" + originDomain + "')"));
                    }
                });
            }
        };
    }

    private static String domainOf(String packageName) {
        Matcher matcher = DOMAIN_PATTERN.matcher(packageName + ".");
        return matcher.matches() ? matcher.group(1) : null;
    }
}
