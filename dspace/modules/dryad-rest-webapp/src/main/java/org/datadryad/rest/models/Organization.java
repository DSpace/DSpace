/*
 */
package org.datadryad.rest.models;

import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
public class Organization {
    public String organizationCode;
    public String organizationName;
    @JsonIgnore
    public Boolean isValid() {
        return (organizationCode != null && organizationCode.length() > 0);
    }
}
