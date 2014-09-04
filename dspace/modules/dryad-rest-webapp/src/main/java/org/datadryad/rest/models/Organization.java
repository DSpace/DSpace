/*
 */
package org.datadryad.rest.models;

import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.datadryad.rest.storage.StoragePath;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
public class Organization {
    public static final String ORGANIZATION_CODE = "organizationCode";

    /**
     * Extracts the organizationCode from a StoragePath
     * @param path the StoragePath
     * @return the value of the path key for "organizationCode", or null if not found
     */
    public static String getOrganizationCode(StoragePath path) {
        int index = path.getKeyPath().indexOf(Organization.ORGANIZATION_CODE);
        if (index != -1) {
            return path.getValuePath().get(index);
        } else {
            return null;
        }
    }
    public Integer organizationId;
    public String organizationCode;
    public String organizationName;
    @JsonIgnore
    public Boolean isValid() {
        return (organizationCode != null && organizationCode.length() > 0);
    }
}
