/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * @author Alexey Maslov
 */
@Entity
@Table(name = "harvested_item")
public class HarvestedItem implements ReloadableEntity<Integer> {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "harvested_item_seq")
    @SequenceGenerator(name = "harvested_item_seq", sequenceName = "harvested_item_seq", allocationSize = 1)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", unique = true)
    private Item item;

    @Column(name = "last_harvested", columnDefinition = "timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastHarvested;

    @Column(name = "oai_id")
    private String oaiId;


    /**
     * Protected constructor, create object using:
     * {@link org.dspace.harvest.service.HarvestedItemService#create(Context, Item, String)}
     */
    protected HarvestedItem() {
    }

    @Override
    public Integer getID() {
        return id;
    }

    void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    // FIXME: duplicate below?
    void setOaiId(String oaiId) {
        this.oaiId = oaiId;
    }

    /**
     * Get the oai_id associated with this item
     *
     * @return itemOaiID item's OAI identifier
     */
    public String getOaiID() {
        return oaiId;
    }

    /**
     * Set the oai_id associated with this item
     *
     * @param itemOaiID item's OAI identifier
     */
    public void setOaiID(String itemOaiID) {
        this.oaiId = itemOaiID;
    }


    public void setHarvestDate(Date date) {
        if (date == null) {
            date = new Date();
        }
        lastHarvested = date;
    }

    public Date getHarvestDate() {
        return lastHarvested;
    }

}
