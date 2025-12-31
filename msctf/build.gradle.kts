plugins {
    id("java")
    id("net.codecrete.windows-api") version "0.8.2"
}

group = rootProject.group
version = rootProject.version

tasks.generateWindowsApi {
    functions = listOf(
        "CoInitializeEx",
        "CoUninitialize",
        "CoCreateInstance",
        "FormatMessageW",
        "LocalFree"
    )
    comInterfaces = listOf(
        "ITfSource",
        "ITfThreadMgr",
        "ITfThreadMgrEx",
        "ITfDocumentMgr",
        "ITfUIElementMgr",
        "ITfUIElementSink",
        "ITextStoreACP",
        "ITfContextOwnerCompositionSink",
    )
    constants = listOf(
        "CLSID_TF_ThreadMgr",
        "TF_TMAE_UIELEMENTENABLEDONLY",
        "E_INVALIDARG",
        "E_NOINTERFACE",
    )
}

val targetJavaVersion = 23

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

repositories {
    mavenCentral()
}

dependencies {
}
