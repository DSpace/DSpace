/**
 * <?xml version="1.0"?>
 * <project
 * 	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"
 * 	xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 * 	<modelVersion>4.0.0</modelVersion>
 * 	<parent>
 * 		<artifactId>sedici2003</artifactId>
 * 		<groupId>ar.edu.unlp.sedici</groupId>
 * 		<version>1.8.0-rc1</version>
 * 	</parent>
 *
 * 	<artifactId>sedici2003-web</artifactId>
 *
 * 	<name>sedici2003-web</name>
 * 	<url>http://maven.apache.org</url>
 * 	<properties>
 * 		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
 * 	</properties>
 * 	<dependencies>
 * 		<!-- General dependencies for standard applications -->
 *
 * 		<dependency>
 * 			<groupId>ar.edu.unlp.sedici</groupId>
 * 			<artifactId>sedici2003-api</artifactId>
 * 			<version>${project.version}</version>
 * 			<type>jar</type>
 * 		</dependency>
 *
 * 		<dependency>
 * 			<groupId>org.springframework</groupId>
 * 			<artifactId>spring-web</artifactId>
 * 			<version>${spring.version}</version>
 * 			<classifier />
 * 			<exclusions>
 * 				<exclusion>
 * 					<groupId>commons-logging</groupId>
 * 					<artifactId>commons-logging</artifactId>
 * 				</exclusion>
 * 			</exclusions>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.springframework</groupId>
 * 			<artifactId>spring-webmvc</artifactId>
 * 			<version>${spring.version}</version>
 * 			<classifier />
 * 			<exclusions>
 * 				<exclusion>
 * 					<groupId>commons-logging</groupId>
 * 					<artifactId>commons-logging</artifactId>
 * 				</exclusion>
 * 			</exclusions>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.springframework.webflow</groupId>
 * 			<artifactId>spring-js-resources</artifactId>
 * 			<version>2.2.1.RELEASE</version>
 * 			<classifier />
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>commons-digester</groupId>
 * 			<artifactId>commons-digester</artifactId>
 * 			<version>2.0</version>
 * 			<classifier />
 * 			<exclusions>
 * 				<exclusion>
 * 					<groupId>commons-logging</groupId>
 * 					<artifactId>commons-logging</artifactId>
 * 				</exclusion>
 * 			</exclusions>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>commons-fileupload</groupId>
 * 			<artifactId>commons-fileupload</artifactId>
 * 			<version>1.2.1</version>
 * 			<classifier />
 * 			<exclusions>
 * 				<exclusion>
 * 					<groupId>commons-logging</groupId>
 * 					<artifactId>commons-logging</artifactId>
 * 				</exclusion>
 * 			</exclusions>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>javax.servlet.jsp.jstl</groupId>
 * 			<artifactId>jstl-api</artifactId>
 * 			<version>1.2</version>
 * 			<classifier />
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.glassfish.web</groupId>
 * 			<artifactId>jstl-impl</artifactId>
 * 			<version>1.2</version>
 * 			<classifier />
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>javax.el</groupId>
 * 			<artifactId>el-api</artifactId>
 * 			<version>1.0</version>
 * 			<scope>provided</scope>
 * 			<classifier />
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>joda-time</groupId>
 * 			<artifactId>joda-time</artifactId>
 * 			<version>1.6</version>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>javax.servlet.jsp</groupId>
 * 			<artifactId>jsp-api</artifactId>
 * 			<version>2.1</version>
 * 			<scope>provided</scope>
 * 			<classifier />
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>commons-codec</groupId>
 * 			<artifactId>commons-codec</artifactId>
 * 			<version>1.4</version>
 * 			<classifier />
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.apache.tiles</groupId>
 * 			<artifactId>tiles-core</artifactId>
 * 			<version>2.2.1</version>
 * 			<classifier />
 * 			<exclusions>
 * 				<exclusion>
 * 					<groupId>commons-logging</groupId>
 * 					<artifactId>commons-logging</artifactId>
 * 				</exclusion>
 * 			</exclusions>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.apache.tiles</groupId>
 * 			<artifactId>tiles-jsp</artifactId>
 * 			<version>2.2.1</version>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.springframework.security</groupId>
 * 			<artifactId>spring-security-core</artifactId>
 * 			<version>${spring-security.version}</version>
 * 			<classifier />
 * 			<exclusions>
 * 				<exclusion>
 * 					<groupId>commons-logging</groupId>
 * 					<artifactId>commons-logging</artifactId>
 * 				</exclusion>
 * 			</exclusions>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.springframework.security</groupId>
 * 			<artifactId>spring-security-config</artifactId>
 * 			<version>${spring-security.version}</version>
 * 			<classifier />
 * 			<exclusions>
 * 				<exclusion>
 * 					<groupId>commons-logging</groupId>
 * 					<artifactId>commons-logging</artifactId>
 * 				</exclusion>
 * 			</exclusions>
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.springframework.security</groupId>
 * 			<artifactId>spring-security-web</artifactId>
 * 			<version>${spring-security.version}</version>
 * 			<classifier />
 * 		</dependency>
 * 		<dependency>
 * 			<groupId>org.springframework.security</groupId>
 * 			<artifactId>spring-security-taglibs</artifactId>
 * 			<version>${spring-security.version}</version>
 * 			<classifier />
 * 		</dependency>
 *
 * 	</dependencies>
 *
 *
 *
 * 	<build>
 * 		<plugins>
 * 			<plugin>
 * 				<groupId>org.apache.maven.plugins</groupId>
 * 				<artifactId>maven-war-plugin</artifactId>
 * 				<version>2.1.1</version>
 * 				<!-- <configuration> <webXml>target/web.xml</webXml> </configuration> -->
 * 				<configuration>
 * 					<webResources>
 * 						<resource>
 * 							<directory>src/main/resources</directory>
 * 							<excludes>
 * 								<exclude>DBRE XML</exclude>
 * 							</excludes>
 * 						</resource>
 * 					</webResources>
 * 				</configuration>
 * 			</plugin>
 * 			<plugin>
 * 				<groupId>org.codehaus.mojo</groupId>
 * 				<artifactId>tomcat-maven-plugin</artifactId>
 * 				<version>1.1</version>
 * 			</plugin>
 * 			<plugin>
 * 				<groupId>org.mortbay.jetty</groupId>
 * 				<artifactId>jetty-maven-plugin</artifactId>
 * 				<version>7.4.2.v20110526</version>
 * 				<configuration>
 * 					<webAppConfig>
 * 						<contextPath>/${project.name}</contextPath>
 * 					</webAppConfig>
 * 				</configuration>
 * 			</plugin>
 *
 *
 * 			<plugin>
 * 				<groupId>org.apache.maven.plugins</groupId>
 * 				<artifactId>maven-deploy-plugin</artifactId>
 * 				<version>2.6</version>
 * 			</plugin>
 * 		</plugins>
 * 	</build>
 * </project>
 */
package ar.edu.unlp.sedici.sedici2003.web;

import ar.edu.unlp.sedici.sedici2003.model.IpDownloads;
import org.springframework.roo.addon.web.mvc.controller.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RooWebScaffold(path = "ipdownloadses", formBackingObject = IpDownloads.class)
@RequestMapping("/ipdownloadses")
@Controller
public class IpDownloadsController {
}
