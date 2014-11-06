/*
 */
package org.datadryad.rest.auth;

import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class AuthorizationTupleTest {
    private static final Logger log = Logger.getLogger(AuthorizationTupleTest.class);

    private static final Integer EPERSON_ID = 500;
    private static final String ROOT_PATH = "/path";
    private static final String PATH_A = "/path/a";
    private static final List<String> PATH_A_COMPONENTS = Arrays.asList("path","a");
    private static final String PATH_B = "/path/b";
    private static final String METHOD_GET = "GET";

    /**
     * Test of isComplete method, of class AuthorizationTuple.
     */
    @Test
    public void testIsComplete() {
        log.info("isComplete");
        AuthorizationTuple instance = new AuthorizationTuple(EPERSON_ID, METHOD_GET, ROOT_PATH);
        Boolean expResult = Boolean.TRUE;
        Boolean result = instance.isComplete();
        assertEquals("AuthorizationTuple with all parameters should be complete", expResult, result);
    }

    /**
     * Test of isComplete method, of class AuthorizationTuple.
     */
    @Test
    public void testIsNotComplete() {
        log.info("isNotComplete");
        AuthorizationTuple instance = new AuthorizationTuple(EPERSON_ID, METHOD_GET, null);
        Boolean expResult = Boolean.FALSE;
        Boolean result = instance.isComplete();
        assertEquals("AuthorizationTuple with null parameter should not be complete", expResult, result);
    }

    /**
     * Test of getPathComponents method, of class AuthorizationTuple.
     */
    @Test
    public void testGetPathComponents() {
        log.info("getPathComponents");
        AuthorizationTuple instance = new AuthorizationTuple(EPERSON_ID, METHOD_GET, PATH_A);
        List expResult = PATH_A_COMPONENTS;
        List result = instance.getPathComponents();
        assertEquals("Path components should match list", expResult, result);
    }

    /**
     * Test of containsPath method, of class AuthorizationTuple.
     */
    @Test
    public void testContainsPathParentChild() {
        log.info("containsPathParentChild");
        AuthorizationTuple parentPathTuple = new AuthorizationTuple(EPERSON_ID, METHOD_GET, ROOT_PATH);
        AuthorizationTuple childPathTuple = new AuthorizationTuple(EPERSON_ID, METHOD_GET, PATH_A);
        Boolean expResult = Boolean.TRUE;
        Boolean result = parentPathTuple.containsPath(childPathTuple);
        assertEquals("Testing root path containing child path should be true", expResult, result);
    }

    /**
     * Test of containsPath method, of class AuthorizationTuple.
     */
    @Test
    public void testNotContainsPathChildParent() {
        log.info("notContainsPathChildParent");
        AuthorizationTuple parentPathTuple = new AuthorizationTuple(EPERSON_ID, METHOD_GET, ROOT_PATH);
        AuthorizationTuple childPathTuple = new AuthorizationTuple(EPERSON_ID, METHOD_GET, PATH_A);
        Boolean expResult = Boolean.FALSE;
        Boolean result = childPathTuple.containsPath(parentPathTuple);
        assertEquals("Testing child path containing root path should be false", expResult, result);
    }

    /**
     * Test of containsPath method, of class AuthorizationTuple.
     */
    @Test
    public void testNotContainsPath() {
        log.info("notContainsPath");
        AuthorizationTuple tupleA = new AuthorizationTuple(EPERSON_ID, METHOD_GET, PATH_A);
        AuthorizationTuple tupleB = new AuthorizationTuple(EPERSON_ID, METHOD_GET, PATH_B);
        Boolean expResult = Boolean.FALSE;
        Boolean result = tupleA.containsPath(tupleB);
        assertEquals(PATH_A + " should not contain " + PATH_B, expResult, result);
        result = tupleB.containsPath(tupleA);
        assertEquals(PATH_B + " should not contain " + PATH_A, expResult, result);
        result = tupleB.containsPath(tupleA);
    }
}
