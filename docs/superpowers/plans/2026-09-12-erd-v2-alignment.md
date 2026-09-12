# ERD v2 Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the JPA entities and their surrounding services/controllers in line with `.claude/todoing/v2-erd.md`, add the friend-chat feature, and fix a real auditing bug discovered along the way — without touching the `orders`/`pass` (payment) domain beyond what it already inherits for free.

**Architecture:** Entity-by-entity changes following the existing layered-architecture convention (`@RestController` → `@Service` → `@Repository`, DTOs for boundaries, static `from()` factories on response DTOs). Timestamp auditing is centralized in `BaseEntity` so most tables get `updated_at` "for free" via inheritance. Indexes are added as `@Table(indexes = ...)` so they ship with the entity that owns the column, not as a separate migration script (this project has no migration tool — Hibernate `ddl-auto` drives schema).

**Tech Stack:** Spring Boot 3.4.4, Java 21, Spring Data JPA / Hibernate, MySQL 9.2 (prod), JUnit 5 + Mockito (via `spring-boot-starter-test`), H2 (added here, test-scope only, to exercise real JPA auditing/persistence behavior).

**Spec:** `.claude/todoing/v2-erd.md` (DBML — paste into dbdiagram.io to view)

## Global Constraints

- Java 21 toolchain / Spring Boot 3.4.4 — do not add dependencies that require a different Boot version.
- No migration tool is in use; schema changes are expressed purely as JPA annotations (Hibernate generates DDL). Do not hand-write SQL migration files.
- Constructor injection + `@RequiredArgsConstructor` only, per `.claude/skills/layered-architecture/SKILL.md` — no field `@Autowired`.
- Controllers return DTOs wrapped in `ApiResponse<T>`, never entities. New error cases go in `hongik.Todoing.global.apiPayload.code.status.ErrorStatus`.
- `orders` / `pass` (the payment domain) are expected to be removed later — do not invest in new payment features; only accept the `updated_at` column they inherit automatically from `BaseEntity` (Task 2).
- Every non-trivial behavior change (a new branch, a mutated field, a new query) ships with a real test in the same task — no "add tests later."

---

### Task 1: Correct `.claude/todoing/v2-erd.md` before building against it

The last ERD revision introduced two mistakes that don't match the real, already-shipping `Friend` behavior. Fix the spec first since every other task in this plan is implemented *from* it.

**Files:**
- Modify: `.claude/todoing/v2-erd.md`

**Interfaces:** none (documentation only).

- [ ] **Step 1: Revert the invented `FriendStatus` values**

  `FriendController.blockFriend` / `FriendService.blockFriend` / `getFriendTodos` actively use `FriendStatus.BLOCKED`, and friendship is instant-accept (`Friend.of()` always sets `ACCEPTED`, there is no pending state). The ERD's `REQUESTED, ACCEPTED, REJECTED` was speculative and would silently drop the blocking feature. In `v2-erd.md`, change:

  ```
  Table friend {
  id bigint [pk, increment]
  status varchar [note: 'enum FriendStatus: REQUESTED, ACCEPTED, REJECTED']
  requester_id bigint [note: 'requester']
  receiver_id bigint [note: 'target']
  created_at timestamp
  updated_at timestamp [note: 'mutated on status transition (accept/reject)']
  ```

  to:

  ```
  Table friend {
  id bigint [pk, increment]
  status varchar [note: 'enum FriendStatus: BLOCKED, ACCEPTED (matches FriendStatus.java — no pending/request state today)']
  user_id bigint [note: 'the member who called addFriend()']
  friend_id bigint [note: 'the target member']
  created_at timestamp
  updated_at timestamp [note: 'mutated by blockFriend()']
  ```

  Update the two `Ref:` lines at the bottom accordingly:

  ```
  Ref: friend.user_id > user.id
  Ref: friend.friend_id > user.id
  ```

  (previously `friend.requester_id` / `friend.receiver_id`).

- [ ] **Step 2: Update `friend_message.friend_id` note and the `Indexes` block reference**

  No column changes needed there (it already pointed at `friend.id`, which still exists) — just re-read the file once fully to make sure nothing else references `requester_id`/`receiver_id`.

  Run: `grep -n "requester_id\|receiver_id\|REQUESTED\|REJECTED" .claude/todoing/v2-erd.md`
  Expected: no output.

- [ ] **Step 3: Commit**

  ```bash
  git add .claude/todoing/v2-erd.md
  git commit -m "docs: fix friend status/columns in v2 ERD to match shipped behavior"
  ```

---

### Task 2: Fix JPA auditing on `BaseEntity` and add `updated_at`

**This is the root-cause task.** `BaseEntity` declares `@CreatedDate` but is missing `@EntityListeners(AuditingEntityListener.class)` — without it, Hibernate never invokes the auditing callback, so `created_at` has silently been `null` on every entity that extends `BaseEntity` (`Chat`, `Friend`, `Order`, `Pass`, `PromptInput`, `Todo`, `TodoReply`, `VerificationUsage`) despite `@EnableJpaAuditing` being present on `TodoingApplication`. Fixing this one file fixes `created_at` everywhere and is the only place `updated_at` needs to be added for those 8 entities.

**Files:**
- Modify: `src/main/java/hongik/Todoing/global/common/BaseEntity.java`
- Modify: `build.gradle` (add test-only H2 — needed to actually exercise persistence/auditing in a test; nothing in this repo can verify Hibernate callbacks without a real `EntityManager`)
- Test: `src/test/java/hongik/Todoing/global/common/BaseEntityAuditingTest.java`

**Interfaces:**
- Produces: `BaseEntity.getUpdatedAt(): LocalDateTime` (Lombok `@Getter`), consumed by every subclass listed above and by Tasks 3–8.

- [ ] **Step 1: Add H2 for test-scope persistence tests**

  In `build.gradle`, add next to the existing `testImplementation` line:

  ```groovy
  	testImplementation 'com.h2database:h2'
  ```

- [ ] **Step 2: Write the failing test**

  Create `src/test/java/hongik/Todoing/global/common/BaseEntityAuditingTest.java`:

  ```java
  package hongik.Todoing.global.common;

  import hongik.Todoing.domain.todo.domain.Todo;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
  import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

  import java.time.LocalDate;

  import static org.assertj.core.api.Assertions.assertThat;

  @DataJpaTest
  class BaseEntityAuditingTest {

      @Autowired
      private TestEntityManager entityManager;

      @Test
      void createdAtAndUpdatedAtArePopulatedOnPersist() {
          Todo todo = Todo.builder()
                  .content("run 5k")
                  .todoDate(LocalDate.now())
                  .memberId(1L)
                  .labelId(1L)
                  .build();

          Todo saved = entityManager.persistFlushFind(todo);

          assertThat(saved.getCreatedAt()).isNotNull();
          assertThat(saved.getUpdatedAt()).isNotNull();
      }

      @Test
      void updatedAtAdvancesOnModification() throws InterruptedException {
          Todo todo = entityManager.persistFlushFind(
                  Todo.builder().content("run 5k").todoDate(LocalDate.now()).memberId(1L).labelId(1L).build()
          );
          var firstUpdatedAt = todo.getUpdatedAt();

          Thread.sleep(5);
          todo.updateComplete(true);
          entityManager.flush();
          entityManager.refresh(todo);

          assertThat(todo.getUpdatedAt()).isAfter(firstUpdatedAt);
      }
  }
  ```

