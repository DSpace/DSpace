/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

import javax.persistence.*;

/**
 * DOI identifiers.
 *
 * @author Pascal-Nicolas Becker
 */
@Entity
@Table(name = "Doi" )
public class DOI
        implements Identifier, ReloadableEntity<Integer>
{
    public static final String SCHEME = "doi:";

    public static final String RESOLVER = "http://dx.doi.org";

    @Id
    @Column(name="doi_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="doi_seq")
    @SequenceGenerator(name="doi_seq", sequenceName="doi_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "doi", unique = true, length = 256)
    private String doi;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dspace_object")
    private DSpaceObject dSpaceObject;

    @Column(name = "resource_type_id")
    private Integer resourceTypeId;

    @Column(name = "status")
    private Integer status;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.identifier.service.DOIService#create(Context)}
     *
     */
    protected DOI()
    {
    }

    public Integer getID() {
        return id;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public DSpaceObject getDSpaceObject() {
        return dSpaceObject;
    }
    
    public void setDSpaceObject(DSpaceObject dSpaceObject) {
        this.dSpaceObject = dSpaceObject;
        
        // set the Resource Type if the Object is not null
        // don't set object type null, we want to know to which resource type
        // the DOI was assigned to if the Object is deleted.
        if (dSpaceObject != null)
        {
            this.resourceTypeId = dSpaceObject.getType();
        }
    }
    
    /**
     * returns the resource type of the DSpaceObject the DOI is or was assigned 
     * to. The resource type is set automatically when a DOI is assigned to a 
     * DSpaceObject, using {@link #setDSpaceObject(org.dspace.content.DSpaceObject) }.
     * @return the integer constant of the DSO, see {@link org.dspace.core.Constants#Constants Constants}
     */
    public Integer getResourceTypeId() {
        return this.resourceTypeId;
    }
    
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
