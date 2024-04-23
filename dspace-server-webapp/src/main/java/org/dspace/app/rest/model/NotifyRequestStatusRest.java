/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.dspace.app.ldn.model.RequestStatus;
import org.dspace.app.rest.NotifyRequestStatusRestController;


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

    private static final long serialVersionUID = 1L;
    public static final String CATEGORY = RestAddressableModel.LDN;
    public static final String NAME = "notifyrequests";
    public static final String PLURAL_NAME = "notifyrequests";

    private List<RequestStatus> notifyStatus;
    private UUID itemuuid;

    public NotifyRequestStatusRest(NotifyRequestStatusRest instance) {
        this.notifyStatus = instance.getNotifyStatus();
    }

    public NotifyRequestStatusRest() {
        this.notifyStatus = new ArrayList<RequestStatus>();
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
        return NotifyRequestStatusRestController.class;
    }

    public List<RequestStatus> getNotifyStatus() {
        return notifyStatus;
    }

    public void setNotifyStatus(List<RequestStatus> notifyStatus) {
        this.notifyStatus = notifyStatus;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

}
