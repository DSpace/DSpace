# ─────────────────────────────────────────────────────────────────────────────
# Dockerfile — DSpace Backend (Runnable JAR / Spring Boot)
#
# O CI copia o resultado do ant fresh_install para ./dspace-installed/
# antes de rodar o docker build, tornando o path relativo ao contexto.
# ─────────────────────────────────────────────────────────────────────────────

FROM eclipse-temurin:17-jre

ENV DSPACE_INSTALL_DIR=/dspaceinstall

# Copia o conteúdo instalado (path relativo ao contexto do build)
COPY dspace-installed/ ${DSPACE_INSTALL_DIR}/

EXPOSE 8080

CMD ["java", "-jar", "/dspaceinstall/webapps/server-boot.jar", "--dspace.dir=/dspaceinstall"]
