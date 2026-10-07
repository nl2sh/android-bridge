import java.util.Properties

plugins {
    id("com.android.application")
}

val signingProperties = Properties().apply {
    val source = rootProject.file("keystore.properties")
    if (source.isFile) source.inputStream().use { load(it) }
}
val signingValues = listOf("storeFile", "storePassword", "keyAlias", "keyPassword").associateWith { key ->
    val environment = when (key) {
        "storeFile" -> "NL2SH_BRIDGE_KEYSTORE_FILE"
        "storePassword" -> "STORE_PASSWORD"
        "keyAlias" -> "KEY_ALIAS"
        else -> "KEY_PASSWORD"
    }
    providers.environmentVariable(environment).orNull ?: signingProperties.getProperty(key)
}
val hasSigning = signingValues.values.any { !it.isNullOrEmpty() }
if (hasSigning) {
    require(signingValues.values.all { !it.isNullOrEmpty() }) { "Incomplete release signing configuration" }
    require(rootProject.file(requireNotNull(signingValues["storeFile"])).isFile) { "Release keystore does not exist" }
}
val bridgeVersion = providers.gradleProperty("nl2shBridgeVersionName").orElse("0.2.0").get()
require(bridgeVersion.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(-[A-Za-z0-9.-]+)?"))) { "Invalid Bridge version" }
val bridgeVersionCodeText = providers.gradleProperty("nl2shBridgeVersionCode").orNull
val bridgeVersionCode = if (bridgeVersionCodeText == null) 200 else
    requireNotNull(bridgeVersionCodeText.toIntOrNull()) { "Invalid Bridge version code" }
require(bridgeVersionCode in 1..2100000000) { "Invalid Bridge version code" }
val bridgeProtocol = 2
val runtimeAssets = layout.buildDirectory.dir("generated/runtime-assets")
val generateRuntimeMetadata = tasks.register("generateRuntimeMetadata") {
    val metadata = runtimeAssets.map { it.file("nl2sh-runtime.json") }
    inputs.property("version", bridgeVersion)
    inputs.property("protocol", bridgeProtocol)
    outputs.file(metadata)
    doLast {
        val output = metadata.get().asFile
        output.parentFile.mkdirs()
        output.writeText("""{"protocol":$bridgeProtocol,"app_version":"$bridgeVersion","package_name":"com.nl2sh.bridge","min_android_api":26,"features":["ui.tree.v2","ui.tap.semantic.v2","ui.gesture.v1","ui.input.v1","clipboard.paste.v1","ime.commit.v1"]}""" + "\n")
    }
}

android {
    namespace = "com.nl2sh.bridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nl2sh.bridge"
        minSdk = 26
        targetSdk = 35
        versionCode = bridgeVersionCode
        versionName = bridgeVersion
        buildConfigField("int", "BRIDGE_PROTOCOL_VERSION", bridgeProtocol.toString())
    }

    buildFeatures { buildConfig = true }
    sourceSets.getByName("main").assets.srcDir(runtimeAssets.get().asFile)
    signingConfigs {
        if (hasSigning) create("release") {
            storeFile = rootProject.file(requireNotNull(signingValues["storeFile"]))
            storePassword = signingValues["storePassword"]
            keyAlias = signingValues["keyAlias"]
            keyPassword = signingValues["keyPassword"]
        }
    }
    buildTypes { release { if (hasSigning) signingConfig = signingConfigs.getByName("release") } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.named("preBuild").configure { dependsOn(generateRuntimeMetadata) }
