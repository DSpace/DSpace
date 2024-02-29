/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.dspace.qaevent.service.dto.CorrectionTypeMessageDTO;
import org.dspace.qaevent.service.dto.NotifyMessageDTO;
import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.dspace.util.RawJsonDeserializer;

/**
 * This class represent the Quality Assurance broker data as loaded in our solr
 * qaevent core
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class QAEvent {
    public static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
        'f' };
    public static final String ACCEPTED = "accepted";
    public static final String REJECTED = "rejected";
    public static final String DISCARDED = "discarded";

    public static final String OPENAIRE_SOURCE = "openaire";
    public static final String DSPACE_USERS_SOURCE = "DSpaceUsers";
    public static final String COAR_NOTIFY_SOURCE = "coar-notify";

    private String source;

    private String eventId;
    /**
     * contains the targeted dspace object,
     * ie: oai:www.openstarts.units.it:123456789/1120 contains the handle
     * of the DSpace pbject in its final part 123456789/1120
     * */
    private String originalId;

    /**
     * evaluated with the targeted dspace object id
     * 
     * */
    private String target;

    private String related;

    private String title;

    private String topic;

    private double trust;

    @JsonDeserialize(using = RawJsonDeserializer.class)
    private String message;

    private Date lastUpdate;

    private String status = "PENDING";

    public QAEvent() {}

    public QAEvent(String source, String originalId, String target, String title,
        String topic, double trust, String message, Date lastUpdate) {
        super();
        this.source = source;
        this.originalId = originalId;
        this.target = target;
        this.title = title;
        this.topic = topic;
        this.trust = trust;
        this.message = message;
        this.lastUpdate = lastUpdate;
        try {
            computedEventId();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public double getTrust() {
        return trust;
    }

    public void setTrust(double trust) {
        this.trust = trust;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEventId() {
        if (eventId == null) {
            try {
                computedEventId();
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setRelated(String related) {
        this.related = related;
    }

    public String getRelated() {
        return related;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source != null ? source : OPENAIRE_SOURCE;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /*
     * DTO constructed via Jackson use empty constructor. In this case, the eventId
     * must be compute on the get method. This method create a signature based on
     * the event fields and store it in the eventid attribute.
     */
    private void computedEventId() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digester = MessageDigest.getInstance("MD5");
        String dataToString = "source=" + source + ",originalId=" + originalId + ", title=" + title + ", topic="
            + topic + ", trust=" + trust + ", message=" + message;
        digester.update(dataToString.getBytes("UTF-8"));
        byte[] signature = digester.digest();
        char[] arr = new char[signature.length << 1];
        for (int i = 0; i < signature.length; i++) {
            int b = signature[i];
            int idx = i << 1;
            arr[idx] = HEX_DIGITS[(b >> 4) & 0xf];
            arr[idx + 1] = HEX_DIGITS[b & 0xf];
        }
        eventId = new String(arr);

    }

    public Class<? extends QAMessageDTO> getMessageDtoClass() {
        switch (getSource()) {
            case OPENAIRE_SOURCE:
                return OpenaireMessageDTO.class;
            case COAR_NOTIFY_SOURCE:
                return NotifyMessageDTO.class;
            case DSPACE_USERS_SOURCE:
                return CorrectionTypeMessageDTO.class;
            default:
                throw new IllegalArgumentException("Unknown event's source: " + getSource());
        }
    }

}
