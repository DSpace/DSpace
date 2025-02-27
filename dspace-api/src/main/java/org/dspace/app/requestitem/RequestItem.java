/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.util.Calendar;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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

    @Column(name = "access_token", unique = true, length = 48)
    private String access_token = null;

    @Column(name = "access_period")
    private int access_period;

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

    public void setAllfiles(boolean allfiles) {
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

    /**
     * @return A unique token to be used by the requester when granted access to the resource, which
     * can be emailed upon approval
     */
    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    /**
     * @return Access period in seconds, for the length of time (from decision date) this granted access will be valid
     */
    public int getAccess_period() {
        return access_period;
    }

    public void setAccess_period(int access_period) {
        this.access_period = access_period;
    }

    /**
     * Sanitize personal information and the approval token, to be used when returning a RequestItem
     * to Angular, especially for users clicking on the secure link
     */
    public void sanitizePersonalData() {
        setReqEmail("sanitized");
        setReqName("sanitized");
        setReqMessage("sanitized");
        // Even though [approval] token is not a name, it can be used to access the original object
        setToken("sanitized");
    }

    public Date getAccessEndDate() {
        if (access_period > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(decision_date);
            calendar.add(Calendar.SECOND, access_period);
            return calendar.getTime();
        }
        return null;
    }

    /**
     * Calculate whether the access period for this item request is current, or has ended
     *
     * @return true if the access period for this item request is current (or 0 (forever)), false if expired
     */
    public boolean accessPeriodCurrent() {
        if (access_period == 0) {
            return true;
        } else if (access_period > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(decision_date);
            calendar.add(Calendar.SECOND, access_period);
            // Return boolean result of "access period end date is AFTER now"
            return calendar.getTime().after(new Date());
        }
        // By default return false
        return false;
    }
}
