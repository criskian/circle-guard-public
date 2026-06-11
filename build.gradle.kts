plugins {
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
    id("org.sonarqube") version "5.0.0.4638"
}

allprojects {
    group = "com.circleguard"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

sonarqube {
    properties {
        property("sonar.host.url",     System.getenv("SONAR_HOST_URL") ?: "http://localhost:9000")
        property("sonar.login",        System.getenv("SONAR_TOKEN") ?: "")
        property("sonar.projectName",  "CircleGuard")
        property("sonar.projectKey",   "circleguard")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.coverage.jacoco.xmlReportPaths",
                 "**/build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")
    apply(plugin = "org.sonarqube")
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:3.2.4"))
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        // ── Observabilidad (Día 4): métricas Prometheus + tracing distribuido OTLP ──
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("io.micrometer:micrometer-registry-prometheus")
        "implementation"("io.micrometer:micrometer-tracing-bridge-otel")
        "implementation"("io.opentelemetry:opentelemetry-exporter-otlp")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("com.h2database:h2")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<org.gradle.testing.jacoco.tasks.JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        executionData.from(
            fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") }
        )
    }
}
