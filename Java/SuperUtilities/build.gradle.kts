import java.nio.file.Paths
import kotlin.io.path.pathString

/*
External Configuration Properties
=================================
Below values can be overridden when invoking gradle build using property arguments, for example:

./gradlew build -Pversion=2.0

                  group => The group ID
                version => The version
          nuixEngineDir => Overrides value to engine release otherwise pulled from ENV var NUIX_ENGINE_DIR
                tempDir => Used to override temp directory for testing which would otherwise default to dir in localappdata
            testDataDir => Directory tests can load test data they depend on from
    testOutputDirectory => Root directory where tests may write data while running.  Each test run will create a timestamp subdirectory
           nuixUsername => Username used to authenticate with CLS (Cloud License Server).
                           Otherwise, would be pulled from ENV var NUIX_USERNAME
           nuixPassword => Password used to authenticate with CLS (Cloud License Server).
                           Otherwise, would be pulled from ENV var NUIX_PASSWORD
               gpr.user => GitHub username for retrieving engine wrapper package used in tests.
                           Otherwise, would be pulled from ENV var GITHUB_USERNAME
                gpr.key => GitHub classic token for retrieving engine wrapper package used in tests, needs at least "read:packages" permission.
                           Otherwise, would be pulled from ENV var GITHUB_TOKEN
*/

plugins {
    id("java")
}

// So we don't get "unmappable character for encoding windows-1252" errors on a Windows machine
tasks.compileJava.get().options.encoding = "UTF-8"
tasks.compileTestJava.get().options.encoding = "UTF-8"

group = findProperty("group") ?: "com.nuix.innovation"
version = findProperty("version") ?: "1.23.0"

project.extra["nuixEngineDirectory"] = findProperty("nuixEngineDirectory") ?: System.getenv("NUIX_ENGINE_DIR")
if (project.extra["nuixEngineDirectory"].toString().isBlank()) {
    throw InvalidUserDataException("Please populate the environment variable 'NUIX_ENGINE_DIR' with directory containing a Nuix Engine release")
}
System.out.println("Resolved 'nuixEngineDirectory': " + project.extra["nuixEngineDirectory"].toString())

// Determines where Nuix dependencies can be found and where JARs will be copied to for tasks:
// - copyDependencyJarsToEngine
// - copyJarToEngine
// - copyJarsToEngine (test execution invokes this)
project.extra["engineLibDir"] = Paths.get(project.extra["nuixEngineDirectory"].toString(), "lib").pathString

// Necessary to be on PATH when test is ran which makes use of Nuix engine
project.extra["engineBinDir"] = Paths.get(project.extra["nuixEngineDirectory"].toString(), "bin").pathString
project.extra["engineBinX86Dir"] = Paths.get(project.extra["nuixEngineDirectory"].toString(), "bin", "x86").pathString

// Determines destination for tasks:
// - copyDependencyJarsToApp
// - copyJarToApp
// - copyJarsToApp
project.extra["nuixAppLibDir"] = findProperty("nuixAppLibDir")

// Configures temp directory used in several places during test execution:
// - System property 'java.io.tmpdir'
// - ENV variable 'TEMP'
// - ENV variable 'TMP'
project.extra["nuixTempDirectory"] = findProperty("tempDir")
        ?: Paths.get(System.getenv("LOCALAPPDATA"), "Temp", "Nuix").pathString

// Directory used to store data a test may rely on (like sample data)
project.extra["testDataDirectory"] = findProperty("testDataDirectory")
        ?: Paths.get("${projectDir}", "..", "..", "TestData").pathString

// Directory that tests may write data to, unique to each test invocation
project.extra["testOutputDirectory"] = findProperty("testOutputDirectory")
        ?: Paths.get("${projectDir}", "..", "..", "TestOutput", "${System.currentTimeMillis()}")

repositories {
    mavenCentral()

    val github_username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
    val github_token = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")

    maven {
        url = uri("https://maven.pkg.github.com/nuix/nuix-java-engine-baseline")
        credentials {
            username = github_username
            password = github_token
        }
    }
}

