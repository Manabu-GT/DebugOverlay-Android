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
