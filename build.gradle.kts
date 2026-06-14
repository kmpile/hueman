import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// colormath on the build classpath so the codegen computes CIELAB with the EXACT same math the runtime
// query uses — the bundled palette ships pre-converted, so Hueman.default() skips ~32k RGB→LAB conversions.
buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("com.github.ajalt.colormath:colormath:3.6.0") }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    `maven-publish`
}

group = "com.kmpile"
version = "0.1.0"

repositories {
    mavenCentral()
}

// ---------------------------------------------------------------------------------------------
// Embed the color dataset as generated Kotlin (works on every target — no per-platform resource
// loading). The CSV is escaped and split into <64 KB string constants (the JVM constant-pool limit)
// joined at runtime; records use control-char delimiters so names with commas/quotes stay safe.
// ---------------------------------------------------------------------------------------------
val generateColorData by tasks.registering {
    val csv = layout.projectDirectory.file("data/colornames.csv")
    val outDir = layout.buildDirectory.dir("generated/colorData")
    inputs.file(csv)
    outputs.dir(outDir)
    doLast {
        val unit = 1.toChar()   // name / hex field delimiter (U+0001)
        val rs = 2.toChar()     // record delimiter (U+0002)
        val hexRe = Regex("#[0-9a-fA-F]{6}")
        fun esc(s: String) = buildString {
            for (c in s) when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '$' -> append("\\\$")
                unit -> append("\\u0001")
                rs -> append("\\u0002")
                '\n', '\r' -> {}
                else -> append(c)
            }
        }
        val parts = ArrayList<String>()
        val cur = StringBuilder()
        fun flush() { if (cur.isNotEmpty()) { parts.add(esc(cur.toString())); cur.clear() } }
        var count = 0
        csv.asFile.useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val m = hexRe.find(line) ?: return@forEach
                val name = line.substring(0, m.range.first).trimEnd(',', ' ', '"').trim('"')
                if (name.isEmpty()) return@forEach
                val hex = m.value
                val lab = com.github.ajalt.colormath.model.RGB(hex).toLAB()
                // record = name | hex | L | a | b  (fields joined by U+0001), LAB precomputed.
                cur.append(name).append(unit).append(hex).append(unit)
                    .append(lab.l).append(unit).append(lab.a).append(unit).append(lab.b).append(rs)
                count++
                if (cur.length > 40_000) flush()  // raw cap → escaped stays under the 64 KB limit
            }
        }
        flush()
        val out = outDir.get().file("com/kmpile/hueman/ColorData.kt").asFile
        out.parentFile.mkdirs()
        out.writeText(buildString {
            appendLine("package com.kmpile.hueman")
            appendLine()
            appendLine("// GENERATED from data/colornames.csv (meodai/color-names, MIT — see NOTICE). Do not edit.")
            appendLine("// $count colors; record = name|hex|L|a|b (fields joined by U+0001, records by U+0002).")
            appendLine("internal val colorData: String = buildString {")
            parts.indices.forEach { appendLine("    append(P$it)") }
            appendLine("}")
            appendLine()
            parts.forEachIndexed { i, p -> appendLine("private const val P$i = \"$p\"") }
        })
        logger.lifecycle("hueman: generated ColorData.kt with $count colors in ${parts.size} chunks")
    }
}

kotlin {
    explicitApi()
    jvmToolchain(libs.versions.jvmTarget.get().toInt())

    androidLibrary {
        namespace = "com.kmpile.hueman"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    js { browser(); nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser(); nodejs() }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateColorData)
            dependencies {
                api(libs.colormath)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Publish as com.kmpile:hueman. Point at a local clone of the kmpile binaries Maven repo with
// -PbinariesRepoDir=<path> (the koog-box convention); otherwise only mavenLocal is available.
publishing {
    repositories {
        providers.gradleProperty("binariesRepoDir").orNull?.let { dir ->
            maven { name = "binaries"; url = uri(File(dir).resolve("repository")) }
        }
    }
}
