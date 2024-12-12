/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {
    "itemuuid",
    "notifyStatus"
})

/**
 * item requests of LDN messages of type
 *
 *    "Offer", "coar-notify:EndorsementAction"
 *    "Offer", "coar-notify:IngestAction"
 *    "Offer", "coar-notify:ReviewAction"
 * 
 * and their acknowledgements - if any
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science dot it)
 */
public class NotifyRequestStatus extends Base {

    private UUID itemUuid;

    private List<RequestStatus> notifyStatus;

    public NotifyRequestStatus() {
        super();
        this.notifyStatus = new ArrayList<RequestStatus>();
    }

    public UUID getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(UUID itemUuid) {
        this.itemUuid = itemUuid;
    }

    public void addRequestStatus(RequestStatus rs) {
        this.notifyStatus.add(rs);
    }

    public List<RequestStatus> getNotifyStatus() {
        return notifyStatus;
    }

    public void setNotifyStatus(List<RequestStatus> notifyStatus) {
        this.notifyStatus = notifyStatus;
    }

}