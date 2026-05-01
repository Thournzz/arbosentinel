# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Use Maven + JDK 21 to compile and package the Spring Boot app
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first — Docker caches this layer separately
# So dependency download only re-runs when pom.xml changes, not on every code change
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
# Smaller runtime image — no Maven/JDK compiler needed, just the JRE
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from stage 1
COPY --from=build /app/target/arbosentinel-*.jar app.jar

# Railway injects PORT — we expose it here for documentation
EXPOSE 9191

# Run with prod profile active
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-Xmx512m", "-jar", "app.jar"]
