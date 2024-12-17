/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * used to map @see org.dspace.app.ldn.model.Notification
 */
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Base {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private Set<String> type;

    /**
     * 
     */
    public Base() {
        type = new HashSet<>();
    }

    /**
     * @return String
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Set<String>
     */
    public Set<String> getType() {
        return type;
    }

    /**
     * @param type
     */
    public void setType(java.lang.Object type) {
        if (type instanceof String) {
            this.type.add((String) type);
        } else if (type instanceof Collection) {
            this.type.addAll((Collection) type);
        }
    }

    /**
     * @param type
     */
    public void addType(String type) {
        this.type.add(type);
    }

    /**
     * @return int
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /**
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Base other = (Base) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}