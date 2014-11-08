/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import org.dspace.content.DSpaceObject;

import javax.persistence.*;

/**
 * DOI identifiers.
 *
 * @author Pascal-Nicolas Becker
 */
@Entity
@Table(name = "Doi", schema = "public" )
public class DOI
        implements Identifier
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

    @Column(name = "status")
    private Integer status;

    public Integer getId() {
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
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
