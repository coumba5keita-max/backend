# ==========================================
# Étape 1 : Build de l'application Spring Boot
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copie du descripteur et du code source
COPY pom.xml .
COPY src ./src

# Compilation avec cache Maven persistant
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -Dmaven.wagon.http.retryHandler.count=5 -Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false

# ==========================================
# Étape 2 : Image d'exécution allégée (Alpine JRE)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Bonnes pratiques de sécurité : exécuter avec un utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/uploads/attachments \
    && chown -R spring:spring /app

# Récupération de l'artefact généré depuis le conteneur de build
COPY --chown=spring:spring --from=build /app/target/*.jar app.jar
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]