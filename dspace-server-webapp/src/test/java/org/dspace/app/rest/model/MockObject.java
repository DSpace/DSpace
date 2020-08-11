/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * A simple model object for use with tests.
 *
 * Simulates a typical JPA object obtained through the DSpace service layer.
 */
public class MockObject {

    private Long storedId;

    private String storedValue;

    public static MockObject create(long id) {
        MockObject mockObject = new MockObject();
        mockObject.setStoredId(id);
        mockObject.setStoredValue("value" + id);
        return mockObject;
    }

    public Long getStoredId() {
        return storedId;
    }

    public void setStoredId(Long storedId) {
        this.storedId = storedId;
    }

    public String getStoredValue() {
        return storedValue;
    }

    public void setStoredValue(String storedValue) {
        this.storedValue = storedValue;
    }
}
