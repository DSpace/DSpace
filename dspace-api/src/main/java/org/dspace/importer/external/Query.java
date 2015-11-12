/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external;

import org.apache.commons.collections.map.MultiValueMap;

import java.util.Collection;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 27/09/12
 * Time: 15:26
 */
public class Query {
    private MultiValueMap parameters = new MultiValueMap();

    public MultiValueMap getParameters() {
        return parameters;
    }

    public void addParameter(String key,Object value){
        parameters.put(key,value);
    }

    protected void addSingletonParameter(String key,Object value){
        parameters.remove(key);
        parameters.put(key,value);
    }

    public <T> T getParameterAsClass(String key, Class<T> clazz){
        Collection c=parameters.getCollection(key);
        if(c==null||c.isEmpty()) return null;
        else {
            Object o=c.iterator().next();
            if(clazz.isAssignableFrom(o.getClass()))
            return (T) o ;
            else return null;
        }

    }

    public Collection getParameter(String key){
        return parameters.getCollection(key);
    }

    public void setParameters(MultiValueMap parameters) {
        this.parameters = parameters;
    }
}
