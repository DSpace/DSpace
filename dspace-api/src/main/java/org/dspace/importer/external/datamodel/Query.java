/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.datamodel;

import org.apache.commons.collections.map.MultiValueMap;

import java.util.Collection;

/** Represents a query to a source. Subclasses may enforce stricter typing or more verbose setting of parameters.
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class Query {

    private MultiValueMap parameters = new MultiValueMap();
    /**
     * Retrieve the parameters set to this Query object
     *
     * @return the {@link org.apache.commons.collections.map.MultiValueMap} set to this object
     */
    public MultiValueMap getParameters() {
        return parameters;
    }

    /**
     * In the parameters variable, adds the value to the collection associated with the specified key.
     * <p>
     * Unlike a normal <code>Map</code> the previous value is not replaced.
     * Instead the new value is added to the collection stored against the key.
     *
     * @param key  the key to store against
     * @param value  the value to add to the collection at the key
     */
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }

    /**
     * In the parameters variable, adds the value to the collection associated with the specified key.
     * <p>
     * Unlike {@link #addParameter(String, Object)} the previous value is overridden.
     * First, any existing values are removed, then the new value is added to the collection at the specified key
     *
     * @param key  the key to store against
     * @param value  the value to add to the collection at the key
     */
    protected void addSingletonParameter(String key, Object value) {
        parameters.remove(key);
        parameters.put(key, value);
    }

    /**
     * Retrieve a parameter as a certain given class
     * @param <T> the type of parameter returned.
     * @param key the key to retrieve the parameter from
     * @param clazz the type to retrieve. (If no parameter with that class is found, a <tt>null</tt> value is returned.)
     * @return the selected parameter, or null.
     */
    public <T> T getParameterAsClass(String key, Class<T> clazz) {
        Collection c=parameters.getCollection(key);
        if (c==null || c.isEmpty()) {
            return null;
        } else {
            Object o = c.iterator().next();
            if (clazz.isAssignableFrom(o.getClass())) {
                return (T) o;
            } else {
                return null;
            }
        }
    }
    /**
     * Gets the collection mapped to the specified key.
     * This method is a convenience method to typecast the result of <code>get(key)</code>.
     *
     * @param key  the key used to retrieve the collection
     * @return the collection mapped to the key, null if no mapping
     */
    public Collection getParameter(String key) {
        return parameters.getCollection(key);
    }


    /**
     * Set the parameters of this query object based on a given {@link org.apache.commons.collections.map.MultiValueMap}
     * @param parameters a {@link org.apache.commons.collections.map.MultiValueMap} to set to this Query object
     */
    public void setParameters(MultiValueMap parameters) {
        this.parameters = parameters;
    }
}
