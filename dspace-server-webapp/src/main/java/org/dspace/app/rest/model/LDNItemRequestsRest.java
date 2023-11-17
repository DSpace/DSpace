/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.dspace.app.rest.RestResourceController;

/**
 * Rest entity for LDN requests targeting items
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science dot it)
 */
@JsonPropertyOrder(value = {
    "itemuuid",
    "endorsements",
    "ingests",
    "reviews"
})
public class LDNItemRequestsRest extends BaseObjectRest<Integer>  {
    public static final String CATEGORY = RestAddressableModel.LDN;
    public static final String NAME = "ldnitemservice";
    public static final String GET_ITEM_REQUESTS = "getItemRequests";

    private ItemRequest endorsements;
    private ItemRequest ingests;
    private ItemRequest reviews;
    private UUID itemuuid;

    public LDNItemRequestsRest() {
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

    public UUID getItemuuid() {
        return itemuuid;
    }

    public void setItemuuid(UUID itemuuid) {
        this.itemuuid = itemuuid;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public Class getController() {
        return RestResourceController.class;
    }

}

class ItemRequest {
    Integer count;
    Boolean accepted;
    Boolean rejected;
    Boolean tentative;
}
