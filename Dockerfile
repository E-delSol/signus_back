# ---- build stage
FROM gradle:8.7-jdk21 AS build
WORKDIR /app

# Copiamos primero metadata para aprovechar cache
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
COPY gradle/libs.versions.toml gradle/libs.versions.toml

# Descarga dependencias (mejora cache)
RUN ./gradlew --no-daemon dependencies || true

# Copiamos el resto del proyecto
COPY . .

# Genera distribución runnable (más estable que buscar el jar)
RUN ./gradlew --no-daemon clean installDist

# ---- run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# OJO: el nombre del directorio depende del project.name en settings.gradle.kts
# Normalmente: /app/build/install/<projectName>/
COPY --from=build /app/build/install /app/install

# Arranca con el script generado por Gradle
# Ajusta <projectName> si tu settings.gradle.kts define otro nombre
ENV PORT=8080
EXPOSE 8080

# Cambia "signus_back" por el nombre real del proyecto si difiere
ENTRYPOINT ["/app/install/signus_back/bin/signus_back"]