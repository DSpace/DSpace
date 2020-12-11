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
import javax.xml.bind.annotation.XmlRootElement;
 
@XmlRootElement(name = "header")

public class Header {
    @XmlElement(name="query")
    private String query;
    
    @XmlElement(name="locale")
    private String locale;
    
    @XmlElement(name="total")
    private String total;

    public String getQuery() {
        return query;
    }

    public String getLocale() {
        return locale;
    }

    public String getTotal() {
        return total;
    }
    
    


}
