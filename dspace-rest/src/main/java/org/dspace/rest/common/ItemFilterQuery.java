/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Metadata Query for DSpace Items using the REST API
 * @author Terry Brady, Georgetown University
 */
@XmlRootElement(name = "item-filter-query")
public class ItemFilterQuery {
    Logger log = Logger.getLogger(ItemFilterQuery.class);

    private String field = "";
    private String operation = "";
    private String value = "";

    public ItemFilterQuery(){}

    /**
     * Construct a metadata query for DSpace items
     * @param field
     *     Name of the metadata field to query
     * @param operation
     *     Operation to perform on a metadata field
     * @param value
     * @throws WebApplicationException
     */
    public ItemFilterQuery(String field, String operation, String value) throws WebApplicationException{
        setup(field, operation, value);
    }

    private void setup(String field, String operation, String value) {
        this.setField(field);
        this.setOperation(operation);
        this.setValue(value);
    }

    @XmlAttribute(name="field")
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
    
    @XmlAttribute(name="operation")
    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
    @XmlAttribute(name="value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
