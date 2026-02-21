# ============================================================
# Stage 1: Build com Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY . .

RUN mvn package -DskipTests -q

# ============================================================
# Stage 2: Runtime (JRE enxuta)
# ============================================================
FROM eclipse-temurin:17-jre-jammy

RUN apt-get update && apt-get install -y --no-install-recommends \
    ant \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m -u 1000 -s /bin/bash dspace \
    && mkdir -p /dspace /build/dspace/target/dspace-installer \
    && chown -R dspace:dspace /dspace /build

COPY --from=builder --chown=dspace:dspace \
    /build/dspace/target/dspace-installer \
    /build/dspace/target/dspace-installer

COPY --chown=root:root docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

USER dspace
WORKDIR /dspace

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=6 \
  CMD curl -sf http://localhost:8080/server/api || exit 1

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
