/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * 
 * http://www.dspace.org/license/
 */
/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.02.01 at 04:40:47 PM GMT 
//

package org.orcid.jaxb.model.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}email" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://www.orcid.org/ns/orcid}address" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType( propOrder = { "email", "address" })
@XmlRootElement(name = "contact-details")
public class ContactDetails implements Serializable {

    private final static long serialVersionUID = 1L;
    protected List<Email> email;
    protected Address address;

    /**
     * Gets the value of the email property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the email property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getEmail().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link org.orcid.jaxb.model.message.Email }
     * 
     * 
     */
    public List<Email> getEmail() {
        if (email == null) {
            email = new ArrayList<Email>();
        }
        return this.email;
    }

    public void setEmail(List<Email> email) {
        this.email = email;
    }

    public Email getEmailByString(String emailString) {
        List<Email> emailList = getEmail();
        for (Email email : emailList) {
            if (emailString.equalsIgnoreCase(email.getValue())) {
                return email;
            }
        }
        return null;
    }

    /**
     * This is a covenience method for setting the primary email.
     * 
     * It is intentionally not called setPrimaryEmail so that is doesn't get
     * called by the visibility filter, which looks at all getters and setters.
     */
    public void addOrReplacePrimaryEmail(Email primaryEmail) {
        List<Email> emailList = getEmail();
        Iterator<Email> emailIterator = emailList.iterator();
        while (emailIterator.hasNext()) {
            Email email = emailIterator.next();
            if (email.isPrimary()) {
                emailIterator.remove();
            }
        }
        primaryEmail.setPrimary(true);
        emailList.add(primaryEmail);
    }

    /**
     * This is a covenience method for getting the primary email.
     * 
     * It is intentionally not called getPrimaryEmail so that is doesn't get
     * called by the visibility filter, which looks at all getters and setters.
     */
    public Email retrievePrimaryEmail() {
        for (Email email : getEmail()) {
            if (email.isPrimary()) {
                return email;
            }
        }
        return null;
    }

    /**
     * covenience method
     * @return boolean
     */
    public boolean primaryEmailVerified() {
        for (Email email : getEmail()) {
            if (email.isPrimary() && email.isVerified()) {
                return true;
            }
        }
        return false;
    }

    /**
     * covenience method
     * @return boolean
     */
    public boolean anyEmailVerified() {
        for (Email email : getEmail()) {
            if (email.isVerified()) {
                return true;
            }
        }
        return false;
    }

    
    /**
     * Gets the value of the address property.
     * 
     * @return possible object is {@link org.orcid.jaxb.model.message.Address }
     *
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Sets the value of the address property.
     *
     * @param value
     *            allowed object is {@link org.orcid.jaxb.model.message.Address }
     * 
     */
    public void setAddress(Address value) {
        this.address = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ContactDetails other = (ContactDetails) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        return true;
    }

}
