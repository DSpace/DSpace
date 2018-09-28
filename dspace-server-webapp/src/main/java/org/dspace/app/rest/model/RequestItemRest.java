/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Represent a user's request for a copy of an Item.
 * @see org.dspace.app.requestitem
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Component
public class RequestItemRest
        extends BaseObjectRest<Integer> {
    public static final String NAME = "copyrequest";

    public static final String CATEGORY = RestAddressableModel.TOOLS;

    @Autowired(required = true)
    private BitstreamConverter bitstreamConverter;

    @Autowired(required = true)
    private ItemConverter itemConverter;

    protected BitstreamRest bitstream;
    protected Date decisionDate;
    protected Date expires;
    protected ItemRest item;
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
    @LinkRest
    @JsonIgnore
    public BitstreamRest getBitstream() {
        return bitstream;
    }

    /**
     * @param bitstream the bitstream requested.
     */
    public void setBitstream(BitstreamRest bitstream) {
        this.bitstream = bitstream;
    }

    /**
     * @return the decisionDate
     */
    public Date getDecisionDate() {
        return decisionDate;
    }

    /**
     * @param decided the decisionDate to set
     */
    public void setDecisionDate(Date decided) {
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
    @LinkRest
    @JsonIgnore
    public ItemRest getItem() {
        return item;
    }

    /**
     * @param item the item requested.
     */
    public void setItem(ItemRest item) {
        this.item = item;
    }

    /**
     * @return the email address of the requester.
     */
    public String getReqEmail() {
        return reqEmail;
    }

    /**
     * @param email the email address of the requester.
     */
    public void setReqEmail(String email) {
        this.reqEmail = email;
    }

    /**
     * @return the requester's message.
     */
    public String getReqMessage() {
        return reqMessage;
    }

    /**
     * @param message the requester's message.
     */
    public void setReqMessage(String message) {
        this.reqMessage = message;
    }

    /**
     * @return the requester's name.
     */
    public String getReqName() {
        return reqName;
    }

    /**
     * @param name the requester's name.
     */
    public void setReqName(String name) {
        this.reqName = name;
    }

    /**
     * @return the requestDate
     */
    public Date getRequestDate() {
        return requestDate;
    }

    /**
     * @param requested the requestDate to set
     */
    public void setRequestDate(Date requested) {
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
    public boolean isAcceptRequest() {
        return acceptRequest;
    }

    /**
     * @param accepted true if the request has been accepted.
     */
    public void setAcceptRequest(boolean accepted) {
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
