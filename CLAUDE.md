# Claude Code Instructions for DebugOverlay

@AGENTS.md

---

## Proactive Subagent Usage

Use specialized subagents to improve quality and efficiency:

| Scenario | Subagent | Model |
|----------|----------|-------|
| Android-specific tasks (ViewModels, Lifecycle, system services) | `mobile-developer` | sonnet |
| Compose UI, Material 3, accessibility, adaptive layouts | `ui-designer` | opus |
| Gradle issues, dependencies, build optimization, CI/CD | `build-engineer` | sonnet |
| Kotlin patterns, coroutines, KMP, Flow, idiomatic review | `kotlin-specialist` | opus |
| Writing or reviewing unit tests | Read @docs/TESTING.md first | — |
| After completing a feature/fix | `code-reviewer` | opus |
| Codebase exploration, "where is X?", "how does Y work?" | `Explore` | haiku |
| Non-trivial implementation planning | `Plan` | inherit |
| Questions about Claude Code itself | `claude-code-guide` | haiku |

**Trigger proactively** — don't wait for the user to ask:
- Writing ViewModel or Lifecycle code → `mobile-developer`
- Writing Compose UI → `ui-designer`
- After implementing a feature → `code-reviewer`
- Kotlin code review → `kotlin-specialist`
- Build sync errors → `build-engineer`

---

## Workflow Pattern

Follow **Explore → Plan → Code → Commit**:
1. Read relevant files first — never propose changes to unread code
2. Use extended thinking for complex decisions ("think hard", "ultrathink")
3. Plan before implementing (use `EnterPlanMode` for non-trivial work)
4. Implement incrementally, validate, then commit

---

## Second Opinion via Codex MCP (Optional)

For complex tasks (planning, architecture decisions, major refactors), check if Codex is available via `/mcp` and consider using it for a second opinion before finalizing.
