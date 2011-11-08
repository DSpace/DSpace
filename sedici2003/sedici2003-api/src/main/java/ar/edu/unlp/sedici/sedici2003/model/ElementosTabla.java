/**
 * <?xml version="1.0"?>
 * <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
 *     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 *   <modelVersion>4.0.0</modelVersion>
 *   <parent>
 *     <artifactId>sedici2003</artifactId>
 *     <groupId>ar.edu.unlp.sedici</groupId>
 *     <version>1.8.0-rc1</version>
 *   </parent>
 *   <artifactId>sedici2003-api</artifactId>
 *   <name>sedici2003-api</name>
 *   <url>http://maven.apache.org</url>
 *
 *   <properties>
 *     <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
 *   </properties>
 *   <dependencies>
 *     <dependency>
 *       <groupId>junit</groupId>
 *       <artifactId>junit</artifactId>
 *       <version>3.8.1</version>
 *       <scope>test</scope>
 *     </dependency>
 *   </dependencies>
 * </project>
 */
package ar.edu.unlp.sedici.sedici2003.model;

import org.springframework.roo.addon.dbre.RooDbManaged;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooEntity(versionField = "", table = "elementos_tabla")
@RooDbManaged(automaticallyDelete = true)
public class ElementosTabla {
}
