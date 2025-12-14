import java.nio.file.Paths

plugins {
    id("cpp-library")
}

group = "tech.tnze"
version = "1.0-SNAPSHOT"

val javaHome: String = System.getProperty("java.home")
val windowsKits = File("C:/Program Files (x86)/Windows Kits/10/Include")
val latestSdk = windowsKits.listFiles()
    ?.filter { it.isDirectory }
    ?.filter { it.name.startsWith("10.") }
    ?.maxByOrNull { it.name }

library {
    baseName = "msctf-jni"
    linkage = listOf(Linkage.SHARED)

    privateHeaders.setFrom(
        Paths.get(javaHome, "include", "win32"),
        Paths.get(javaHome, "include"),
        latestSdk?.let { Paths.get(it.path, "cppwinrt") },
        project(":msctf").layout.buildDirectory.dir("generated/sources/headers/java/main"),
    )
}

tasks.withType<CppCompile> {
    dependsOn(":msctf:build")
    compilerArgs.add("/std:c++latest")
}
