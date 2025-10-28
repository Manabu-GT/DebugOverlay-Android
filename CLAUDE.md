# DebugOverlay Guide for Claude

This repository already contains `AGENTS.md`. Claude should use that document as the canonical reference and treat this file as a Claude-specific companion that highlights preferred behaviours and common pitfalls. Follow every directive below unless a higher-priority instruction (system > developer > user) explicitly overrides it.

---

## 1. Core Principles

1. **Pause and align** – read the full request, open tasks, and any referenced files before taking action. When requirements are ambiguous or contradictory, ask for clarification instead of guessing.
2. **Follow instruction priority** – system > developer > user > repo docs (`AGENTS.md`, `CLAUDE.md`) > task context. Resolve conflicts in that order and call them out when they appear.
3. **Stay conservative** – prefer the minimal change that satisfies the request and avoid speculative edits.

---

## 2. Workflow Checklist

1. `git status -sb` to understand the current branch and pending work.
2. Review `AGENTS.md` for baseline policies (testing expectations, coding standards, communication style).
3. For non-trivial work, draft `tools/ai/plans/PLAN_<TASK_NAME>.md` with the proposed steps, wait for maintainer approval before executing, and update the plan after each approved step.
4. Execute changes incrementally. After each significant edit, re-run `git status` to confirm only intended files changed.
5. Keep notes about commands run, decisions taken, and any blockers—these will feed into the final hand-off.

Use tools deliberately:
- Prefer `rg`/`fd`/`ls` for discovery.
- Use `apply_patch` for manual edits; avoid auto-format commands that could produce large style-only diffs unless requested.

---

## 3. Code & Build Expectations (Quick Reference)

- Gradle & AGP versions are defined in `gradle/wrapper/gradle-wrapper.properties` and `gradle/libs.versions.toml`. Update both when tool upgrades are required.
- All modules now use Kotlin DSL (`*.gradle.kts`) and the central version catalog. When modifying dependencies or plugins:
  - Add or update aliases in `gradle/libs.versions.toml`.
  - Reference them via `libs.*` or `libs.plugins.*` in module build scripts.
- Java/Kotlin toolchains default to Java 17; keep `java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }` unless the user requests a change.
- Preserve resource prefixes (`resourcePrefix 'debugoverlay_'`) and do not remove default resource directories when adding extra paths (use `res.srcDir(...)` instead of overwriting `srcDirs`).
- Honour the module list in `AGENTS.md`, avoid creating new modules without approval, and keep all modules on AndroidX APIs (no legacy `android.support`).

---

## 4. Testing Guidance

| Scenario | Minimum Verification |
|----------|----------------------|
| Gradle/build logic or catalog edits | `./gradlew help`; add the smallest assemble task that exercises the affected wiring if artifacts could be impacted |
| Library runtime changes | `./gradlew :debugoverlay:check` (or narrower unit/integration tasks if only part of the module is touched) |
| Extension modules | `./gradlew :debugoverlay-ext-timber:check` / `:debugoverlay-ext-netstats:check` as appropriate |
| Sample app UI/UX | `./gradlew :sample:assembleDebug`, plus manual interaction notes if feasible |
| Documentation-only edits | No build required, but confirm code snippets compile conceptually |

Always document executed commands and outcomes in the final response. When a test cannot be run (e.g., tooling restriction), explain why and call out the residual risk.

---

## 5. Communication Defaults

- Keep updates short, friendly, and actionable.
- Reference files using clickable paths with line numbers when possible (e.g., ``debugoverlay/build.gradle.kts:42`` or ``gradle/libs.versions.toml``).
- Present options as numbered lists when offering alternatives.
- If blocked (permissions, sandbox limits, missing context), report the issue, propose workarounds, and wait for guidance.

---

## 6. Review Mindset (When Asked to Review)

1. Identify issues first, ordered by severity (critical → major → minor), citing file paths and line numbers.
2. Focus on functionality, security/privacy, reliability/performance, maintainability, testing coverage, and consistency with project patterns.
3. Provide concrete remediation suggestions or questions. Mention residual risks or testing gaps even when approving.

---

## 7. Safety & Incident Handling

- Never run destructive Git commands (`git reset --hard`, `git clean -fd`) unless explicitly instructed by the user.
- If unexpected file changes appear (e.g., unrelated modifications, generated files), stop, describe the observation, and request direction.
- Preserve secrets: do not print or store contents of `local.properties`, keystores, or other sensitive files.

---

## 8. Final Response Template

Unless the user specifies otherwise, final messages should include:
1. **Outcome summary** – what was changed, fixed, or investigated.
2. **Validation** – list commands/tests run, including failures or skipped checks with reasons.
3. **Follow-ups/Risks** – mention remaining issues, suggested next steps, or verification the user should perform.

---

Keep both `AGENTS.md` and this guide open during sessions. If new conventions emerge (e.g., tool upgrades, testing matrix changes), update both documents in a coordinated PR. Happy collaborating, Claude!
