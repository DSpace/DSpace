/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.openaire.funders.model;

/**
 *
 * @author dpie
 */
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.*;
 
@XmlType
public class ResultHeader {

    @XmlElement(name="objIdentifier", namespace="http://www.driver-repository.eu/namespace/dri")
    private String driObjectIdentifier;
    @XmlElement(name="dateOfCollection", namespace="http://www.driver-repository.eu/namespace/dri")
    private String dateOfCollection;
    @XmlElement(name="dateOfTransformation", namespace="http://www.driver-repository.eu/namespace/dri")
    private String dateOfTransformation;
    
                            
    public String getDriObjectIdentifier() {
        return driObjectIdentifier;
    }

    public String getDateOfCollection() {
        return dateOfCollection;
    }

    public String getDateOfTransformation() {
        return dateOfTransformation;
    }

    
    
    
}
