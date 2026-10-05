/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

@file:Suppress("UnstableApiUsage")

import extension.setupDependencyInjection
import extension.testCommonDependencies
import org.gradle.kotlin.dsl.withType
import org.sonarqube.gradle.SonarResolverTask

plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.libraries.pushproviders.firebase"

    buildFeatures {
        resValues = true
    }

    buildTypes {
        fun com.android.build.api.dsl.LibraryBuildType.civcomFirebaseConfig(packageName: String, environment: String) {
            val path = System.getenv(environment)
            val data = path?.let { groovy.json.JsonSlurper().parse(file(it)) as Map<*, *> }
            val project = data?.get("project_info") as? Map<*, *>
            val clients = data?.get("client") as? List<*>
            val client = clients?.map { it as Map<*, *> }?.singleOrNull {
                val info = it["client_info"] as? Map<*, *>
                (info?.get("android_client_info") as? Map<*, *>)?.get("package_name") == packageName
            }
            require(data == null || client != null) { "Firebase configuration does not contain CIVCOM package $packageName" }
            require(data == null || project?.get("project_id") == "civcom-soia-kgpsp") { "Firebase configuration must belong to the CIVCOM project" }
            val info = client?.get("client_info") as? Map<*, *>
            val keys = client?.get("api_key") as? List<*>
            val apiKey = (keys?.firstOrNull() as? Map<*, *>)?.get("current_key") as? String
            if (data != null) {
                require(
                    !apiKey.isNullOrBlank() && (project?.get("project_number") as? String)?.isNotBlank() == true &&
                    (project?.get("project_id") as? String)?.isNotBlank() == true &&
                    (info?.get("mobilesdk_app_id") as? String)?.isNotBlank() == true
                ) { "Incomplete CIVCOM Firebase configuration" }
            }
            resValue("string", "google_app_id", info?.get("mobilesdk_app_id") as? String ?: "")
            resValue("string", "google_api_key", apiKey ?: "")
            resValue("string", "gcm_defaultSenderId", project?.get("project_number") as? String ?: "")
            resValue("string", "project_id", project?.get("project_id") as? String ?: "")
        }
        getByName("debug") { civcomFirebaseConfig("pl.gov.psp.civcom.debug", "CIVCOM_FIREBASE_DEV_JSON") }
        getByName("release") {
            civcomFirebaseConfig("pl.gov.psp.civcom", "CIVCOM_FIREBASE_PROD_JSON")
            consumerProguardFiles("consumer-rules.keep")
        }
        register("nightly") {
            civcomFirebaseConfig("pl.gov.psp.civcom.nightly", "CIVCOM_FIREBASE_DEV_JSON")
            consumerProguardFiles("consumer-rules.keep")
            matchingFallbacks += listOf("release")
        }
    }
}

// Configure the SonarQube plugin to wait for the resource generation tasks to complete before running the analysis.
tasks.withType<SonarResolverTask>().configureEach {
    dependsOn("generateDebugResValues", "generateDebugAndroidTestResValues")
}

setupDependencyInjection()

dependencies {
    implementation(libs.androidx.corektx)
    implementation(projects.features.enterprise.api)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.core)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.push.api)
    implementation(projects.libraries.sessionStorage.api)
    implementation(projects.libraries.uiStrings)
    implementation(projects.libraries.troubleshoot.api)
    implementation(projects.services.toolbox.api)

    implementation(projects.libraries.pushstore.api)
    implementation(projects.libraries.pushproviders.api)

    api(platform(libs.google.firebase.bom))
    api("com.google.firebase:firebase-messaging") {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "com.google.firebase", module = "firebase-analytics")
        exclude(group = "com.google.firebase", module = "firebase-measurement-connector")
    }

    testCommonDependencies(libs)
    testImplementation(libs.kotlinx.collections.immutable)
    testImplementation(projects.features.enterprise.test)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.push.test)
    testImplementation(projects.libraries.pushstore.test)
    testImplementation(projects.libraries.sessionStorage.test)
    testImplementation(projects.libraries.troubleshoot.test)
    testImplementation(projects.services.toolbox.test)
}
