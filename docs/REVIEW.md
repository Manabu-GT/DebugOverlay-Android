# Code Review Protocol

## Step 1: Read and Understand

- Review the changes and understand their purpose
- Identify scope and impact of modifications
- Note any breaking changes or API modifications

**External references:**
- [Android Framework](https://cs.android.com/android)
- [AndroidX Libraries](https://cs.android.com/androidx)
- [Kotlin Standard Library](https://github.com/JetBrains/kotlin)

## Step 2: Comprehensive Analysis

| Dimension | Check For |
|-----------|-----------|
| **Functionality** | Edge cases, regressions, unexpected behavior |
| **Security** | Data leaks, permission issues, unsafe storage |
| **Reliability** | Crashes, ANRs, performance degradation, memory leaks |
| **Maintainability** | Readability, idiomatic code, consistency with project patterns |
| **Testing** | Sufficient tests, edge case coverage |
| **Consistency** | Alignment with existing patterns |

**Compose API guidelines:**
- [Compose API Guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
- [Compose Component Guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md)

## Step 3: Prioritize Findings

| Severity | Examples |
|----------|----------|
| **Critical** | Security vulnerabilities, crashes, data loss, breaking changes |
| **Major** | Performance degradation, maintainability concerns, missing tests |
| **Minor** | Style inconsistencies, documentation gaps |

## Step 4: Formulate Feedback

- Provide specific, actionable suggestions
- Reference exact file paths and line numbers (e.g., `debugoverlay-core/src/.../File.kt:42`)
- Order findings by severity
- Mention residual risks even if no blocking issues

## Step 5: Document

- Summarize findings in order of severity
- Note follow-up actions or additional testing needed
