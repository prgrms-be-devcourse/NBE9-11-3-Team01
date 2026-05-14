plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("kapt") version "2.2.21"
    kotlin("plugin.lombok") version "2.2.21"
}

group = "com.team01"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-json")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
	
	// 코틀린 표준 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // H2
    runtimeOnly("com.h2database:h2")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // 가상 메일 서버 (개발/테스트용)
    implementation("com.icegreen:greenmail:2.1.0")

    // Lombok (Kotlin 전환 전까지 유지)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    kapt("org.projectlombok:lombok") // 코틀린이 자바 롬복을 인식하게 만드는 설정

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}
kapt {
    keepJavacAnnotationProcessors = true
}
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

//
//
//
//plugins {
//    java
//    id("org.springframework.boot") version "4.0.5"
//    id("io.spring.dependency-management") version "1.1.7"
//    kotlin("jvm") version "2.2.21"
//    kotlin("plugin.spring") version "2.2.21"
//    kotlin("plugin.jpa") version "2.2.21"
////    kotlin("kapt") version "2.2.21"
//}
//
//group = "com.team01"
//version = "0.0.1-SNAPSHOT"
//description = "backend"
//
//java {
//    toolchain {
//        languageVersion = JavaLanguageVersion.of(24)
//    }
//}
//
//repositories {
//    mavenCentral()
//}
//
//dependencies {
//    implementation("org.springframework.boot:spring-boot-h2console")
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    implementation("org.springframework.boot:spring-boot-starter-validation")
//    implementation("org.springframework.boot:spring-boot-starter-webmvc")
//    implementation("org.springframework.boot:spring-boot-starter-security")
//    implementation("org.springframework.boot:spring-boot-starter-web")
//    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
//    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
//    implementation("org.springframework.boot:spring-boot-starter-mail")
//
//    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
//
//    implementation ("org.springframework.boot:spring-boot-starter-data-redis")
//    implementation ("org.springframework.boot:spring-boot-starter-json")
//    implementation ("com.fasterxml.jackson.core:jackson-databind")
//    implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
//
//    implementation("com.icegreen:greenmail:2.1.0") 	// [추가] 가상 메일 서버 (테스트 및 개발용)
//
//    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
//    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
//    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
//
//    compileOnly("org.projectlombok:lombok")
//    developmentOnly("org.springframework.boot:spring-boot-devtools")
//    runtimeOnly("com.h2database:h2")
//    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
//    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
//    annotationProcessor("org.projectlombok:lombok")
////    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
//    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
//    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
//    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testCompileOnly("org.projectlombok:lombok")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
//    testAnnotationProcessor("org.projectlombok:lombok")
//}
//
//tasks.withType<Test> {
//    useJUnitPlatform()
//}