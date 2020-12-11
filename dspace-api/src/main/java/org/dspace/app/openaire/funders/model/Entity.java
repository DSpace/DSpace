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
import javax.xml.bind.annotation.XmlElement;
 
public class Entity {
    @XmlElement(name="project", namespace="http://namespace.openaire.eu/oaf") 
    private Project project;

    public Project getProject() {
        return project;
    }
    
}
