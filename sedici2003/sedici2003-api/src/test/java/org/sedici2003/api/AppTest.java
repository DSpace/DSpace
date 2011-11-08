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
package org.sedici2003.api;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
