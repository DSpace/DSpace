/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.pmc.model;

import it.cilea.osd.common.core.HasTimeStampInfo;
import it.cilea.osd.common.core.TimeStampInfo;
import it.cilea.osd.common.model.Identifiable;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

//WARNING REFACTORING 27/11/2015 we decide to mantain this data structure and move only the metrics count in the dedicated table 
@Entity
@Table(name = "cris_pmc_citation")
@NamedQueries({
        @NamedQuery(name = "PMCCitation.findAll", query = "from PMCCitation order by id"),
        @NamedQuery(name = "PMCCitation.uniqueCitationByItemID", query = "select cit from PMCCitation cit join cit.itemIDs itemID where itemID = ?") })
public class PMCCitation implements Identifiable, HasTimeStampInfo
{
    @Id
    @Column(name = "pubmedID")
    private Integer id;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SELECT)
	@CollectionTable(name = "cris_pmc_citation_itemids", joinColumns = @JoinColumn(name = "cris_pmc_citation_pubmedid"))
	@Column(name = "element")
    private List<Integer> itemIDs;

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @CollectionTable(name = "cris_pmc_citation2record", joinColumns = @JoinColumn(name = "cris_pmc_citation_pubmedid"))
    @Column(name = "pmcrecords_pmcid")
    private List<PMCRecord> pmcRecords;

    /** timestamp info for creation and last modify */
    @Embedded
    private TimeStampInfo timeStampInfo;

    public TimeStampInfo getTimeStampInfo()
    {
        if (timeStampInfo == null)
        {
            timeStampInfo = new TimeStampInfo();
        }
        return timeStampInfo;
    }

    public void setTimeStampInfo(TimeStampInfo timeStampInfo)
    {
        this.timeStampInfo = timeStampInfo;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer pubmedID)
    {
        this.id = pubmedID;
    }

    public List<Integer> getItemIDs()
    {
        return itemIDs;
    }

    public void setItemIDs(List<Integer> itemIDs)
    {
        this.itemIDs = itemIDs;
    }

    public List<PMCRecord> getPmcRecords()
    {
        return pmcRecords;
    }

    public void setPmcRecords(List<PMCRecord> pmcRecords)
    {
        this.pmcRecords = pmcRecords;
    }

}
