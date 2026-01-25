repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
    id("java")
    id("net.codecrete.windows-api") version "0.8.4"
}

group = rootProject.group
version = rootProject.version

tasks.generateWindowsApi {
    basePackage = "tech.tnze.msctf"
    functions = listOf(
        "CoInitializeEx",
        "CoUninitialize",
        "CoCreateInstance",
        "FormatMessageW",
        "LocalFree",
        "SysStringLen",
        "SysFreeString"
    )
    comInterfaces = listOf(
        "ITfSource",
        "ITfThreadMgr",
        "ITfThreadMgrEx",
        "ITfDocumentMgr",
        "ITfUIElementMgr",
        "ITfUIElementSink",
        "ITfContextOwnerCompositionSink",
        "ITfCandidateListUIElement",
        "ITextStoreACP2",
        "ITextStoreACPSink",
    )
    constants = listOf(
        "CLSID_TF_ThreadMgr",
        "TF_TMAE_UIELEMENTENABLEDONLY",
        "E_INVALIDARG",
        "E_NOINTERFACE",
        "E_POINTER",
        "E_FAIL",
        "CONNECT_E_CANNOTCONNECT",
        "CONNECT_E_NOCONNECTION",
        "CONNECT_E_ADVISELIMIT",
        "TS_E_SYNCHRONOUS",
        "TS_E_NOLOCK",
        "TS_E_INVALIDPOS",
        "TS_E_INVALIDPOINT",
        "TS_E_NOLAYOUT",
        "TS_S_ASYNC",
        "TS_LF_SYNC",
        "TS_SD_LOADING",
        "TS_SD_READONLY",
        "TS_SS_DISJOINTSEL",
        "TS_SS_REGIONS",
        "TS_SS_TRANSITORY",
        "TS_SS_NOHIDDENTEXT",
        "TS_SS_TKBAUTOCORRECTENABLE",
        "TS_SS_TKBPREDICTIONENABLE",
        "TF_DEFAULT_SELECTION",
        "TF_TF_IGNOREEND",
    )
    enumerations = listOf(
        "TEXT_STORE_LOCK_FLAGS",
        "TsActiveSelEnd",
        "TsRunType",
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

dependencies {
}
