/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.dspace.app.rest.RestResourceController;

/**
 * Represent a user's request for a copy of an Item.
 * @see org.dspace.app.requestitem
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@LinksRest(links = {
    @LinkRest(name = "bitstream", linkClass = BitstreamRest.class, method = "getUuid"),
    @LinkRest(name = "item", linkClass = ItemRest.class, method = "getUuid")
})
public class RequestItemRest
        extends BaseObjectRest<Integer> {
    public static final String NAME = "copyrequest";

    public static final String CATEGORY = RestAddressableModel.TOOLS;

    protected String bitstream_id;
    protected Date decisionDate;
    protected Date expires;
    protected String item_id;
    protected String reqEmail;
    protected String reqMessage;
    protected String reqName;
    protected Date requestDate;
    protected String token;
    protected boolean acceptRequest;
    protected boolean allfiles;

    /**
     * @return the bitstream requested.
     */
    public String getBitstream_id() {
        return bitstream_id;
    }

    /**
     * @param bitstream_id the bitstream requested.
     */
    public void setBitstream_id(String bitstream_id) {
        this.bitstream_id = bitstream_id;
    }

    /**
     * @return the decisionDate
     */
    public Date getDecision_date() {
        return decisionDate;
    }

    /**
     * @param decided the decisionDate to set
     */
    public void setDecision_date(Date decided) {
        this.decisionDate = decided;
    }

    /**
     * @return the expires
     */
    public Date getExpires() {
        return expires;
    }

    /**
     * @param expires the expires to set
     */
    public void setExpires(Date expires) {
        this.expires = expires;
    }

    /**
     * @return the item requested.
     */
    public String getItem_id() {
        return item_id;
    }

    /**
     * @param item_id the item requested.
     */
    public void setItem_id(String item_id) {
        this.item_id = item_id;
    }

    /**
     * @return the email address of the requester.
     */
    public String getRequest_email() {
        return reqEmail;
    }

    /**
     * @param email the email address of the requester.
     */
    public void setRequest_email(String email) {
        this.reqEmail = email;
    }

    /**
     * @return the requester's message.
     */
    public String getRequest_message() {
        return reqMessage;
    }

    /**
     * @param message the requester's message.
     */
    public void setRequest_message(String message) {
        this.reqMessage = message;
    }

    /**
     * @return the requester's name.
     */
    public String getRequest_name() {
        return reqName;
    }

    /**
     * @param name the requester's name.
     */
    public void setRequest_name(String name) {
        this.reqName = name;
    }

    /**
     * @return the requestDate
     */
    public Date getRequest_date() {
        return requestDate;
    }

    /**
     * @param requested the requestDate to set
     */
    public void setRequest_date(Date requested) {
        this.requestDate = requested;
    }

    /**
     * @return the token which identifies this request.
     */
    public String getToken() {
        return token;
    }

    /**
     * @param token the token which identifies this request.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @return true if the request has been accepted.
     */
    public boolean isAccept_request() {
        return acceptRequest;
    }

    /**
     * @param accepted true if the request has been accepted.
     */
    public void setAccept_request(boolean accepted) {
        this.acceptRequest = accepted;
    }

    /**
     * @return true if the request is for all files in the item.
     */
    public boolean isAllfiles() {
        return allfiles;
    }

    /**
     * @param allfiles true requesting all of the item's files.
     */
    public void setAllfiles(boolean allfiles) {
        this.allfiles = allfiles;
    }

    /*
     * Common REST object methods.
     */

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return super.getTypePlural();
    }
}
