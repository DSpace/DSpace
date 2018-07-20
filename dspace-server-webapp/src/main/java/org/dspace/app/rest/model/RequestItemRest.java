/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;

/**
 * Represent a user's request for a copy of an Item.
 * @see org.dspace.app.requestitem
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class RequestItemRest
        extends BaseObjectRest {
    public static final String NAME = "copy_request";

    public static final String CATEGORY = RestAddressableModel.COPY_REQUEST;

    protected BitstreamRest bitstream;
    protected Date decision_date;
    protected Date expires;
    protected ItemRest item;
    protected String req_email;
    protected String req_message;
    protected String req_name;
    protected Date request_date;
    protected String token;
    protected boolean accept_request;
    protected boolean allfiles;

    /**
     * @return the bitstream
     */
    public BitstreamRest getBitstream() {
        return bitstream;
    }

    /**
     * @param bitstream the bitstream to set
     */
    public void setBitstream(BitstreamRest bitstream) {
        this.bitstream = bitstream;
    }

    /**
     * @return the decision_date
     */
    public Date getDecision_date() {
        return decision_date;
    }

    /**
     * @param decision_date the decision_date to set
     */
    public void setDecision_date(Date decision_date) {
        this.decision_date = decision_date;
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
     * @return the item
     */
    public ItemRest getItem() {
        return item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(ItemRest item) {
        this.item = item;
    }

    /**
     * @return the req_email
     */
    public String getReq_email() {
        return req_email;
    }

    /**
     * @param req_email the req_email to set
     */
    public void setReq_email(String req_email) {
        this.req_email = req_email;
    }

    /**
     * @return the req_message
     */
    public String getReq_message() {
        return req_message;
    }

    /**
     * @param req_message the req_message to set
     */
    public void setReq_message(String req_message) {
        this.req_message = req_message;
    }

    /**
     * @return the req_name
     */
    public String getReq_name() {
        return req_name;
    }

    /**
     * @param req_name the req_name to set
     */
    public void setReq_name(String req_name) {
        this.req_name = req_name;
    }

    /**
     * @return the request_date
     */
    public Date getRequest_date() {
        return request_date;
    }

    /**
     * @param request_date the request_date to set
     */
    public void setRequest_date(Date request_date) {
        this.request_date = request_date;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @return the accept_request
     */
    public boolean isAccept_request() {
        return accept_request;
    }

    /**
     * @param accept_request the accept_request to set
     */
    public void setAccept_request(boolean accept_request) {
        this.accept_request = accept_request;
    }

    /**
     * @return all files?
     */
    public boolean isAllfiles() {
        return allfiles;
    }

    /**
     * @param allfiles all files?
     */
    public void setAllfiles(boolean allfiles) {
        this.allfiles = allfiles;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return super.getTypePlural();
    }
}