// Dependencies referenced using this configuration will be copied as part these tasks:
// - CopyDependencyJarsToApp
// - CopyDependencyJarsToEngine
// - copyJarsToApp
// - copyJarsToEngine (Note this is used to get JARs in place before test run)
val deployedDependency: Configuration by configurations.creating {
    isTransitive = true
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:+")
    implementation("org.jetbrains:annotations:24.0.1")
    compileOnly("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
    testCompileOnly("org.projectlombok:lombok:1.18.26")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.26")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    // Wrapper class for running tests in engine, pulled from public facing GitHub repository
    testImplementation("com.nuix.innovation:enginewrapper:Nuix9.10-v1.1.4")

    // Used to extract test data
    testImplementation("net.lingala.zip4j:zip4j:2.11.5")

    // Dependencies from engine lib referenced during compile time

    val compileDependencies = arrayOf(
            "*joda*.jar",
            "nuix-api*.jar",
            "nuix-util-*.jar",
            "nuix-data-*.jar",
            "nuix-scripting-*.jar",
            "aspose-cells-*.jar",
            "aspose-pdf-*.jar",
            "*slf4j*.jar",
            "*log4j*.jar",
            "*commons-io*.jar",
            "*commons-csv*.jar",
            "*commons-math*.jar",
            "*commons-lang*.jar",
            "*commons-text*.jar",
            "*commons-compress*.jar",
            "RoaringBitmap-*.jar",
            "sqlite*.jar",
            "guava*.jar",
            "gson*.jar",
            "jruby*.jar",
            "flying-saucer*.jar",
            "jaxb*.jar",
            "lucene*.jar",
            "jsoup*.jar",
            "itext*.jar",
            "sevenzip*.jar"
    )
    val lib = project.extra["engineLibDir"].toString()
    compileOnly(fileTree(baseDir = lib) { include(*compileDependencies) })
    testCompileOnly(fileTree(baseDir = lib) { include(*compileDependencies) })

    // When tests run which make use of NuixEngine wrapper class, it will need to have the
    // Nuix Engine dependencies in the lib folder present on the class path
    testRuntimeOnly(fileTree(baseDir = project.extra["engineLibDir"].toString()) {
        include("*.jar")
    })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

fun configureTestEnvironment(test: Test) {
    // Args passed to JVM running tests
    test.jvmArgs(
            "--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED",  // Engine 9.6(?) and later require this
            "-Xmx4G",
            "-Djava.io.tmpdir=\"${project.extra["nuixTempDirectory"]}\"",
            // "-verbose:class" // Can help troubleshoot weird dependency issues
    )

    // Configure ENV vars for JVM tests run in
    test.setEnvironment(
            // Add our engine release's bin and bin/x86 to PATH env var of test JVM process
            Pair("PATH", "${System.getenv("PATH")};${project.extra["engineBinDir"]};${project.extra["engineBinX86Dir"]}"),

            // Define where tests can place re-usable test data
            Pair("TEST_DATA_DIRECTORY", project.extra["testDataDirectory"]),

            // Define where tests can write output produce for later review
            Pair("TEST_OUTPUT_DIRECTORY", project.extra["testOutputDirectory"]),

            // Forward ENV username and password
            Pair("NUIX_USERNAME", System.getenv("NUIX_USERNAME")),
            Pair("NUIX_PASSWORD", System.getenv("NUIX_PASSWORD")),

            // Forward LOCALAPPDATA and APPDATA
            Pair("LOCALAPPDATA", System.getenv("LOCALAPPDATA")),
            Pair("APPDATA", System.getenv("APPDATA")),
            Pair("PROGRAMDATA", System.getenv("PROGRAMDATA")),

            // Important to have in some instances, otherwise some code may resolve a local
            // path in project tree rather than actual system drive
            Pair("SYSTEMDRIVE", System.getenv("SYSTEMDRIVE")),

            // We need to make sure we set these so workers will properly resolve temp dir
            // (when using a worker based operation via EngineWrapper).
            Pair("TEMP", project.extra["nuixTempDirectory"]),
            Pair("TMP", project.extra["nuixTempDirectory"]),

            Pair("NUIX_ENGINE_DIR", project.extra["nuixEngineDirectory"])
    )
}

// Copies compiled JAR to engine lib directory
tasks.register<Copy>("copyJarToEngine") {
    dependsOn(tasks.jar)
    // You will receive implicit dependency errors without this
    mustRunAfter(tasks.compileJava, tasks.compileTestJava)
    val jarName = "${rootProject.name}.jar"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(tasks.jar)
    into(Paths.get(project.extra["engineLibDir"].toString()).toFile())
    rename(".*\\.jar", jarName)
}

// Copies 'deployedDependency' JARs and their transitive JARs to engine lib directory
tasks.register<Copy>("copyDependencyJarsToEngine") {
    // You will receive implicit dependency errors without this
    mustRunAfter(tasks.compileJava, tasks.compileTestJava)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.findByName("deployedDependency"))
    into(Paths.get(project.extra["engineLibDir"].toString()).toFile())
    rename("(.*)\\.jar", "${rootProject.name}-Dependency-$1.jar")
}

// Copies JAR and 'deployedDependency' JARs to engine lib directory
// Invoked before tests are ran
tasks.register("copyJarsToEngine") {
    println("Copying files to engine lib dir...")
    dependsOn("copyJarToEngine")
    dependsOn("copyDependencyJarsToEngine")
}

// Copies compiled JAR to Nuix Workstation lib directory
// as defined by project.extra["nuixAppLibDir"]
tasks.register<Copy>("copyJarToApp") {
    dependsOn(tasks.jar)
    val jarName = "${rootProject.name}.jar"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(tasks.jar)
    into(Paths.get(project.extra["nuixAppLibDir"].toString()).toFile())
    rename(".*\\.jar", jarName)
}

// Copies 'deployedDependency' JARs and their transitive JARs to Nuix Workstation lib directory
// as defined by project.extra["nuixAppLibDir"]
tasks.register<Copy>("copyDependencyJarsToApp") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(configurations.findByName("deployedDependency"))
    into(Paths.get(project.extra["nuixAppLibDir"].toString()).toFile())
    rename("(.*)\\.jar", "${rootProject.name}-Dependency-$1.jar")
}

// Copies JAR and 'deployedDependency' JARs to Nuix Workstation lib directory
// as defined by project.extra["nuixAppLibDir"]
tasks.register("copyJarsToApp") {
    println("Copying files to app lib dir...")
    dependsOn("copyinJarToApp")
    dependsOn("copyDependencyJarsToApp")
}

// Before tests are ran we need to make sure a few things are done:
// - JAR and 'deployedDependency' JARs have been copied to engine lib dir
// - IDE executes our test using JUnit
// - Environment configuration is performed which sets up ENV vars, properties, etc
tasks.test {
    dependsOn(tasks.findByName("copyJarsToEngine"))
    mustRunAfter("copyDependencyJarsToEngine", "copyJarToEngine")
    useJUnitPlatform()
    configureTestEnvironment(this)
}

// Customize where Javadoc output is written to
tasks.getByName<Javadoc>("javadoc") {
    options.encoding = "UTF-8"
    setDestinationDir(Paths.get("$projectDir", "..", "..", "docs").toFile())
}