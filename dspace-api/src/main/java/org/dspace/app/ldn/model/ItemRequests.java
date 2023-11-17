/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {
    "itemuuid",
    "endorsements",
    "ingests",
    "reviews"
})

/**
 * item requests of LDN messages of type
 *
 *    "Offer", "coar-notify:EndorsementAction"
 *    "Offer", "coar-notify:IngestAction"
 *    "Offer", "coar-notify:ReviewAction"
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science dot it)
 */
public class ItemRequests extends Base {

    private ItemRequest endorsements;
    private ItemRequest ingests;
    private ItemRequest reviews;
    private UUID itemUuid;

    public ItemRequests() {
        super();
    }

    public ItemRequest getEndorsements() {
        return endorsements;
    }

    public void setEndorsements(ItemRequest endorsements) {
        this.endorsements = endorsements;
    }

    public ItemRequest getIngests() {
        return ingests;
    }

    public void setIngests(ItemRequest ingests) {
        this.ingests = ingests;
    }

    public ItemRequest getReviews() {
        return reviews;
    }

    public void setReviews(ItemRequest reviews) {
        this.reviews = reviews;
    }

    public UUID getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(UUID itemUuid) {
        this.itemUuid = itemUuid;
    }

    public class ItemRequest {
        Integer count;
        Boolean accepted;
        Boolean rejected;
        Boolean tentative;
    }
}


