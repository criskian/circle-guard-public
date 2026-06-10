plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testRuntimeOnly("com.h2database:h2")
}

tasks.named<Test>("test") {
    maxParallelForks = 1
    jvmArgs("-Xmx384m")
    useJUnitPlatform { excludeTags("integration") }
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    maxParallelForks = 1
    jvmArgs("-Xmx384m")
    useJUnitPlatform { includeTags("integration") }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    shouldRunAfter("test")
    outputs.upToDateWhen { false }
}

tasks.named<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey",  "circleguard-form-service")
        property("sonar.projectName", "CircleGuard Form Service")
        property("sonar.sources",     "src/main/java")
        property("sonar.tests",       "src/test/java")
        property("sonar.coverage.jacoco.xmlReportPaths",
                 "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}
