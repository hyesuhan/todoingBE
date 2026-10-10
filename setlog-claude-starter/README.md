# SETLOG Claude Code starter pack

Copy `CLAUDE.md`, `.claude/` and `docs/` into the root of your SETLOG git repository. This is an AI-guidance and design-document starter, **not yet a runnable Spring Boot application**.

### Suggested first use
1. In Claude Code run `/plan-feature Room 생성 시 Round 1 즉시 OPEN 구현`.
2. Review proposed assumptions / scope.
3. Run `/implement-feature Room 생성 트랜잭션과 통합 테스트` (skill invocation).
4. Run `/review-change`, then `/test-backend` after you add the Gradle project.

Source-of-truth hierarchy: user-approved feature decision > docs/PRD.md & docs/DOMAIN-RULES.md > docs/API-CONTRACT.md > CLAUDE.md > role-specific rules. If disagreement is found, stop and ask rather than silently reconcile.

Agents: `backend-developer`, `code-reviewer`, `database-reviewer`. Use `/agents` to inspect Claude Code agents. Skills live in `.claude/skills/<skill-name>/SKILL.md`; `.claude/commands/` convenience prompts are included but new invocable workflows should prefer skills.

Initial policy assumptions that still need confirmation: weekly schedule timing and first-round length; archive when a member rejoins; catch-up round behavior during downtime; deletion/retention when a user leaves.
