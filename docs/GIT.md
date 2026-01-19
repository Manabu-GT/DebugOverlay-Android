# Git Workflow

## Commit Hygiene

- Do not amend or squash existing user commits unless asked
- Never commit secrets or local config (`local.properties`, keystores)
- Default to ASCII for new files unless non-ASCII is justified

## GitHub CLI

Use `gh` for GitHub operations:

```bash
gh pr create --title "..." --body "..."   # Create PR
gh pr view 123                             # View PR details
gh pr checks                               # Check CI status
gh issue create --title "..." --body "..." # Create issue
gh api repos/{owner}/{repo}/pulls/123/comments  # Read PR comments
```

## Planning (for agents without built-in plan mode)

For non-trivial work, optionally create `tools/ai/plans/PLAN_<TASK_NAME>.md`:
1. Draft proposed steps
2. Wait for maintainer approval
3. Update after each approved step

Note: Claude Code users should use the built-in `EnterPlanMode` instead.
