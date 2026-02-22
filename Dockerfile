FROM eclipse-temurin:17-jre

ARG DSPACE_INSTALL_DIR=/dspaceinstall
ENV DSPACE_INSTALL_DIR=${DSPACE_INSTALL_DIR}

# Copia o DSpace já instalado pelo ant fresh_install no runner
COPY ${DSPACE_INSTALL_DIR} ${DSPACE_INSTALL_DIR}

EXPOSE 8080

CMD java -jar ${DSPACE_INSTALL_DIR}/webapps/server-boot.jar \
         --dspace.dir=${DSPACE_INSTALL_DIR}
