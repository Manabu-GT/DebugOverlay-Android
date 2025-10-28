# Publishing Guide

This project uses the Vanniktech Maven Publish plugin to deploy artifacts to Maven Central. The steps below capture everything you need to do for a new release (e.g., `1.1.4`). Keep your git working tree clean before starting.

---

## Prerequisites

1. **Credentials & signing**
   Provide the following properties either in `~/.gradle/gradle.properties` (local) or as CI environment variables (`ORG_GRADLE_PROJECT_*`):

   ```properties
   mavenCentralUsername=YOUR_MAVEN_CENTRAL_USERNAME
   mavenCentralPassword=YOUR_MAVEN_CENTRAL_PASSWORD
   signing.keyId=YOUR_KEY_ID
   signing.password=YOUR_KEY_PASSPHRASE
   signing.secretKeyRingFile=YOUR_PRIVATE_KEY_FILE
   ```

2. **Version metadata**
   Update `gradle.properties` with the new `VERSION_NAME`, refresh `CHANGELOG.md`, README badges, etc., and commit those changes.

---

## Release Steps

1. **Smoke-test the build**
   ```bash
   ./gradlew clean \
     :debugoverlay:assembleRelease \
     :debugoverlay-no-op:assembleRelease \
     :debugoverlay-ext-timber:assembleRelease \
     :debugoverlay-ext-netstats:assembleRelease
   ./gradlew test
   ```

2. **Optional: inspect the artifacts locally**
   ```bash
   ./gradlew publishToMavenLocal
   ```
   Check `~/.m2/repository/com/ms-square/...` for the generated AARs, POMs, and signatures.

3. **Publish to MavenCentral staging**
   ```bash
   ./gradlew publishAllPublicationsToMavenCentralRepository
   ```
   This uploads the release publications and leaves the staging repository open (automatic release is disabled on purpose).

4. **Close & release the staging repository**
   - Log in to https://central.sonatype.com/publishing.
   - Locate the new staging repository, review the artifacts, then click **Publish**.
     (If something is wrong, drop the staging repository and fix the build before retrying.)

5. **Tag the release and push**
   ```bash
   git tag -a v1.1.4 -m "Release 1.1.4"
   git push origin main v1.1.4
   ```

6. **Create the GitHub release**
   Use https://github.com/Manabu-GT/DebugOverlay-Android/releases to draft a new release referencing the tag and changelog highlights.

7. **Verify Maven Central sync**
   After Sonatype finishes syncing (usually within a couple of hours), confirm the artifact appears at https://central.sonatype.com/artifact/com.ms-square/debugoverlay.

---

Following these steps ensures every release is signed, staged, and published consistently. Update this guide if the plugin configuration or maven central publishing process changes.
