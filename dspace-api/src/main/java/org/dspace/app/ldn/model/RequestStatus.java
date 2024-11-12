/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

/**
 * Information about the Offer and Acknowledgements targeting a specified Item
 * 
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.com)
 */
public class RequestStatus {

    private String serviceName;
    private String serviceUrl;
    private String offerType;
    private NotifyRequestStatusEnum status;

    public String getServiceName() {
        return serviceName;
    }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    public String getServiceUrl() {
        return serviceUrl;
    }
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    public NotifyRequestStatusEnum getStatus() {
        return status;
    }
    public void setStatus(NotifyRequestStatusEnum status) {
        this.status = status;
    }
    public String getOfferType() {
        return offerType;
    }
    public void setOfferType(String offerType) {
        this.offerType = offerType;
    }

}
