import java.nio.file.Paths

plugins {
    id("cpp-library")
}

group = "tech.tnze"
version = "1.0-SNAPSHOT"

val javaHome: String = System.getProperty("java.home")

library {
    baseName = "msctf-jni"
    linkage = listOf(Linkage.SHARED)

    publicHeaders.setFrom(
        Paths.get(javaHome, "include", "win32"),
        Paths.get(javaHome, "include")
    )

//    source.setFrom("src/main/cpp")
}

//tasks.withType<CppCompile> {
//    compilerArgs.add("/utf-8")
//}
