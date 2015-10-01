/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import org.dspace.content.DSpaceObject;

import javax.persistence.*;

/**
 * Database entity representation of the handle table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name="handle")
public class Handle {

    @Id
    @Column(name="handle_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="handle_seq")
    @SequenceGenerator(name="handle_seq", sequenceName="handle_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "handle", unique = true)
    private String handle;

    @OneToOne(fetch =  FetchType.EAGER)
    @JoinColumn(name = "resource_id")
    private DSpaceObject dso;

    @Column(name = "resource_type_id")
    private Integer resourceTypeId;

    public Integer getId() {
        return id;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public void setDSpaceObject(DSpaceObject dso) {
        this.dso = dso;
    }

    public DSpaceObject getDSpaceObject() {
        return dso;
    }

    public void setResourceTypeId(Integer resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    public Integer getResourceTypeId() {
        return resourceTypeId;
    }
}
