/*
 */
package org.datadryad.rest.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
public class Author {
    public String familyName;
    public String givenNames;
    public String identifier;
    public String identifierType;
    public Author() {}
    public final String fullName() {
        return familyName + ", " + givenNames;
    }
}
