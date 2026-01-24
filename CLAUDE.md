# Claude Code Instructions for DebugOverlay

Use `AGENTS.md` as the canonical project reference for code standards, testing, review protocol, and communication style. This file contains only Claude-specific behavioral instructions.

---

## 1. Instruction Priority

Resolve conflicts in this order:
1. System instructions
2. Developer instructions
3. User instructions
4. Repository docs (`AGENTS.md`, then this file)
5. Task context

Call out conflicts when they appear.

---

## 2. Proactive Subagent Usage

Use specialized subagents to improve quality and efficiency:

| Scenario | Subagent | Model |
|----------|----------|-------|
| Android-specific tasks (ViewModels, Lifecycle, system services) | `mobile-developer` | sonnet |
| Compose UI, Material 3, accessibility, adaptive layouts | `ui-designer` | opus |
| Gradle issues, dependencies, build optimization, CI/CD | `build-engineer` | sonnet |
| Kotlin patterns, coroutines, KMP, Flow, idiomatic review | `kotlin-specialist` | opus |
| Writing or reviewing unit tests | Read `docs/TESTING.md` first | — |
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

## 3. Tool Preferences

| Task | Preferred | Avoid |
|------|-----------|-------|
| Find files by pattern | `Glob` | `find`, `ls` in Bash |
| Search file contents | `Grep` | `grep`, `rg` in Bash |
| Read files | `Read` | `cat`, `head`, `tail` |
| Edit files | `Edit` | `sed`, `awk` |
| Open-ended exploration | `Task` with `Explore` agent | Multiple manual Glob/Grep |
| GitHub operations (PRs, issues, comments) | `gh` CLI | Direct API calls |

Run independent tool calls in parallel. Chain dependent calls sequentially.

---

## 4. Workflow Pattern

Follow **Explore → Plan → Code → Commit**:
1. Read relevant files first — never propose changes to unread code
2. Use extended thinking for complex decisions ("think hard", "ultrathink")
3. Plan before implementing (use `EnterPlanMode` for non-trivial work)
4. Implement incrementally, validate, then commit

---

## 5. Task Tracking

Use `TodoWrite` for any multi-step task:
- Create todos at the start of non-trivial work
- Mark each todo `in_progress` before starting (one at a time)
- Mark `completed` immediately after finishing
- Use checklists for migrations or repetitive fixes

---

## 6. Second Opinion via Codex MCP (Optional)

For complex tasks (planning, architecture decisions, major refactors), check if Codex is available via `/mcp` and consider using it for a second opinion before finalizing.
