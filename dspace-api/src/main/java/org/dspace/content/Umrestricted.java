/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import javax.persistence.Entity;


import java.io.Serializable;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

import javax.persistence.*;
import java.util.UUID;

//import org.dspace.content.service.UmrestrictedService;
//import org.dspace.content.factory.ContentServiceFactory;



@Entity
@Table(name = "umrestricted")
public class Umrestricted implements Serializable, ReloadableEntity<Integer>{
                      
    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="umrestricted_seq")
    @SequenceGenerator(name="umrestricted_seq", sequenceName="umrestricted_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "item_id", unique = false)
    private String item_id;
 
    @Column(name = "release_date", unique = false)
    private String release_date;

    protected Umrestricted()
    {

    }

//    @Transient
//    private transient UmrestrictedService umrestrictedService;


    public Integer getID() {
        return id;
    }


    public String getItemId() {
        return item_id;
    }

    public String getReleaseDate() {
        return release_date;
    }

//    public UmrestrictedService getItemService() {
//        if (umrestrictedService == null) {
//            umrestrictedService = ContentServiceFactory.getInstance().getUmrestrictedService();
//        }
//        return umrestrictedService;
//    }


}
