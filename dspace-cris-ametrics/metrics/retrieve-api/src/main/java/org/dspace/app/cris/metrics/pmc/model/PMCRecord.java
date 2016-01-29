/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.pmc.model;

import it.cilea.osd.common.model.Identifiable;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "cris_pmc_record")
public class PMCRecord implements Identifiable
{
    /**
     * The pmcID of the record. It is named <code>id</code> as required by the
     * Identifiable interface.
     */
    @Id
    @Column(name = "pmcID")
    private Integer id;

    public List<Integer> getPubmedIDs()
    {
        return pubmedIDs;
    }

    public void setPubmedIDs(List<Integer> pubmedIDs)
    {
        this.pubmedIDs = pubmedIDs;
    }

    public List<String> getHandles()
    {
        return handles;
    }

    public void setHandles(List<String> handles)
    {
        this.handles = handles;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SELECT)
    @CollectionTable(name = "cris_pmc_record_pubmedids", joinColumns = @JoinColumn(name = "cris_pmc_record_pmcid"))
    @Column(name = "element")
    private List<Integer> pubmedIDs;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SELECT)
    @CollectionTable(name = "cris_pmc_record_handles", joinColumns = @JoinColumn(name = "cris_pmc_record_pmcid"))
    @Column(name = "element")
    private List<String> handles;

    @Type(type="org.hibernate.type.StringClobType")
    @Column(name = "title")
    private String title;

    @Type(type="org.hibernate.type.StringClobType")
    @Column(name = "authors")
    private String authors;

    @Type(type="org.hibernate.type.StringClobType")
    @Column(name = "publicationnote")
    private String publicationNote;

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getAuthors()
    {
        return authors;
    }

    public void setAuthors(String authors)
    {
        this.authors = authors;
    }

    public String getPublicationNote()
    {
        return publicationNote;
    }

    public void setPublicationNote(String publicationNote)
    {
        this.publicationNote = publicationNote;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
}
