# syntax=docker/dockerfile:1.7

# ─────────────────────────────────────────────────────────────────────
# Stage 1 — build
# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS build

WORKDIR /src

# Cache Maven dependencies in a separate layer.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

# Now copy sources; this layer invalidates on any code change.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -DskipTests

# ─────────────────────────────────────────────────────────────────────
# Stage 2 — runtime
# ─────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime

# Non-root user
RUN groupadd --system --gid 1001 sovereign \
 && useradd  --system --uid 1001 --gid sovereign --create-home sovereign

WORKDIR /app
RUN chown sovereign:sovereign /app

COPY --from=build --chown=sovereign:sovereign \
     /src/target/sovereign-brain-1.0.0-SOVEREIGN.jar \
     /app/sovereign-brain.jar

USER sovereign

# Structured logging on stdout; headless so nothing tries to open a TTY.
ENV JAVA_OPTS="-Djava.awt.headless=true -Dsovereign.headless=true"
ENV SOVEREIGN_MODE="autonomous"

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD bash -c '</dev/tcp/127.0.0.1/8084' || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/sovereign-brain.jar \"$@\"", "--"]
