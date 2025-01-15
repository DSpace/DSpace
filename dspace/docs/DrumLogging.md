# DRUM Logging

## Introduction

This document describes DRUM customizations to the DSpace logging configuration.

DRUM customizes DSpace to enable JSON-formatted log entries when using
Kubernetes, as it enables more flexible search options when using Splunk.

By default, DSpace configures the Spring Boot embedded Tomcat server to use the
Log4j2 logging framework, via the
"org.springframework.boot:spring-boot-starter-log4j2" Maven dependency, in
preference to the stock Spring Boot "logback" framework.

## DRUM DSpace Logs

There are two distinct log configurations for the DRUM DSpace application:

* DSpace application logs
* Spring Boot embedded Tomcat server access logs

## Local Development Environment Logging

In the local development environment, DSpace is run via Docker Compose, and
the DSpace application logs are controlled by the
"dspace/config/log4j2-container.xml" file.

The Tomcat server access logs are controlled by Spring Boot. The default
Spring Boot configuration, which does not enable the Tomcat server access log,
is typically used, but can be managed by Spring Boot "server.tomcat.accesslog.*"
properties in the "dspace/config/local.cfg", see
<https://docs.spring.io/spring-boot/docs/3.2.6/reference/html/application-properties.html#appendix.application-properties.server>.

To enable JSON-formatted logging (similar to how logs are displayed in
Kubernetes), do the following:

1) In "dspace/config/log4j2-container.xml", replace the "PatternLayout"
   stanza in the "A1" appender:

    ```xml
            <Appender name='A1'
                      type='Console'
                <JsonTemplateLayout eventTemplateUri="classpath:EcsLayout.json" />
           </Appender>
    ```

   This will enable JSON-formatted logging for the DSpace application logs.

2) Add the following to "dspace/config/local.cfg" to enable the Tomcat access
   logs (in JSON format):

   ```text
   # Tomcat access log
   # Set umd.server.tomcat.accesslog.json.enabled to "true" for JSON logging
   umd.server.tomcat.accesslog.json.enabled=true
   # Set server.tomcat.accesslog.enabled to "true" for common logging
   #server.tomcat.accesslog.enabled=true
   server.tomcat.accesslog.directory=/dev
   server.tomcat.accesslog.prefix=stdout
   server.tomcat.accesslog.buffered=false
   server.tomcat.accesslog.suffix=
   server.tomcat.accesslog.file-date-format=
   server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D
   ```

## Kubernetes JSON-formatted Logging

### DRUM - DSpace Logs

In Kubernetes, the DSpace application logs are controlled by the
"overlays/\<NAMESPACE>/log4j2.xml" file (where "\<NAMESPACE>" is the Kubernetes
namespace (i.e., "sandbox", "test", "qa", or "prod")).

To enable JSON-formatted logging, the Log4j "Appenders" are modified to use
the "JsonTemplateLayout", i.e.:

```xml
        <Appender name='A1'
                  ...
        >
            ...
            <JsonTemplateLayout eventTemplateUri="classpath:EcsLayout.json" />
            ...
        </Appender>
```

The JSON-formatted Tomcat access log is enabled by the
"umd.server.tomcat.accesslog.json.enabled" property in the
"overlays/\<NAMESPACE>/local.cfg" file:

```text
# Tomcat access log
umd.server.tomcat.accesslog.json.enabled=true
server.tomcat.accesslog.directory=/dev
server.tomcat.accesslog.prefix=stdout
server.tomcat.accesslog.buffered=false
server.tomcat.accesslog.suffix=
server.tomcat.accesslog.file-date-format=
server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D
```

### DRUM - Solr Logs

#### Solr Log4J application logs

In Kubernetes, the Solr application logs are controlled by the
"overlays/\<NAMESPACE>/solr/log4j2.xml" file (where "\<NAMESPACE>" is the
Kubernetes namespace (i.e., "sandbox", "test", "qa", or "prod").

The "log4j2.xml" file configures Log4J to use the "JsonTemplateLayout", to
provide JSON-formatted output to Splunk.

#### Solr Java garbage collection logs

The command-line options provided to the JVM to run Solr are configured by the
stock "solr" Docker image to log Java garbage collection activity in the
"/var/solr/logs/solr_gc.log" file. This logging is *not* controlled by Log4J,
and is *not* JSON-formatted.

The command-line options that generate the "solr_gc.log" specify a rolling log
with each file limited to 20 megabytes, and a maximum of 10 files, so the
maximum size of the logs should be 200 megabytes.

## DRUM Customizations

To enable JSON-formatted logging, the embedded Tomcat server provided by Spring
Boot must be modified to use a "org.apache.catalina.valves.JsonAccessLogValve"
valve instead of the default "org.apache.catalina.valves.AccessLogValve".

In a non-embedded Tomcat server, the "JsonAccessLogValve" would be configured
via the Tomcat's "server.xml" configuration file. When using the Spring Boot
embedded Tomcat server, this file is not available, and instead an
implementation of the Spring Boot
"org.springframework.boot.web.server.WebServerFactoryCustomizer" class is used,
see <https://docs.spring.io/spring-boot/docs/3.2.6/reference/html/howto.html#howto.webserver.configure>.

The "dspace/modules/server-boot/src/main/java/org/dspace/app/UmdTomcatWebServerFactoryCustomizer.java"
class provides the "WebServerFactoryCustomizer" implementation necessary to
configure the Spring Boot embedded Tomcat server to use the
"JsonAccessLogValve".

The "dspace/modules/server-boot/src/main/java/org/dspace/app/ServerBootApplication.java"
class has been customized to include the "UmdTomcatWebServerFactoryCustomizer"
class has part of its configuration.

## log4j-layout-template-json Dependency

The "JsonTemplateLayout" used in the Log4J appender is provided by the
"org.apache.logging.log4j:log4j-layout-template-json" Maven dependency.

In order for the "JsonTemplateLayout" class to be available to both the
Spring Boot embedded Tomcat server, and the DSpace Java application, the
"log4j-layout-template-json" dependency has been added to the
"dspace-api/pom.xml" file, as this gives it the earliest integration at
startup.

If JSON formatting is only needed for the embedded Tomcat server, and *not*
the DSpace application, the "log4j-layout-template-json" only needs to be added
to the "dspace/modules/server-boot/pom.xml" file.
