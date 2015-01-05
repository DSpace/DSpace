/*
 */
package org.datadryad.rest.storage;

import com.sun.istack.logging.Logger;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class StoragePathTest {
    private static final Logger log = Logger.getLogger(StoragePathTest.class);

    /**
     * Test of addPathElement method, of class StoragePath.
     */
    @Test
    public void testAddPathElement() {
        log.info("addPathElement");
        String key = "key";
        String value = "value";
        StoragePath instance = new StoragePath();
        assertEquals(0, instance.size());
        instance.addPathElement(key, value);
        assertEquals("Path size should be 1", 1, instance.size());
    }

    /**
     * Test of getKeyPath method, of class StoragePath.
     */
    @Test
    public void testGetKeyPath() {
        log.info("getKeyPath");
        StoragePath instance = new StoragePath();
        String key = "key";
        String value = "value";
        List expResult = Arrays.asList(key);
        instance.addPathElement(key, value);
        List result = instance.getKeyPath();
        assertEquals("key Path should be a list containing only the test key", expResult, result);
    }

    /**
     * Test of getValuePath method, of class StoragePath.
     */
    @Test
    public void testGetValuePath() {
        log.info("getValuePath");
        StoragePath instance = new StoragePath();
        String key = "key";
        String value = "value";
        List expResult = Arrays.asList(value);
        instance.addPathElement(key, value);
        List result = instance.getValuePath();
        assertEquals("value path should be a list containing only the test value", expResult, result);
    }

    /**
     * Test of validElements method, of class StoragePath.
     */
    @Test
    public void testInvalidElements() {
        System.out.println("invalidElements");
        StoragePath instance = new StoragePath();
        // Should be invalid if string length is 0
        String key = "key";
        String invalidValue = "";
        instance.addPathElement(key, invalidValue);
        Boolean expResult = false;
        Boolean result = instance.validElements();
        assertEquals("Storage Path with zero-length value should not be valid", expResult, result);
    }

    /**
     * Test of validElements method, of class StoragePath.
     */
    @Test
    public void testValidElements() {
        System.out.println("validElements");
        StoragePath instance = new StoragePath();
        // Should be invalid if string length is 0
        String key = "key";
        String validValue = "f";
        instance.addPathElement(key, validValue);
        Boolean expResult = true;
        Boolean result = instance.validElements();
        assertEquals("Storage path with nonzero-length value should be valid", expResult, result);
    }

}
