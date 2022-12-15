/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * Object representing an Item Request.
 */
@Entity
@Table(name = "requestitem")
public class RequestItem implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "requestitem_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "requestitem_seq")
    @SequenceGenerator(name = "requestitem_seq", sequenceName = "requestitem_seq", allocationSize = 1)
    private int requestitem_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_id")
    private Bitstream bitstream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "request_email", length = 64)
    private String reqEmail;

    @Column(name = "request_name", length = 64)
    private String reqName;

    @Column(name = "request_message", columnDefinition = "text")
    private String reqMessage;

    @Column(name = "token", unique = true, length = 48)
    private String token;

    @Column(name = "allfiles")
    private boolean allfiles;

    @Column(name = "decision_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date decision_date = null;

    @Column(name = "expires")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expires = null;

    @Column(name = "request_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date request_date = null;

    @Column(name = "accept_request")
    private boolean accept_request;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.app.requestitem.service.RequestItemService#createRequest(
     * Context, Bitstream, Item, boolean, String, String, String)}
     */
    protected RequestItem() {
    }

    @Override
    public Integer getID() {
        return requestitem_id;
    }

    void setAllfiles(boolean allfiles) {
        this.allfiles = allfiles;
    }

    /**
     * @return {@code true} if all of the Item's files are requested.
     */
    public boolean isAllfiles() {
        return allfiles;
    }

    void setReqMessage(String reqMessage) {
        this.reqMessage = reqMessage;
    }

    /**
     * @return a message from the requester.
     */
    public String getReqMessage() {
        return reqMessage;
    }

    void setReqName(String reqName) {
        this.reqName = reqName;
    }

    /**
     * @return Human-readable name of the user requesting access.
     */
    public String getReqName() {
        return reqName;
    }

    void setReqEmail(String reqEmail) {
        this.reqEmail = reqEmail;
    }

    /**
     * @return address of the user requesting access.
     */
    public String getReqEmail() {
        return reqEmail;
    }

    void setToken(String token) {
        this.token = token;
    }

    /**
     * @return a unique request identifier which can be emailed.
     */
    public String getToken() {
        return token;
    }

    void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    void setBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    public Bitstream getBitstream() {
        return bitstream;
    }

    public Date getDecision_date() {
        return decision_date;
    }

    public void setDecision_date(Date decision_date) {
        this.decision_date = decision_date;
    }

    public boolean isAccept_request() {
        return accept_request;
    }

    public void setAccept_request(boolean accept_request) {
        this.accept_request = accept_request;
    }

    public Date getExpires() {
        return expires;
    }

    void setExpires(Date expires) {
        this.expires = expires;
    }

    public Date getRequest_date() {
        return request_date;
    }

    void setRequest_date(Date request_date) {
        this.request_date = request_date;
    }
}
