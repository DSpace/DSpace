/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import java.util.Objects;

/**
 * Represents the details of an event, including a key and an associated object.
 * This class is used to encapsulate specific details about an event in the system.
 *
 * <p>Each event detail consists of:
 * <ul>
 *   <li>A {@link DetailType} key that identifies the type of detail.</li>
 *   <li>An associated object that provides additional information about the detail.</li>
 * </ul>
 *
 * <p>The class provides methods to retrieve the key and the associated object,
 * as well as overrides for {@code equals} and {@code hashCode} to ensure proper
 * comparison and hashing based on the key and object.
 *
 * <p>Example usage:
 * <pre>
 *     EventDetail eventDetail = new EventDetail(DetailType.SOME_TYPE, someObject);
 *     DetailType key = eventDetail.getDetailKey();
 *     Object detail = eventDetail.getDetailObject();
 * </pre>
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class EventDetail {

    private DetailType detailKey;

    private Object detailObject;

    public EventDetail(DetailType detailKey, Object detailObject) {
        setDetailKey(detailKey);
        setDetailObject(detailObject);
    }

    public DetailType getDetailKey() {
        return detailKey;
    }

    private void setDetailKey(DetailType detailKey) {
        this.detailKey = detailKey;
    }

    public Object getDetailObject() {
        return detailObject;
    }

    private void setDetailObject(Object detailObject) {
        this.detailObject = detailObject;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventDetail that = (EventDetail) o;
        return detailKey == that.detailKey && Objects.equals(detailObject, that.detailObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detailKey, detailObject);
    }
}
