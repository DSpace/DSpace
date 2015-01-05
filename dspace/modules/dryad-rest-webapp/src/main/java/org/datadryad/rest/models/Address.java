/*
 */
package org.datadryad.rest.models;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement

public class Address {
    public String addressLine1, addressLine2, addressLine3;
    public String city, zip, state, country;
    public Address() {}
}
