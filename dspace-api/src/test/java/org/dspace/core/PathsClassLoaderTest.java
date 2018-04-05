/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static com.sun.org.apache.bcel.internal.Constants.ACC_PUBLIC;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the PathsClassLoader.
 *
 * @author mhwood
 */
public class PathsClassLoaderTest
{
    private static final String FILENAME_PREFIX = "foo";
    private static final String CLASS_FILENAME_SUFFIX = ".class";
    private static final String JAR_FILENAME_SUFFIX = ".jar";
    private static final ClassLoader parentCL = PathsClassLoaderTest.class.getClassLoader();

    /** The test bare class file. */
    private static File classFile;

    /** The test class file in a JAR. */
    private static File jarFile;

    /** Name of the test class in the file. */
    private static String className;

    /** Name of the test class in the JAR. */
    private static String jarClassName;

    public PathsClassLoaderTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {

        // Create a name for a temporary class file.
        try {
            classFile = File.createTempFile(FILENAME_PREFIX, CLASS_FILENAME_SUFFIX);
            classFile.deleteOnExit();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        String classFileName = classFile.getName();
        className = classFileName.substring(0,
                classFileName.length() - CLASS_FILENAME_SUFFIX.length());

        // Create an empty class.
        ClassGen cg = new ClassGen(className, "java.lang.Object",
                "<generated>", ACC_PUBLIC, null);
        cg.addEmptyConstructor(ACC_PUBLIC);

        // Create a class file from the empty class.
        try {
            cg.getJavaClass().dump(classFile);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Create a JAR containing the empty class.
        JarOutputStream jar;
        try {
            jarFile = File.createTempFile(FILENAME_PREFIX, JAR_FILENAME_SUFFIX);
            jarFile.deleteOnExit();
            String jarFileName = jarFile.getName();
            jarClassName = jarFileName.substring(0,
                    jarFileName.length() - JAR_FILENAME_SUFFIX.length());

            cg = new ClassGen(jarClassName, "java.lang.Object",
                "<generated>", ACC_PUBLIC, null);
            cg.addEmptyConstructor(ACC_PUBLIC);

            jar = new JarOutputStream(new FileOutputStream(jarFile));
            JarEntry entry = new JarEntry(jarClassName + ".class");
            jar.putNextEntry(entry);
            jar.write(cg.getJavaClass().getBytes());
            jar.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    /**
     * Test of findClass method, of class PathsClassLoader.
     * @throws Exception if error
     */
    @Test
    public void testFindClass()
            throws Exception
    {
        System.out.println("findClass");

        String[] classpath = { classFile.getParent(),
            jarFile.getCanonicalPath() };
        PathsClassLoader instance = new PathsClassLoader(parentCL, classpath);
        Class result = instance.findClass(className);
        assertTrue("Should return a Class from file", result instanceof Class);

        classpath[0] = jarFile.getCanonicalPath();
        instance = new PathsClassLoader(parentCL, classpath);
        result = instance.findClass(jarClassName);
        assertTrue("Should return a Class from JAR", result instanceof Class);
    }

}
