/*
 */
package org.datadryad.rest.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement

public class CorrespondingAuthor {
    public Address address;
    public Author author;
    public String email;

    public CorrespondingAuthor() {}
}
