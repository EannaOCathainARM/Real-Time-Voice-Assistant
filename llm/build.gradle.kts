/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.arm.llm"

    defaultConfig {
        externalNativeBuild {
            cmake {
                targets += "arm-llm-jni"

                //Check if KleidiAI needs to be disabled
                val kleidiAiDisabled = project.findProperty("kleidiAI") == "false"

                //Disable KleidiAI
                if (kleidiAiDisabled) {
                    arguments += "-DGGML_CPU_KLEIDIAI=OFF"
                }
            }
        }

        sourceSets {
            getByName("main") {
                java.srcDir("llm-src/src/java")
            }
        }
    }
}
