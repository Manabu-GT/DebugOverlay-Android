# DebugOverlay Agents Guide

Welcome! This guide defines how automation and human agents should collaborate in the DebugOverlay-Android repository. Follow it strictly unless the task explicitly overrides a rule.

---

## 1. Mission Checklist

1. Understand the issue or request. If the ticket is vague, pause and ask for clarification.
2. Inspect the current state: run `git status -sb`, scan recent changes, and note branch naming.
3. Make a lightweight plan (≤5 steps) for non-trivial work; update the plan after each completed step.
4. Execute the plan while keeping the tree clean—no reverting user edits and no destructive commands (`git reset --hard`, etc.).
5. Validate locally (build, lint, or targeted tests) whenever changes touch executable code. Capture command, outcome, and failures.
6. Summarise the result: describe the change, list validations, and call out risks or follow-ups.

---

## 2. Code & Build Standards

- **Gradle & AGP**
    - Project tracks tool versions via `gradle/wrapper/gradle-wrapper.properties` (Gradle) and `gradle/libs.versions.toml` (AGP, plugins, dependencies). Keep those files as the single sources of truth; any wrapper or plugin change must remain compatible with JDK 17+.
    - When upgrading dependencies or plugins, update the catalog (`gradle/libs.versions.toml`) and keep repository definitions centralised in `settings.gradle.kts`. Avoid reintroducing deprecated repositories (e.g., JCenter).

- **Module Conventions**
    - Library modules stick to `namespace` declarations and Java 17 compatibility (`compileOptions`).
    - Prefer AndroidX APIs; migrations from legacy `android.support` should include package updates and dependency replacements.

- **Project Modules**
    - `debugoverlay` - Main library module with core overlay functionality
    - `debugoverlay-no-op` - No-op variant for release builds
    - `debugoverlay-ext-timber` - Timber logging extension module
    - `debugoverlay-ext-netstats` - Network statistics extension module
    - `sample` - Demo application showcasing library features

- **Formatting & Static Analysis**
    - Respect existing style (4-space Java/Kotlin, XML indentation).
    - Run relevant formatters (`ktlint`, `spotless`, IDE auto-format) if part of the workflow, but do not introduce sweeping style-only diffs.

- **Resource Handling**
    - Keep public resource prefixes (`resourcePrefix 'debugoverlay_'`).
    - For new assets, ensure they live in the correct variant directory and include mdpi/hdpi etc. when required.

---

## 3. Testing Expectations

| Change Type | Mandatory Checks |
|-------------|------------------|
| Gradle/build logic | `./gradlew help`; run the smallest assemble task that exercises your change when artifact wiring is affected |
| Library runtime code | `./gradlew :debugoverlay:check` (or targeted tests) |
| Extension modules | `./gradlew :debugoverlay-ext-timber:check` or `./gradlew :debugoverlay-ext-netstats:check` |
| Sample app UX/UI | `./gradlew :sample:assembleDebug` plus manual sanity if feasible |
| Documentation only | No build, but ensure links and code snippets compile conceptually |

Document all executed commands in the hand-off message. If a test is skipped, state why and note the risk. For script/config changes (Gradle `.kts`, `libs.versions.toml`, wrapper updates), run a lightweight task such as `./gradlew help` to ensure the configuration still loads.

---

## 4. Git & History Hygiene

- Do not amend or squash existing user commits unless asked.
- Never commit secrets or local config (`local.properties`, keystores).
- When adding files, default to ASCII unless non-ASCII already exists and is justified.

---

## 5. Review Protocol

Use the following structured analysis process when performing code reviews:

**Step 1: Read and Understand**
- Carefully review the changes and understand their purpose
- Identify the scope and impact of modifications
- Note any breaking changes or API modifications

**Step 2: Comprehensive Analysis**
- **Functionality:** Does the code work as intended? Are there edge cases, regressions, or unexpected behavior?
- **Security & Privacy:** Any data leaks, permission issues, unsafe storage, or security vulnerabilities?
- **Reliability:** Could the change crash, ANR, or degrade performance? Memory leaks or resource issues?
- **Maintainability:** Is the code readable, idiomatic, and consistent with project patterns?
- **Testing:** Does the patch include sufficient tests? Are edge cases covered? Manual testing steps documented?
- **Consistency:** Are there inconsistencies with existing code or design patterns?

**Step 3: Prioritize Findings**
- **Critical issues:** Security vulnerabilities, crashes, data loss, breaking changes
- **Major issues:** Performance degradation, maintainability concerns, missing tests
- **Minor issues:** Style inconsistencies, documentation gaps, minor optimizations

**Step 4: Formulate Feedback**
- Provide specific, actionable suggestions with concrete remediation steps
- Reference exact file paths and line numbers (e.g., `debugoverlay/src/main/java/com/ms_square/debugoverlay/DebugOverlay.java:42`)
- Order findings by severity; mention residual risks even if no blocking issues

**Step 5: Document and Communicate**
- Summarize findings in order of severity
- Ensure feedback is comprehensive and constructive
- Note any follow-up actions or additional testing needed

---

## 6. Security Guidelines

- **Data Protection:** Ensure sensitive data is not logged or exposed in debug overlays
- **Permission Handling:** Verify that overlay permissions are properly requested and handled
- **Input Validation:** Validate all user inputs and external data sources
- **Resource Access:** Ensure proper cleanup of resources and prevent memory leaks
- **API Security:** Review any new API endpoints for proper authentication and authorization

---

## 7. Performance Standards

- **Memory Management:** Avoid memory leaks, especially in long-running overlay services
- **CPU Usage:** Minimize background processing and optimize rendering performance
- **Battery Impact:** Consider power consumption for overlay updates and animations
- **Network Efficiency:** Optimize data fetching and caching strategies
- **UI Responsiveness:** Ensure overlay interactions don't block the main thread

---

## 8. Communication Style

- Provide concise, friendly updates. Use numbered lists when offering options.
- Reference files with clickable paths (e.g., ``debugoverlay/build.gradle:15``). Avoid line ranges.
- When unsure, ask before acting. When blocked by tooling/sandbox limits, propose alternatives.

---

## 9. Incident Handling

- If unexpected local changes appear, halt and request guidance.
- On build or test failures, share logs, diagnose briefly, and propose fixes instead of silently retrying.

---

Keep this guide visible during sessions. Deviations must be justified in the task notes. Happy debugging!
