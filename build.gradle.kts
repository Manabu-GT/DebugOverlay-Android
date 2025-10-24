plugins {
    /**
     * Use `apply false` in the top-level build.gradle file to add a Gradle
     * plugin as a build dependency but not apply it to the current (root)
     * project. Don't use `apply false` in sub-projects. For more information,
     * see Applying external plugins with same version to subprojects.
     */
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.dexcount) apply false
}

allprojects {
    // This ensures a group and version are defined for all modules.
    group = project.property("GROUP") as String
    version = project.property("VERSION_NAME") as String
}

// http://www.gradle.org/docs/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html
extra.apply {
    set("compileSdkVersion", 36)
    set("minSdkVersion", 26)
    set("targetSdkVersion", 36)
}
