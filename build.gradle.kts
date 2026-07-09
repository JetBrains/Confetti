import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("base")
    alias(libs.plugins.composeai.preview) apply false
}

buildscript {
    dependencies {
        classpath("build-logic:build-logic")
    }
}

tasks.register("setupCredentials") {
    val gcpServiceAccountKeyFile = file("backend/datastore/src/jvmMain/resources/gcp_service_account_key.json")
    val firebaseServiceAccountKeyFile = file("backend/service-graphql/src/main/resources/firebase_service_account_key.json")
    val apolloKeyFile = file("backend/service-graphql/src/main/resources/apollo.key")
    doLast {
        fun File.writeEnv(name: String) {
            parentFile.mkdirs()
            writeText(System.getenv(name))
        }
        if (System.getenv("CI")?.isNotEmpty() == true) {
            println("setting up google services...")
            gcpServiceAccountKeyFile.writeEnv("GOOGLE_SERVICES_JSON")
            firebaseServiceAccountKeyFile.writeEnv("FIREBASE_SERVICES_JSON")
            apolloKeyFile.writeEnv("APOLLO_KEY")
        }
    }
}

tasks.register("quickChecks") {
    dependsOn(
        ":backend:service-graphql:build",
        ":backend:service-import:build",
        ":androidApp:assembleDebug",
        ":wearApp:assembleDebug",
        ":wearApp:assembleDebugAndroidTest",
    )
}

allprojects {
    val kotlinLanguageVersionOverride = providers.gradleProperty("kotlin_language_version")
        .map(org.jetbrains.kotlin.gradle.dsl.KotlinVersion::fromVersion)
        .orNull
    val kotlinApiVersionOverride = providers.gradleProperty("kotlin_api_version")
        .map(org.jetbrains.kotlin.gradle.dsl.KotlinVersion::fromVersion)
        .orNull
    val kotlinAdditionalCliOptions = providers.gradleProperty("kotlin_additional_cli_options")
        .map { it.split(" ").filter { option -> option.isNotEmpty() } }
        .orNull

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure(KotlinMultiplatformExtension::class.java) {
            compilerOptions {
                kotlinLanguageVersionOverride?.let {
                    languageVersion.set(it)
                    logger.info("<KUP> ${project.path} : set LV to $it")
                }

                kotlinApiVersionOverride?.let {
                    apiVersion.set(it)
                    logger.info("<KUP> ${project.path} : set APIV to $it")
                }

                kotlinAdditionalCliOptions?.let {
                    freeCompilerArgs.addAll(it)
                    logger.info(
                        "<KUP> ${project.path} : added ${
                            kotlinAdditionalCliOptions.joinToString(
                                " "
                            )
                        }"
                    )
                }

                freeCompilerArgs.add("-Xreport-all-warnings")
                logger.info("<KUP> ${project.path} : added -Xreport-all-warnings")
                // output kotlin.git-searchable names of reported diagnostics
                freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
                logger.info("<KUP> ${project.path} : added -Xrender-internal-diagnostic-names")

                allWarningsAsErrors.set(false)
            }
        }
    }
}