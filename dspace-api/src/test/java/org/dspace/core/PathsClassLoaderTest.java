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
import java.io.IOException;
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
    private static final String FILE_NAME_PREFIX = "foo";
    private static final String FILE_NAME_SUFFIX = ".class";
    private static final ClassLoader parentCL = PathsClassLoaderTest.class.getClassLoader();

    private static File classFile;
    private static String className;

    public PathsClassLoaderTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
        try {
            classFile = File.createTempFile(FILE_NAME_PREFIX, FILE_NAME_SUFFIX);
            classFile.deleteOnExit();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        String classFileName = classFile.getName();
        className = classFileName.substring(0,
                classFileName.length() - FILE_NAME_SUFFIX.length());

        ClassGen cg = new ClassGen(className, "java.lang.Object",
                "<generated>", ACC_PUBLIC, null);
        cg.addEmptyConstructor(ACC_PUBLIC);

        try {
            cg.getJavaClass().dump(classFile);
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
     * @throws java.lang.Exception
     */
    @Test
    public void testFindClass()
            throws Exception
    {
        System.out.println("findClass");

        String[] classpath = { classFile.getParent() };
        PathsClassLoader instance = new PathsClassLoader(parentCL, classpath);
        Class result = instance.findClass(className);
        assertTrue("Should return a Class", result instanceof Class);
    }

}
