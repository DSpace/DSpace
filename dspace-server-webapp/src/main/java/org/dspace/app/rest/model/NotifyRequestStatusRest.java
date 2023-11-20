/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;
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
    "notifyStatus",
    "itemuuid"
})
public class NotifyRequestStatusRest extends RestAddressableModel {
    public static final String CATEGORY = RestAddressableModel.LDN;
    public static final String NAME = "ldnitemservice";
    public static final String GET_ITEM_REQUESTS = "getItemRequests";

    private List<NotifyRequestsStatus> notifyStatus;
    private UUID itemuuid;

    public NotifyRequestStatusRest() {
        super();
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

enum NotifyStatus {
    REJECTED, ACCEPTED, REQUESTED
}

class NotifyRequestsStatus {
    String serviceName;
    String serviceUrl;
    NotifyStatus status;
}
