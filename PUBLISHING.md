# Publishing Guide

This project uses the [Vanniktech Maven Publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) to deploy artifacts to Maven Central.

---

## Publishing via CI (Recommended)

The recommended way to publish is via the GitHub Actions workflow. This ensures consistent, auditable releases.

> **Note:** Only the repository owner can trigger the release workflow.

### One-time Setup

Add these secrets to your repository (Settings → Secrets and variables → Actions):

| Secret | Description |
|--------|-------------|
| `MAVEN_CENTRAL_USERNAME` | Maven Central Portal username |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central Portal token |
| `SIGNING_KEY` | GPG private key (ASCII-armored, base64 encoded) |
| `SIGNING_KEY_PASSWORD` | GPG key passphrase |

To generate `SIGNING_KEY`:
```bash
# Linux
gpg --armor --export-secret-keys YOUR_KEY_ID | base64 -w 0

# macOS
gpg --armor --export-secret-keys YOUR_KEY_ID | base64 | tr -d '\n'
```

### Release Steps

1. Go to **Actions** → **Release** workflow
2. Click **Run workflow**
3. Enter the version (e.g., `2.0.0` or `2.0.0-SNAPSHOT`)
4. Optionally check **Dry run** to test without publishing
5. Click **Run workflow**

The workflow will:
- Run all checks (tests, lint, detekt, spotless)
- Publish to Maven Central
- Create a git tag and GitHub Release (for non-SNAPSHOT versions)
- Auto-generate release notes from merged PRs

### SNAPSHOT vs Release

| Version | Example | Behavior |
|---------|---------|----------|
| SNAPSHOT | `2.0.0-SNAPSHOT` | Publishes to snapshots repo, no tag/release created |
| Release | `2.0.0`, `2.0.0-beta01` | Publishes and closes staging, creates tag and GitHub Release |

---

## Publishing Locally

Use local publishing only when needed (e.g., testing the publish process, debugging issues).

### Prerequisites

Add credentials to `~/.gradle/gradle.properties` (not the project's):

```properties
mavenCentralUsername=YOUR_MAVEN_CENTRAL_USERNAME
mavenCentralPassword=YOUR_MAVEN_CENTRAL_PASSWORD

# File-based signing (recommended for local)
signing.keyId=LAST_8_CHARS_OF_KEY_ID
signing.password=YOUR_KEY_PASSPHRASE
signing.secretKeyRingFile=~/.gradle/secring.gpg
```

To export your GPG key:
```bash
# Find your key ID
gpg --list-secret-keys --keyid-format SHORT

# Export to file
gpg --export-secret-keys YOUR_KEY_ID > ~/.gradle/secring.gpg
```

### Local Release Steps

1. **Run checks**
   ```bash
   ./gradlew clean check
   ```

2. **Test locally (optional)**
   ```bash
   ./gradlew publishToMavenLocal
   ```
   Check `~/.m2/repository/com/ms-square/...` for generated artifacts.

   > **Note:** This does not require Maven Central credentials or GPG signing.

3. **Publish to Maven Central**
   ```bash
   ./gradlew publishAndReleaseToMavenCentral -PVERSION_NAME=2.0.0
   ```
   > **Note:** The version override keeps `gradle.properties` unchanged, matching how CI handles versioning.

4. **Create tag and GitHub Release manually**
   ```bash
   git tag -a v2.0.0 -m "Release 2.0.0"
   git push origin v2.0.0
   ```
   Then create the GitHub Release from the repository's Releases page.

---

## Verify Release

After publishing, verify artifacts appear on Maven Central:
- Search is typically available within **15-30 minutes**
- Full CDN propagation may take **up to 4 hours**

Artifact URLs:
- <https://central.sonatype.com/artifact/com.ms-square/debugoverlay>
- <https://central.sonatype.com/artifact/com.ms-square/debugoverlay-core>
- <https://central.sonatype.com/artifact/com.ms-square/debugoverlay-extension-okhttp>
- <https://central.sonatype.com/artifact/com.ms-square/debugoverlay-extension-timber>
