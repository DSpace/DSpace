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
 *     DetailType key = eventDetail.getDetailType();
 *     Object detail = eventDetail.getDetailObject();
 * </pre>
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class EventDetail {

    private DetailType detailType;

    private Object detailObject;

    public EventDetail(DetailType detailType, Object detailObject) {
        setDetailType(detailType);
        setDetailObject(detailObject);
    }

    public DetailType getDetailType() {
        return detailType;
    }

    private void setDetailType(DetailType detailType) {
        this.detailType = detailType;
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
        return detailType == that.detailType && Objects.equals(detailObject, that.detailObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detailType, detailObject);
    }
}
