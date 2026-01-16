package com.ms.square.debugoverlay

import java.io.OutputStream

/**
 * Contributes custom diagnostic data to bug reports.
 *
 * Implement this interface to add app-specific data (preferences, feature flags,
 * analytics state, etc.) to bug reports. The library handles threading, timeout
 * enforcement, and error isolation automatically.
 *
 * ## Quick Start
 * ```kotlin
 * class UserPreferencesContributor(
 *   private val context: Context
 * ) : BugReportDataContributor {
 *   override val filename = "user_preferences.txt"
 *
 *   override fun writeTo(outputStream: OutputStream) {
 *     PrintWriter(outputStream).use { writer ->
 *       context.getSharedPreferences("user", MODE_PRIVATE)
 *         .all
 *         .forEach { (key, value) -> writer.println("$key = $value") }
 *     }
 *   }
 * }
 *
 * // Registration (in Application.onCreate):
 * DebugOverlay.configure {
 *   copy(
 *     bugReportDataContributors = listOf(
 *       UserPreferencesContributor(applicationContext)
 *     )
 *   )
 * }
 * ```
 *
 * ## Simple Cases
 * For one-off contributors without creating a class, use the factory function:
 * ```kotlin
 * DebugOverlay.configure {
 *   copy(
 *     bugReportDataContributors = listOf(
 *       BugReportDataContributor("build_info.txt") { out ->
 *         out.write("version=${BuildConfig.VERSION_NAME}".toByteArray())
 *       }
 *     )
 *   )
 * }
 * ```
 *
 * ## Timeout & Error Handling
 * - Default timeout: **5 seconds** per contributor
 * - On timeout/exception: contributor is skipped, warning logged, report continues
 * - Partial writes are discarded (file deleted on failure)
 *
 * ## Lifecycle Safety
 * Contributors are retained for the app's lifetime. To avoid memory leaks:
 * - Use Application context, not Activity/Fragment
 * - Inject singletons (Hilt/Koin `@Singleton`)
 * - Fetch fresh data in [writeTo], don't capture state at construction
 *
 * ```kotlin
 * // SAFE:
 * class PrefsContributor(
 *   private val appContext: Context  // Application-scoped
 * ) : BugReportDataContributor { ... }
 *
 * // UNSAFE - leaks Activity!
 * class LeakyContributor(
 *   private val activity: MainActivity
 * ) : BugReportDataContributor { ... }
 * ```
 *
 * ## Privacy & Security
 * Bug reports may be shared externally. Filter sensitive data before writing:
 * ```kotlin
 * prefs.all.entries
 *   .filterNot { it.key.contains("token", ignoreCase = true) }
 *   .filterNot { it.key.contains("password", ignoreCase = true) }
 *   .forEach { (key, value) -> writer.println("$key = $value") }
 * ```
 *
 * @see DebugOverlay.Config.bugReportDataContributors
 */
public interface BugReportDataContributor {
  /**
   * Filename for this data in the bug report ZIP.
   *
   * Must be a simple filename without path separators (e.g., "my_data.json").
   * Automatically prefixed with "custom_" to avoid collisions with built-in files.
   *
   * Recommended extensions: `.txt`, `.json`, `.xml`, `.log`
   *
   * Example: `"feature_flags.json"` appears as `"custom_feature_flags.json"` in ZIP
   */
  public val filename: String

  /**
   * Writes diagnostic data to the provided stream.
   *
   * Called on [kotlinx.coroutines.Dispatchers.IO] with a **5-second timeout**.
   * Blocking I/O is safe but must complete within the timeout. Long-running
   * operations (network calls, large database queries) should be avoided.
   *
   * The stream is buffered; no need to wrap in `BufferedOutputStream`.
   *
   * @param outputStream The stream to write data to
   * @throws Exception Any exception is caught and logged; contributor is skipped
   */
  public fun writeTo(outputStream: OutputStream)

  public companion object {
    /**
     * Creates a [BugReportDataContributor] from a lambda.
     * Useful for simple, one-off contributors without creating a class.
     *
     * ```kotlin
     * BugReportDataContributor("env.txt") { out ->
     *   out.write("env=production".toByteArray())
     * }
     * ```
     */
    public inline operator fun invoke(
      filename: String,
      crossinline writer: (OutputStream) -> Unit,
    ): BugReportDataContributor = object : BugReportDataContributor {
      override val filename = filename
      override fun writeTo(outputStream: OutputStream) = writer(outputStream)
    }
  }
}