- [ ] **Step 3: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.global.common.BaseEntityAuditingTest"`
  Expected: FAIL — `createdAt`/`updatedAt` are `null` (no listener wired, and `updatedAt` doesn't exist yet so this won't even compile until Step 4's field exists — confirm the *compile* error first, then re-run after adding just the field but not the listener, to see the assertion actually fail on `null`).

- [ ] **Step 4: Fix `BaseEntity`**

  Replace the full contents of `src/main/java/hongik/Todoing/global/common/BaseEntity.java`:

  ```java
  package hongik.Todoing.global.common;

  import jakarta.persistence.Column;
  import jakarta.persistence.EntityListeners;
  import jakarta.persistence.MappedSuperclass;
  import lombok.Getter;
  import org.springframework.data.annotation.CreatedDate;
  import org.springframework.data.annotation.LastModifiedDate;
  import org.springframework.data.jpa.domain.support.AuditingEntityListener;

  import java.time.LocalDateTime;

  @Getter
  @MappedSuperclass
  @EntityListeners(AuditingEntityListener.class)
  public class BaseEntity {

      @CreatedDate
      @Column(name = "created_at", updatable = false)
      private LocalDateTime createdAt;

      @LastModifiedDate
      @Column(name = "updated_at")
      private LocalDateTime updatedAt;
  }
  ```

- [ ] **Step 5: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.global.common.BaseEntityAuditingTest"`
  Expected: PASS (2 tests)

- [ ] **Step 6: Commit**

  ```bash
  git add build.gradle src/main/java/hongik/Todoing/global/common/BaseEntity.java src/test/java/hongik/Todoing/global/common/BaseEntityAuditingTest.java
  git commit -m "fix: wire AuditingEntityListener so created_at/updated_at actually populate"
  ```

---

### Task 3: `Chat` → `ai_chat` table, add read receipt

**Files:**
- Modify: `src/main/java/hongik/Todoing/domain/aiChat/domain/Chat.java`
- Test: `src/test/java/hongik/Todoing/domain/aiChat/domain/ChatTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 2).
- Produces: `Chat.isRead(): boolean`, `Chat.markRead(): void` — not consumed elsewhere in this plan, available for a future notifications feature.

- [ ] **Step 1: Write the failing test**

  Create `src/test/java/hongik/Todoing/domain/aiChat/domain/ChatTest.java`:

  ```java
  package hongik.Todoing.domain.aiChat.domain;

  import org.junit.jupiter.api.Test;

  import static org.assertj.core.api.Assertions.assertThat;

  class ChatTest {

      @Test
      void newChatIsUnreadUntilMarkedRead() {
          Chat chat = Chat.builder().message("hi").build();

          assertThat(chat.isRead()).isFalse();

          chat.markRead();

          assertThat(chat.isRead()).isTrue();
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.domain.aiChat.domain.ChatTest"`
  Expected: FAIL with "cannot find symbol: method isRead()" (compile error)

- [ ] **Step 3: Implement**

  Replace `src/main/java/hongik/Todoing/domain/aiChat/domain/Chat.java`:

  ```java
  package hongik.Todoing.domain.aiChat.domain;


  import hongik.Todoing.domain.member.domain.Member;
  import hongik.Todoing.global.common.BaseEntity;
  import jakarta.persistence.*;
  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  @Entity
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Table(
          name = "ai_chat",
          indexes = @Index(name = "idx_ai_chat_sender_created", columnList = "sender_id, created_at")
  )
  public class Chat extends BaseEntity {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long chatId;

      // 길이 제한
      @Column(length = 255)
      private String message;

      @ManyToOne
      @JoinColumn(name = "sender_id")
      private Member sender;

      @Column(name = "is_read", nullable = false)
      private boolean isRead;

      public void markRead() {
          this.isRead = true;
      }
  }
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.domain.aiChat.domain.ChatTest"`
  Expected: PASS

- [ ] **Step 5: Commit**

  ```bash
  git add src/main/java/hongik/Todoing/domain/aiChat/domain/Chat.java src/test/java/hongik/Todoing/domain/aiChat/domain/ChatTest.java
  git commit -m "feat: rename chat table to ai_chat and add read receipt"
  ```

---

### Task 4: `Member` — `nickname` rename, unique/not-null constraints, soft delete

`nickname` is the display name (was `name`), `email` is the actual login identifier (`MemberRepository.findByEmail`/`existsByEmail`, used by both email login and Kakao OAuth) and needs the unique constraint the code has always assumed but the schema never enforced. There is currently no member-deletion feature at all (hard or soft) — this task ships a complete, minimal one: a `DELETE /api/users/me` endpoint. It intentionally does **not** retrofit `deletedAt IS NULL` filtering into `findByEmail`/login — that touches the auth flow and deserves its own reviewed task; call this out, don't silently skip it.

**Files:**
- Modify: `src/main/java/hongik/Todoing/domain/member/domain/Member.java`
- Modify: `src/main/java/hongik/Todoing/domain/member/repository/MemberRepository.java`
- Modify: `src/main/java/hongik/Todoing/domain/member/service/MemberService.java`
- Modify: `src/main/java/hongik/Todoing/domain/member/controller/MemberController.java`
- Modify: `src/main/java/hongik/Todoing/domain/member/converter/MemberConverter.java`
- Modify: `src/main/java/hongik/Todoing/domain/member/dto/response/GetProfileDTO.java`
- Modify: `src/main/java/hongik/Todoing/domain/member/dto/response/UpdateProfileDTO.java`
- Modify: `src/main/java/hongik/Todoing/domain/auth/converter/AuthConverter.java`
- Modify: `src/main/java/hongik/Todoing/domain/auth/service/AuthService.java`
- Modify: `src/main/java/hongik/Todoing/domain/auth/dto/SignUpRequestDto.java`
- Modify: `src/main/java/hongik/Todoing/domain/auth/util/PrincipalDetails.java`
- Modify: `src/main/java/hongik/Todoing/domain/friend/converter/FriendConverter.java`
- Test: `src/test/java/hongik/Todoing/domain/member/domain/MemberTest.java`

**Interfaces:**
- Produces: `Member.getNickname(): String`, `Member.updateNickname(String): void`, `Member.isDeleted(): boolean`, `Member.delete(): void`. `AuthConverter.toMember(email, nickname, password, encoder)` keeps the same parameter order/count — only the field it sets changes name.

- [ ] **Step 1: Write the failing test**

  Create `src/test/java/hongik/Todoing/domain/member/domain/MemberTest.java`:

  ```java
  package hongik.Todoing.domain.member.domain;

  import org.junit.jupiter.api.Test;

  import static org.assertj.core.api.Assertions.assertThat;

  class MemberTest {

      @Test
      void updateNicknameChangesDisplayName() {
          Member member = Member.builder().nickname("old").email("a@b.com").role("ROLE_USER").build();

          member.updateNickname("new");

          assertThat(member.getNickname()).isEqualTo("new");
      }

      @Test
      void deleteMarksMemberAsDeleted() {
          Member member = Member.builder().nickname("old").email("a@b.com").role("ROLE_USER").build();

          assertThat(member.isDeleted()).isFalse();

          member.delete();

          assertThat(member.isDeleted()).isTrue();
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.domain.member.domain.MemberTest"`
  Expected: FAIL — compile error, `nickname`/`isDeleted`/`delete` don't exist on `Member` yet.

- [ ] **Step 3: Rewrite `Member`**

  Replace `src/main/java/hongik/Todoing/domain/member/domain/Member.java`:

  ```java
  package hongik.Todoing.domain.member.domain;

  import hongik.Todoing.domain.friend.domain.Friend;
  import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
  import hongik.Todoing.global.apiPayload.exception.GeneralException;
  import hongik.Todoing.global.common.BaseEntity;
  import jakarta.persistence.*;
  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  import java.time.LocalDateTime;
  import java.util.ArrayList;
  import java.util.Arrays;
  import java.util.List;

  @Entity
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Table(name = "`user`")
  public class Member extends BaseEntity {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(nullable = false, unique = true)
      private String nickname;

      private String password;

      @Column(nullable = false, unique = true)
      private String email;

      private String role; // ROLE_USER, ROLE_ADMIN

      private LocalDateTime deletedAt;

      public List<String> getRoleList() {
          if(!this.role.isEmpty()) {
              return Arrays.asList(this.role.split(","));
          }
          return new ArrayList<>();
      }

      public void updateNickname(String nickname) {
          this.nickname = nickname;
      }

      public void updatePassword(String password) {
          this.password = password;
      }

      public boolean isDeleted() {
          return this.deletedAt != null;
      }

      public void delete() {
          this.deletedAt = LocalDateTime.now();
      }

      public Friend createFriendship(Member target) {

          if (this.equals(target)) {
              throw new GeneralException(ErrorStatus.CANNOT_BE_FRIEND_WITH_SELF);
          }
          return Friend.of(this, target);
      }
  }
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.domain.member.domain.MemberTest"`
  Expected: PASS

- [ ] **Step 5: Fix every other compile error the rename causes**

  `./gradlew compileJava` will now fail at each of these call sites — fix them one by one, they're all mechanical `.name`/`.getName()`/`.updateName()` → `.nickname`/`.getNickname()`/`.updateNickname()` swaps:

  `src/main/java/hongik/Todoing/domain/member/repository/MemberRepository.java` — rename the unused `findByName` to keep it consistent (nothing calls it, but a stale name is misleading):
  ```java
  package hongik.Todoing.domain.member.repository;

  import hongik.Todoing.domain.member.domain.Member;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.util.Optional;

  public interface MemberRepository extends JpaRepository<Member, Long> {
      Optional<Member> findById(Long id);
      Optional<Member> findByNickname(String nickname);
      Optional<Member> findByEmail(String email);
      boolean existsByEmail(String email);
  }
  ```

  `src/main/java/hongik/Todoing/domain/member/converter/MemberConverter.java`:
  ```java
  package hongik.Todoing.domain.member.converter;

  import hongik.Todoing.domain.member.dto.response.GetProfileDTO;

  public class MemberConverter {

      public static GetProfileDTO toGetProfileDTO(String email, String nickname) {
          return new GetProfileDTO(email, nickname);
      }
  }
  ```

  `src/main/java/hongik/Todoing/domain/member/dto/response/GetProfileDTO.java`:
  ```java
  package hongik.Todoing.domain.member.dto.response;

  public record GetProfileDTO(
          String email,
          String nickname
  ) { }
  ```

  `src/main/java/hongik/Todoing/domain/member/dto/response/UpdateProfileDTO.java`:
  ```java
  package hongik.Todoing.domain.member.dto.response;

  public record UpdateProfileDTO(
          String nickname,
          String password
  ) { }
  ```

  `src/main/java/hongik/Todoing/domain/member/service/MemberService.java` — update the two call sites and add `withdraw`:
  ```java
  package hongik.Todoing.domain.member.service;

  import hongik.Todoing.domain.member.converter.MemberConverter;
  import hongik.Todoing.domain.member.domain.Member;
  import hongik.Todoing.domain.member.dto.response.GetProfileDTO;
  import hongik.Todoing.domain.member.dto.response.UpdateProfileDTO;
  import hongik.Todoing.domain.member.repository.MemberRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.security.crypto.password.PasswordEncoder;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  public class MemberService {

      private final PasswordEncoder passwordEncoder;
      private final MemberRepository memberRepository;

      public GetProfileDTO getProfile(Member member) {
          return MemberConverter.toGetProfileDTO(member.getEmail(), member.getNickname());
      }

      @Transactional
      public void updateProfile(Member member, UpdateProfileDTO request) {
          if(request.nickname() != null)
              member.updateNickname(request.nickname());

          if(request.password() != null) {
              String encoded = passwordEncoder.encode(request.password());
              member.updatePassword(encoded);
          }

          memberRepository.save(member);
      }

      @Transactional
      public void withdraw(Member member) {
          member.delete();
          memberRepository.save(member);
      }
  }
  ```

  `src/main/java/hongik/Todoing/domain/member/controller/MemberController.java` — add the withdraw endpoint:
  ```java
  package hongik.Todoing.domain.member.controller;

  import hongik.Todoing.domain.auth.util.PrincipalDetails;
  import hongik.Todoing.domain.member.domain.Member;
  import hongik.Todoing.domain.member.dto.response.GetProfileDTO;
  import hongik.Todoing.domain.member.dto.response.UpdateProfileDTO;
  import hongik.Todoing.domain.member.service.MemberService;
  import hongik.Todoing.global.apiPayload.ApiResponse;
  import hongik.Todoing.global.apiPayload.code.status.SuccessStatus;
  import io.swagger.v3.oas.annotations.Operation;
  import lombok.RequiredArgsConstructor;
  import org.springframework.security.core.annotation.AuthenticationPrincipal;
  import org.springframework.web.bind.annotation.*;

  @RestController
  @RequestMapping("/api/users")
  @RequiredArgsConstructor
  public class MemberController {

      private final MemberService memberService;

      @Operation(summary = "내 프로필을 조회합니다.")
      @GetMapping("/me")
      public ApiResponse<GetProfileDTO> getMyProfile(
              @AuthenticationPrincipal PrincipalDetails principal) {
          Member member = principal.getMember();
          return ApiResponse.onSuccess(memberService.getProfile(member));
      }

      @Operation(summary = "내 프로필을 변경합니다.")
      @PatchMapping("/me")
      public ApiResponse<Void> updateMyProfile (
              @AuthenticationPrincipal PrincipalDetails principal,
              @RequestBody UpdateProfileDTO dto
              ) {
          Member member = principal.getMember();
          memberService.updateProfile(member, dto);
          return ApiResponse.of(SuccessStatus._OK, null);
      }

      @Operation(summary = "회원을 탈퇴(소프트 삭제)합니다.")
      @DeleteMapping("/me")
      public ApiResponse<Void> withdraw(
              @AuthenticationPrincipal PrincipalDetails principal) {
          memberService.withdraw(principal.getMember());
          return ApiResponse.onSuccess(null);
      }
  }
  ```

  `src/main/java/hongik/Todoing/domain/auth/dto/SignUpRequestDto.java`:
  ```java
  package hongik.Todoing.domain.auth.dto;

  import lombok.Getter;

  @Getter
  public class SignUpRequestDto {

      private String nickname;
      private String email;
      private String password;
  }
  ```

  `src/main/java/hongik/Todoing/domain/auth/converter/AuthConverter.java`:
  ```java
  package hongik.Todoing.domain.auth.converter;

  import hongik.Todoing.domain.auth.dto.KakaoLoginResponseDto;
  import hongik.Todoing.domain.member.domain.Member;
  import org.springframework.security.crypto.password.PasswordEncoder;

  public class AuthConverter {

      public static Member toMember(String email, String nickname, String password, PasswordEncoder passwordEncoder) {

          String passwordToUse = password != null ? passwordEncoder.encode(password) :
                  passwordEncoder.encode("defaultPassword");
          return Member.builder()
                  .email(email)
                  .role("ROLE_USER")
                  .password(passwordToUse)
                  .nickname(nickname)
                  .build();
      }

      public static KakaoLoginResponseDto JoinResponse(Member member, String accessToken) {
          return KakaoLoginResponseDto.builder()
                  .email(member.getEmail())
                  .name(member.getNickname())
                  .accessToken(accessToken)
                  .build();
      }
  }
  ```

  (`KakaoLoginResponseDto.name` is left as-is — it's the outbound API field name, not the entity field, so no reason to churn the wire contract.)

  In `src/main/java/hongik/Todoing/domain/auth/service/AuthService.java`, change:
  ```java
  Member newMember = AuthConverter.toMember(
          kakaoProfile.getKakao_account().getEmail(),
          kakaoProfile.getProperties().getNickname(),
          "OAUTH",
          passwordEncoder
  );
  ```
  (unchanged — already passes the Kakao nickname positionally) and:
  ```java
  Member member = Member.builder()
          .nickname(request.getNickname())
          .email(request.getEmail())
          .password(encodedPassword)
          .role("ROLE_USER")
          .build();
  ```
  (was `.name(request.getName())`).

  `src/main/java/hongik/Todoing/domain/auth/util/PrincipalDetails.java` — the test-only constructor:
  ```java
  public PrincipalDetails(String username, String password, String role) {
      this.member = Member.builder()
              .nickname(username)
              .password(password)
              .role(role)
              .build();
  }
  ```

  `src/main/java/hongik/Todoing/domain/friend/converter/FriendConverter.java`:
  ```java
  public static FriendResponseDTO toFriendResponse(Friend friend) {
      Member target = friend.getFriend();
      return new FriendResponseDTO(
              target.getId(),
              target.getNickname(),
              friend.getStatus());
  }
  ```

- [ ] **Step 6: Full compile check**

  Run: `./gradlew compileJava`
  Expected: BUILD SUCCESSFUL, zero references to `Member#getName/updateName` remain.

  Run: `grep -rn "\.getName()\|updateName(" --include='*.java' src/main/java/hongik/Todoing/domain/member src/main/java/hongik/Todoing/domain/auth src/main/java/hongik/Todoing/domain/friend`
  Expected: no output.

- [ ] **Step 7: Run the full test suite**

  Run: `./gradlew test`
  Expected: PASS (all tests, including Tasks 2–3's)

- [ ] **Step 8: Commit**

  ```bash
  git add -A
  git commit -m "refactor: rename Member.name to nickname, enforce email/nickname uniqueness, add soft delete"
  ```

  **Known follow-up (do not silently skip, call it out to the user):** `MemberRepository.findByEmail`/`existsByEmail` and the Kakao/email login flows do not yet exclude withdrawn (`deletedAt != null`) members — a withdrawn user can currently still log in. That's an auth-flow change touching `AuthService`/`MemberRepository` and deserves its own reviewed task.

---

### Task 5: `Verification` gets `created_at`/`updated_at`

`Verification` currently has no timestamp at all, even though the ERD calls for `created_at`. It doesn't mutate after being written, so inheriting `updated_at` too (rather than a second `@MappedSuperclass`) is the cheaper, consistent choice.

**Files:**
- Modify: `src/main/java/hongik/Todoing/domain/verification/domain/Verification.java`
- Test: `src/test/java/hongik/Todoing/domain/verification/domain/VerificationPersistenceTest.java`

**Interfaces:**
- Consumes: `BaseEntity` (Task 2).

- [ ] **Step 1: Write the failing test**

  Create `src/test/java/hongik/Todoing/domain/verification/domain/VerificationPersistenceTest.java`:

  ```java
  package hongik.Todoing.domain.verification.domain;

  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
  import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

  import static org.assertj.core.api.Assertions.assertThat;

  @DataJpaTest
  class VerificationPersistenceTest {

      @Autowired
      private TestEntityManager entityManager;

      @Test
      void createdAtIsPopulatedOnPersist() {
          Verification verification = Verification.builder()
                  .type(VerificationType.TEXT)
                  .todoId(1L)
                  .success(true)
                  .confidence(0.9)
                  .build();

          Verification saved = entityManager.persistFlushFind(verification);

          assertThat(saved.getCreatedAt()).isNotNull();
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.domain.verification.domain.VerificationPersistenceTest"`
  Expected: FAIL — compile error, `getCreatedAt()` doesn't exist on `Verification` yet.

- [ ] **Step 3: Implement**

  Replace `src/main/java/hongik/Todoing/domain/verification/domain/Verification.java`:

  ```java
  package hongik.Todoing.domain.verification.domain;

  import hongik.Todoing.global.common.BaseEntity;
  import jakarta.persistence.*;
  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  @Entity
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Table(indexes = @Index(name = "idx_verification_todo_id", columnList = "todo_id"))
  public class Verification extends BaseEntity {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long verificationId;

      @Enumerated(EnumType.STRING)
      private VerificationType type;

      private Boolean success;

      private double confidence;

      @Column(name = "todo_id")
      private Long todoId;
  }
  ```

  (This also folds in Task 8's index for this table — see Task 8's note.)

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.domain.verification.domain.VerificationPersistenceTest"`
  Expected: PASS

- [ ] **Step 5: Commit**

  ```bash
  git add src/main/java/hongik/Todoing/domain/verification/domain/Verification.java src/test/java/hongik/Todoing/domain/verification/domain/VerificationPersistenceTest.java
  git commit -m "feat: add created_at/updated_at and todo_id index to Verification"
  ```

---

### Task 6: `Todo` — wire up `verification_id` / add `verified_at`

`Todo.verification_id` exists as a column today but nothing ever sets it — `VerificationService.processVerification` saves a `Verification` row and flips `todo.updateComplete(true)` on success, but never links the two. This task fixes the dangling field the ERD assumes is wired, and renames the ugly snake_case Java field (`verification_id`) to camelCase while touching it.

**Files:**
- Modify: `src/main/java/hongik/Todoing/domain/todo/domain/Todo.java`
- Modify: `src/main/java/hongik/Todoing/domain/verification/service/VerificationService.java`
- Test: `src/test/java/hongik/Todoing/domain/todo/domain/TodoTest.java`
- Test: `src/test/java/hongik/Todoing/domain/verification/service/VerificationServiceTest.java`

**Interfaces:**
- Produces: `Todo.markVerified(Long verificationId, LocalDateTime at): void`, `Todo.getVerificationId(): Long`, `Todo.getVerifiedAt(): LocalDateTime`.

- [ ] **Step 1: Write the failing `Todo` test**

  Create `src/test/java/hongik/Todoing/domain/todo/domain/TodoTest.java`:

  ```java
  package hongik.Todoing.domain.todo.domain;

  import org.junit.jupiter.api.Test;

  import java.time.LocalDate;
  import java.time.LocalDateTime;

  import static org.assertj.core.api.Assertions.assertThat;

  class TodoTest {

      @Test
      void markVerifiedSetsVerificationIdAndTimestamp() {
          Todo todo = Todo.builder().content("run 5k").todoDate(LocalDate.now()).memberId(1L).labelId(1L).build();
          LocalDateTime now = LocalDateTime.now();

          todo.markVerified(42L, now);

          assertThat(todo.getVerificationId()).isEqualTo(42L);
          assertThat(todo.getVerifiedAt()).isEqualTo(now);
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.domain.todo.domain.TodoTest"`
  Expected: FAIL — compile error, `markVerified`/`getVerificationId`/`getVerifiedAt` don't exist yet.

- [ ] **Step 3: Implement in `Todo`**

  In `src/main/java/hongik/Todoing/domain/todo/domain/Todo.java`, replace:

  ```java
  @Column(name = "verification_id", nullable = true)
  private Long verification_id;
  ```

  with:

  ```java
  @Column(name = "verification_id")
  private Long verificationId;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;
  ```

  and add the import `java.time.LocalDateTime`, and add this method next to `updateComplete`/`updateTodo`:

  ```java
  public void markVerified(Long verificationId, LocalDateTime at) {
      this.verificationId = verificationId;
      this.verifiedAt = at;
  }
  ```

  Also add the FK index while touching this class's `@Table`:

  ```java
  @Table(name = "todo", indexes = {
          @Index(name = "idx_todo_user_id", columnList = "user_id"),
          @Index(name = "idx_todo_label_id", columnList = "label_id")
  })
  ```

  (This folds in Task 8's indexes for `todo` — see Task 8's note.)

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.domain.todo.domain.TodoTest"`
  Expected: PASS

- [ ] **Step 5: Write the failing `VerificationService` test**

  There is no existing test for this service. Create `src/test/java/hongik/Todoing/domain/verification/service/VerificationServiceTest.java`:

  ```java
  package hongik.Todoing.domain.verification.service;

  import hongik.Todoing.domain.label.domain.Label;
  import hongik.Todoing.domain.label.domain.LabelType;
  import hongik.Todoing.domain.label.repository.LabelRepository;
  import hongik.Todoing.domain.member.domain.Member;
  import hongik.Todoing.domain.order.adaptor.PassAdaptor;
  import hongik.Todoing.domain.order.validator.PassValidator;
  import hongik.Todoing.domain.todo.domain.Todo;
  import hongik.Todoing.domain.todo.repository.TodoRepository;
  import hongik.Todoing.domain.verification.Adaptor.VerificationAdaptor;
  import hongik.Todoing.domain.verification.repository.VerificationUsageRepository;
  import hongik.Todoing.domain.verification.validator.VerificationValidator;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;

  import java.time.LocalDate;
  import java.util.List;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.when;

  @ExtendWith(MockitoExtension.class)
  class VerificationServiceTest {

      @Mock private VerificationAdaptor verificationAdaptor;
      @Mock private VerificationValidator verificationValidator;
      @Mock private VisionService visionService;
      @Mock private PassAdaptor passAdaptor;
      @Mock private TodoRepository todoRepository;
      @Mock private LabelRepository labelRepository;
      @Mock private PassValidator passValidator;
      @Mock private VerificationUsageRepository verificationUsageRepository;

      private VerificationService verificationService;
      private Todo todo;
      private Member member;

      @BeforeEach
      void setUp() {
          verificationService = new VerificationService(
                  verificationAdaptor, verificationValidator, visionService, passAdaptor,
                  todoRepository, labelRepository, passValidator, verificationUsageRepository
          );

          member = Member.builder().id(1L).nickname("tester").email("t@t.com").role("ROLE_USER").build();
          todo = Todo.builder().todoId(10L).content("run").todoDate(LocalDate.now()).memberId(1L).labelId(5L).build();

          when(todoRepository.findByTodoId(10L)).thenReturn(Optional.of(todo));
          when(labelRepository.findById(5L)).thenReturn(Optional.of(Label.builder().labelId(5L).labelName(LabelType.EXERCISE).build()));
          when(passAdaptor.findByUserId(1L)).thenReturn(List.of());
      }

      @Test
      void successfulTextVerificationLinksVerificationToTodo() {
          when(verificationAdaptor.save(any())).thenAnswer(invocation -> {
              var verification = invocation.getArgument(0, hongik.Todoing.domain.verification.domain.Verification.class);
              return verification.toBuilder().verificationId(99L).build();
          });

          verificationService.verifyTodoText(member, 10L, "run 5k done");

          assertThat(todo.getVerificationId()).isEqualTo(99L);
          assertThat(todo.getVerifiedAt()).isNotNull();
      }
  }
  ```

  This needs `Verification` to have a Lombok `toBuilder = true` so the mocked `save()` can return a copy with an ID — add that in this step too:

  In `src/main/java/hongik/Todoing/domain/verification/domain/Verification.java` (from Task 5), change:
  ```java
  @Builder
  ```
  to:
  ```java
  @Builder(toBuilder = true)
  ```

- [ ] **Step 6: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.domain.verification.service.VerificationServiceTest"`
  Expected: FAIL — `todo.getVerificationId()` is `null` (the service never calls `markVerified`).

- [ ] **Step 7: Wire `markVerified` into `VerificationService`**

  In `src/main/java/hongik/Todoing/domain/verification/service/VerificationService.java`, in `processVerification`, change:

  ```java
  // 6) Todo 완료 처리
  if (success) {
      todo.updateComplete(true);
  }
  ```

  to:

  ```java
  // 6) Todo 완료 처리 + 인증 연결
  if (success) {
      todo.updateComplete(true);
      todo.markVerified(verification.getVerificationId(), java.time.LocalDateTime.now());
  }
  ```

- [ ] **Step 8: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.domain.verification.service.VerificationServiceTest"`
  Expected: PASS

- [ ] **Step 9: Full test run**

  Run: `./gradlew test`
  Expected: PASS (all)

- [ ] **Step 10: Commit**

  ```bash
  git add -A
  git commit -m "feat: link Todo to its Verification on successful verification, add verified_at"
  ```

---

### Task 7: `Friend` — prevent duplicate rows for the same pair

`FriendService.addFriend` already checks `existsByMemberAndFriend` before inserting, but nothing stops two concurrent requests from racing past that check — a DB-level constraint is the actual guarantee.

**Files:**
- Modify: `src/main/java/hongik/Todoing/domain/friend/domain/Friend.java`
- Test: `src/test/java/hongik/Todoing/domain/friend/domain/FriendConstraintTest.java`

**Interfaces:** none new — this is a constraint-only change.

- [ ] **Step 1: Write the failing test**

  Create `src/test/java/hongik/Todoing/domain/friend/domain/FriendConstraintTest.java`:

  ```java
  package hongik.Todoing.domain.friend.domain;

  import hongik.Todoing.domain.member.domain.Member;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
  import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
  import org.springframework.dao.DataIntegrityViolationException;

  import static org.assertj.core.api.Assertions.assertThatThrownBy;

  @DataJpaTest
  class FriendConstraintTest {

      @Autowired
      private TestEntityManager entityManager;

      @Test
      void duplicateFriendPairIsRejectedByTheDatabase() {
          Member me = entityManager.persistFlushFind(Member.builder().nickname("me").email("me@t.com").role("ROLE_USER").build());
          Member other = entityManager.persistFlushFind(Member.builder().nickname("other").email("other@t.com").role("ROLE_USER").build());

          entityManager.persistFlushFind(Friend.of(me, other));

          assertThatThrownBy(() -> {
              entityManager.persistFlushFind(Friend.of(me, other));
          }).isInstanceOf(DataIntegrityViolationException.class);
      }
  }
  ```

- [ ] **Step 2: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.domain.friend.domain.FriendConstraintTest"`
  Expected: FAIL — the second insert succeeds today (no constraint), so the `assertThatThrownBy` fails.

- [ ] **Step 3: Implement**

  In `src/main/java/hongik/Todoing/domain/friend/domain/Friend.java`, change:

  ```java
  @Table(name = "friend")
  ```

  to:

  ```java
  @Table(
          name = "friend",
          uniqueConstraints = @UniqueConstraint(name = "uk_friend_pair", columnNames = {"user_id", "friend_id"})
  )
  ```

- [ ] **Step 4: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.domain.friend.domain.FriendConstraintTest"`
  Expected: PASS

- [ ] **Step 5: Commit**

  ```bash
  git add src/main/java/hongik/Todoing/domain/friend/domain/Friend.java src/test/java/hongik/Todoing/domain/friend/domain/FriendConstraintTest.java
  git commit -m "feat: add unique constraint on (user_id, friend_id) to prevent duplicate friend rows"
  ```

---

### Task 8: Remaining FK indexes (`PromptInput`, `VerificationUsage`, `Order`, `Pass`)

`Todo` and `Verification`'s indexes were already added in Tasks 5–6 (folded in while those files were open — no reason to open them twice). This task covers the rest. `orders`/`pass` get an index but no other change — per the Global Constraints, no new payment features.

**Files:**
- Modify: `src/main/java/hongik/Todoing/domain/prompt/domain/PromptInput.java`
- Modify: `src/main/java/hongik/Todoing/domain/verification/domain/VerificationUsage.java`
- Modify: `src/main/java/hongik/Todoing/domain/order/domain/order/Order.java`
- Modify: `src/main/java/hongik/Todoing/domain/order/domain/pass/Pass.java`

**Interfaces:** none new — annotation-only, no behavior to unit test. (Index presence is verified by the app starting up cleanly against a real schema, not a unit test — Hibernate would fail fast at boot on a malformed `@Index` on a nonexistent column.)

- [ ] **Step 1: `PromptInput`**

  In `src/main/java/hongik/Todoing/domain/prompt/domain/PromptInput.java`, add above the class (currently has no `@Table` at all):

  ```java
  @Table(indexes = @Index(name = "idx_prompt_input_user_id", columnList = "user_id"))
  ```

- [ ] **Step 2: `VerificationUsage`**

  In `src/main/java/hongik/Todoing/domain/verification/domain/VerificationUsage.java`, add above the class:

  ```java
  @Table(indexes = @Index(name = "idx_verification_usage_user_id", columnList = "user_id"))
  ```

- [ ] **Step 3: `Order`**

  In `src/main/java/hongik/Todoing/domain/order/domain/order/Order.java`, change:

  ```java
  @Table(name = "orders")
  ```

  to:

  ```java
  @Table(name = "orders", indexes = @Index(name = "idx_orders_user_id", columnList = "user_id"))
  ```

- [ ] **Step 4: `Pass`**

  In `src/main/java/hongik/Todoing/domain/order/domain/pass/Pass.java`, add above the class (currently has no `@Table` at all):

  ```java
  @Table(indexes = @Index(name = "idx_pass_user_id", columnList = "user_id"))
  ```

- [ ] **Step 5: Compile check**

  Run: `./gradlew compileJava`
  Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

  ```bash
  git add src/main/java/hongik/Todoing/domain/prompt/domain/PromptInput.java src/main/java/hongik/Todoing/domain/verification/domain/VerificationUsage.java src/main/java/hongik/Todoing/domain/order/domain/order/Order.java src/main/java/hongik/Todoing/domain/order/domain/pass/Pass.java
  git commit -m "perf: add missing indexes on FK columns (prompt_input, verification_usage, orders, pass)"
  ```

---

### Task 9: Friend chat feature (`FriendMessage`)

The new feature the user asked for. Reuses the existing, already-unique `Friend` row as the "room" — no separate chat-room table needed. `friendId` in the URL means the *other member's* id, matching the existing `/api/friends/{friendId}/...` convention (`FriendController.deleteFriend`/`blockFriend`/`getFriendTodos` all take the other member's id, not the `Friend` row's id).

**Files:**
- Create: `src/main/java/hongik/Todoing/domain/friend/domain/FriendMessage.java`
- Create: `src/main/java/hongik/Todoing/domain/friend/repository/FriendMessageRepository.java`
- Create: `src/main/java/hongik/Todoing/domain/friend/dto/FriendMessageRequestDTO.java`
- Create: `src/main/java/hongik/Todoing/domain/friend/dto/FriendMessageResponseDTO.java`
- Create: `src/main/java/hongik/Todoing/domain/friend/service/FriendChatService.java`
- Create: `src/main/java/hongik/Todoing/domain/friend/controller/FriendChatController.java`
- Modify: `src/main/java/hongik/Todoing/domain/friend/repository/FriendRepository.java`
- Modify: `src/main/java/hongik/Todoing/global/apiPayload/code/status/ErrorStatus.java`
- Test: `src/test/java/hongik/Todoing/domain/friend/service/FriendChatServiceTest.java`

**Interfaces:**
- Consumes: `Friend` (existing), `FriendRepository.findRelationshipBetween(Member, Member): Optional<Friend>` (new, this task), `MemberRepository.findById` (existing).
- Produces: `FriendChatService.sendMessage(Member me, Long friendId, String content): FriendMessageResponseDTO`, `FriendChatService.getMessages(Member me, Long friendId): List<FriendMessageResponseDTO>`.

- [ ] **Step 1: Add the bidirectional lookup to `FriendRepository`**

  `findByMemberAndFriend(me, target)` only matches the direction the row was created in — if A added B, B has no row to find "from B's side." Chat needs to work regardless of who added whom. In `src/main/java/hongik/Todoing/domain/friend/repository/FriendRepository.java`:

  ```java
  package hongik.Todoing.domain.friend.repository;

  import hongik.Todoing.domain.friend.domain.Friend;
  import hongik.Todoing.domain.member.domain.Member;
  import org.springframework.data.jpa.repository.JpaRepository;
  import org.springframework.data.jpa.repository.Query;
  import org.springframework.data.repository.query.Param;

  import java.util.List;
  import java.util.Optional;

  public interface FriendRepository extends JpaRepository<Friend, Long> {
      boolean existsByMemberAndFriend(Member member, Member friend);
      Friend findByMemberAndFriend (Member member, Member friend);
      List<Friend> findAllByMember(Member member);

      @Query("""
          SELECT f FROM Friend f
          WHERE (f.member = :a AND f.friend = :b)
             OR (f.member = :b AND f.friend = :a)
      """)
      Optional<Friend> findRelationshipBetween(@Param("a") Member a, @Param("b") Member b);
  }
  ```

- [ ] **Step 2: Add error codes**

  In `src/main/java/hongik/Todoing/global/apiPayload/code/status/ErrorStatus.java`, add next to `FRIEND_REQUEST_IS_NULL`:

  ```java
      FRIEND_MESSAGE_IS_NULL(HttpStatus.BAD_REQUEST, "400", "메시지 내용이 비어 있습니다."),
  ```

- [ ] **Step 3: Write the failing service test**

  Create `src/test/java/hongik/Todoing/domain/friend/service/FriendChatServiceTest.java`:

  ```java
  package hongik.Todoing.domain.friend.service;

  import hongik.Todoing.domain.friend.domain.Friend;
  import hongik.Todoing.domain.friend.domain.FriendMessage;
  import hongik.Todoing.domain.friend.domain.FriendStatus;
  import hongik.Todoing.domain.friend.dto.FriendMessageResponseDTO;
  import hongik.Todoing.domain.friend.repository.FriendMessageRepository;
  import hongik.Todoing.domain.friend.repository.FriendRepository;
  import hongik.Todoing.domain.member.domain.Member;
  import hongik.Todoing.domain.member.repository.MemberRepository;
  import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
  import hongik.Todoing.global.apiPayload.exception.GeneralException;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;

  import java.util.List;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.Mockito.when;

  @ExtendWith(MockitoExtension.class)
  class FriendChatServiceTest {

      @Mock private MemberRepository memberRepository;
      @Mock private FriendRepository friendRepository;
      @Mock private FriendMessageRepository friendMessageRepository;

      private FriendChatService friendChatService;
      private Member me;
      private Member other;

      @BeforeEach
      void setUp() {
          friendChatService = new FriendChatService(memberRepository, friendRepository, friendMessageRepository);
          me = Member.builder().id(1L).nickname("me").email("me@t.com").role("ROLE_USER").build();
          other = Member.builder().id(2L).nickname("other").email("other@t.com").role("ROLE_USER").build();
          when(memberRepository.findById(2L)).thenReturn(Optional.of(other));
      }

      @Test
      void sendMessagePersistsAndReturnsDto() {
          Friend friend = Friend.of(me, other);
          when(friendRepository.findRelationshipBetween(me, other)).thenReturn(Optional.of(friend));
          when(friendMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

          FriendMessageResponseDTO response = friendChatService.sendMessage(me, 2L, "hello");

          assertThat(response.senderId()).isEqualTo(1L);
          assertThat(response.message()).isEqualTo("hello");
          assertThat(response.isRead()).isFalse();
      }

      @Test
      void sendMessageToBlockedFriendThrows() {
          Friend friend = Friend.of(me, other);
          friend.updateStatus(FriendStatus.BLOCKED);
          when(friendRepository.findRelationshipBetween(me, other)).thenReturn(Optional.of(friend));

          assertThatThrownBy(() -> friendChatService.sendMessage(me, 2L, "hello"))
                  .isInstanceOf(GeneralException.class)
                  .hasFieldOrPropertyWithValue("code", ErrorStatus.FRIEND_BLOCKED);
      }

      @Test
      void getMessagesReturnsThreadInOrder() {
          Friend friend = Friend.of(me, other);
          when(friendRepository.findRelationshipBetween(me, other)).thenReturn(Optional.of(friend));
          FriendMessage first = FriendMessage.write(friend, me, "hi");
          FriendMessage second = FriendMessage.write(friend, other, "hey");
          when(friendMessageRepository.findByFriendOrderByCreatedAtAsc(friend)).thenReturn(List.of(first, second));

          List<FriendMessageResponseDTO> messages = friendChatService.getMessages(me, 2L);

          assertThat(messages).hasSize(2);
          assertThat(messages.get(0).message()).isEqualTo("hi");
          assertThat(messages.get(1).message()).isEqualTo("hey");
      }
  }
  ```

  This asserts on `GeneralException.code` (the `BaseErrorCode` field exposed by Lombok's `@Getter` as `getCode()`), confirmed against `GeneralException.java` — not a guess.

- [ ] **Step 4: Run test to verify it fails**

  Run: `./gradlew test --tests "hongik.Todoing.domain.friend.service.FriendChatServiceTest"`
  Expected: FAIL — compile error, `FriendMessage`/`FriendMessageRepository`/`FriendChatService`/`FriendMessageResponseDTO` don't exist yet.

- [ ] **Step 5: Create `FriendMessage`**

  ```java
  package hongik.Todoing.domain.friend.domain;

  import hongik.Todoing.domain.member.domain.Member;
  import hongik.Todoing.global.common.BaseEntity;
  import jakarta.persistence.*;
  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  @Entity
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Table(
          name = "friend_message",
          indexes = @Index(name = "idx_friend_message_friend_created", columnList = "friend_id, created_at")
  )
  public class FriendMessage extends BaseEntity {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long messageId;

      @ManyToOne
      @JoinColumn(name = "friend_id", nullable = false)
      private Friend friend;

      @ManyToOne
      @JoinColumn(name = "sender_id", nullable = false)
      private Member sender;

      @Column(length = 500, nullable = false)
      private String message;

      @Column(name = "is_read", nullable = false)
      private boolean isRead;

      public static FriendMessage write(Friend friend, Member sender, String message) {
          return FriendMessage.builder()
                  .friend(friend)
                  .sender(sender)
                  .message(message)
                  .isRead(false)
                  .build();
      }

      public void markRead() {
          this.isRead = true;
      }
  }
  ```

- [ ] **Step 6: Create `FriendMessageRepository`**

  ```java
  package hongik.Todoing.domain.friend.repository;

  import hongik.Todoing.domain.friend.domain.Friend;
  import hongik.Todoing.domain.friend.domain.FriendMessage;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.util.List;

  public interface FriendMessageRepository extends JpaRepository<FriendMessage, Long> {
      List<FriendMessage> findByFriendOrderByCreatedAtAsc(Friend friend);
  }
  ```

- [ ] **Step 7: Create the DTOs**

  `src/main/java/hongik/Todoing/domain/friend/dto/FriendMessageRequestDTO.java`:
  ```java
  package hongik.Todoing.domain.friend.dto;

  import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
  import hongik.Todoing.global.apiPayload.exception.GeneralException;

  public record FriendMessageRequestDTO(String message) {
      public FriendMessageRequestDTO {
          if (message == null || message.isBlank()) {
              throw new GeneralException(ErrorStatus.FRIEND_MESSAGE_IS_NULL);
          }
      }
  }
  ```

  `src/main/java/hongik/Todoing/domain/friend/dto/FriendMessageResponseDTO.java`:
  ```java
  package hongik.Todoing.domain.friend.dto;

  import hongik.Todoing.domain.friend.domain.FriendMessage;

  import java.time.LocalDateTime;

  public record FriendMessageResponseDTO(
          Long messageId,
          Long senderId,
          String message,
          boolean isRead,
          LocalDateTime createdAt
  ) {
      public static FriendMessageResponseDTO from(FriendMessage entity) {
          return new FriendMessageResponseDTO(
                  entity.getMessageId(),
                  entity.getSender().getId(),
                  entity.getMessage(),
                  entity.isRead(),
                  entity.getCreatedAt()
          );
      }
  }
  ```

- [ ] **Step 8: Create `FriendChatService`**

  ```java
  package hongik.Todoing.domain.friend.service;

  import hongik.Todoing.domain.friend.domain.Friend;
  import hongik.Todoing.domain.friend.domain.FriendMessage;
  import hongik.Todoing.domain.friend.domain.FriendStatus;
  import hongik.Todoing.domain.friend.dto.FriendMessageResponseDTO;
  import hongik.Todoing.domain.friend.repository.FriendMessageRepository;
  import hongik.Todoing.domain.friend.repository.FriendRepository;
  import hongik.Todoing.domain.member.domain.Member;
  import hongik.Todoing.domain.member.repository.MemberRepository;
  import hongik.Todoing.global.apiPayload.code.status.ErrorStatus;
  import hongik.Todoing.global.apiPayload.exception.GeneralException;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.util.List;

  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  public class FriendChatService {

      private final MemberRepository memberRepository;
      private final FriendRepository friendRepository;
      private final FriendMessageRepository friendMessageRepository;

      @Transactional
      public FriendMessageResponseDTO sendMessage(Member me, Long friendId, String content) {
          Friend friend = findOpenRelationship(me, friendId);

          FriendMessage saved = friendMessageRepository.save(FriendMessage.write(friend, me, content));
          return FriendMessageResponseDTO.from(saved);
      }

      public List<FriendMessageResponseDTO> getMessages(Member me, Long friendId) {
          Friend friend = findOpenRelationship(me, friendId);

          return friendMessageRepository.findByFriendOrderByCreatedAtAsc(friend).stream()
                  .map(FriendMessageResponseDTO::from)
                  .toList();
      }

      private Friend findOpenRelationship(Member me, Long friendId) {
          Member other = memberRepository.findById(friendId)
                  .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

          Friend friend = friendRepository.findRelationshipBetween(me, other)
                  .orElseThrow(() -> new GeneralException(ErrorStatus.FRIEND_NOT_FOUND));

          if (friend.getStatus() == FriendStatus.BLOCKED) {
              throw new GeneralException(ErrorStatus.FRIEND_BLOCKED);
          }
          return friend;
      }
  }
  ```

- [ ] **Step 9: Run test to verify it passes**

  Run: `./gradlew test --tests "hongik.Todoing.domain.friend.service.FriendChatServiceTest"`
  Expected: PASS (all 3 cases)

- [ ] **Step 10: Create the controller**

  ```java
  package hongik.Todoing.domain.friend.controller;

  import hongik.Todoing.domain.auth.util.PrincipalDetails;
  import hongik.Todoing.domain.friend.dto.FriendMessageRequestDTO;
  import hongik.Todoing.domain.friend.dto.FriendMessageResponseDTO;
  import hongik.Todoing.domain.friend.service.FriendChatService;
  import hongik.Todoing.global.apiPayload.ApiResponse;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.security.SecurityRequirement;
  import lombok.RequiredArgsConstructor;
  import org.springframework.security.core.annotation.AuthenticationPrincipal;
  import org.springframework.web.bind.annotation.*;

  import java.util.List;

  @SecurityRequirement(name = "JWT")
  @RestController
  @RequestMapping("/api/friends/{friendId}/messages")
  @RequiredArgsConstructor
  public class FriendChatController {

      private final FriendChatService friendChatService;

      @Operation(summary = "친구에게 메시지를 보냅니다.")
      @PostMapping
      public ApiResponse<FriendMessageResponseDTO> sendMessage(
              @AuthenticationPrincipal PrincipalDetails principal,
              @PathVariable Long friendId,
              @RequestBody FriendMessageRequestDTO request
      ) {
          return ApiResponse.onSuccess(
                  friendChatService.sendMessage(principal.getMember(), friendId, request.message())
          );
      }

      @Operation(summary = "친구와의 채팅 내역을 조회합니다.")
      @GetMapping
      public ApiResponse<List<FriendMessageResponseDTO>> getMessages(
              @AuthenticationPrincipal PrincipalDetails principal,
              @PathVariable Long friendId
      ) {
          return ApiResponse.onSuccess(friendChatService.getMessages(principal.getMember(), friendId));
      }
  }
  ```

- [ ] **Step 11: Full compile + test run**

  Run: `./gradlew build`
  Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 12: Commit**

  ```bash
  git add -A
  git commit -m "feat: add friend chat (FriendMessage entity, repository, service, controller)"
  ```

---

## Self-Review Notes (for whoever executes this)

- **Spec coverage:** `user` (Task 4), `todo` (Task 6 + indexes), `label` (no change — reference data, matches spec), `ai_chat` (Task 3), `friend` (Tasks 1, 7), `friend_message` (Task 9), `prompt_input` (Task 8), `verification` (Task 5), `verification_usage` (Task 8), `orders`/`pass` (Task 8, index only — payment is pending removal per the user, no new logic). Every table in `v2-erd.md` has a task.
- **Sequencing matters:** Task 2 must land before Tasks 3, 5, 6, 8, 9 (they all either extend `BaseEntity` or rely on `updated_at` existing). Task 1 must land before anyone reads the spec to implement Task 7 (otherwise they'd implement the wrong `FriendStatus`).
- **Payment domain:** deliberately minimal — one index each on `Order`/`Pass` (Task 8), nothing else. Don't expand scope there.
