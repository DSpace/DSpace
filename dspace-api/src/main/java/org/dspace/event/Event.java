/*
 * Event.java
 * 
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.event;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ObjectIdentifierMint;

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An Event object represents a single action that changed one object in the
 * DSpace data model. An "atomic" action at the application or business-logic
 * API level may spawn many of these events.
 * <p>
 * This class includes tools to help set and use the contents of the event. Note
 * that it describes DSpace data object types in two ways: by the type
 * identifiers in the Constants class, and also by an Event-specific bitmask
 * (used by its internal filters). All public API calls use the Constants
 * version of the data model types.
 * <p>
 * Note that the type of the event itself is actually descriptive of the
 * <em>action</em> it performs: ADD, MODIFY, etc. The most significant
 * elements of the event are:
 * <p>
 * <br> - (Action) Type <br> - Subject -- DSpace object to which the action
 * applies, e.g. the Collection to which an ADD adds a member. <br> - Object --
 * optional, when present it is the other object effected by an action, e.g. the
 * Item ADDed to a Collection by an ADD. <br> - detail -- a textual summary of
 * what changed, content and its significance varies by the combination of
 * action and subject type. <br> - timestamp -- exact millisecond timestamp at
 * which event was logged.
 * 
 * @version $Revision$
 */
public class Event implements Serializable
{
    /** ---------- Constants ------------- * */

    /** Event (Action) types */
    public static final int CREATE = 1 << 0; // create new object

    public static final int MODIFY = 1 << 1; // modify object

    public static final int MODIFY_METADATA = 1 << 2; // modify object

    public static final int ADD = 1 << 3; // add content to container

    public static final int REMOVE = 1 << 4; // remove content from container

    public static final int DELETE = 1 << 5; // destroy object

    /** Index of filter parts in their array: */
    public static final int SUBJECT_MASK = 0; // mask of subject types

    public static final int EVENT_MASK = 1; // mask of event type

    // XXX NOTE: keep this up to date with any changes to event (action) types.
    private static final String eventTypeText[] = { "CREATE", "MODIFY",
            "MODIFY_METADATA", "ADD", "REMOVE", "DELETE" };

    /** XXX NOTE: These constants must be kept synchronized * */
    /** XXX NOTE: with ALL_OBJECTS_MASK *AND* objTypeToMask hash * */
    private static final int NONE = 0;

    private static final int BITSTREAM = 1 << Constants.BITSTREAM; // 0

    private static final int BUNDLE = 1 << Constants.BUNDLE; // 1

    private static final int ITEM = 1 << Constants.ITEM; // 2

    private static final int COLLECTION = 1 << Constants.COLLECTION; // 3

    private static final int COMMUNITY = 1 << Constants.COMMUNITY; // 4

    private static final int SITE = 1 << Constants.SITE; // 5

    private static final int GROUP = 1 << Constants.GROUP; // 6

    private static final int EPERSON = 1 << Constants.EPERSON; // 7

    private static final int ALL_OBJECTS_MASK = BITSTREAM | BUNDLE | ITEM
            | COLLECTION | COMMUNITY | SITE | GROUP | EPERSON;

    private static Map<Integer, Integer> objTypeToMask = new HashMap<Integer, Integer>();

    private static Map<Integer, Integer> objMaskToType = new HashMap<Integer, Integer>();
    static
    {
        objTypeToMask.put(new Integer(Constants.BITSTREAM), new Integer(
                BITSTREAM));
        objMaskToType.put(new Integer(BITSTREAM), new Integer(
                Constants.BITSTREAM));

        objTypeToMask.put(new Integer(Constants.BUNDLE), new Integer(BUNDLE));
        objMaskToType.put(new Integer(BUNDLE), new Integer(Constants.BUNDLE));

        objTypeToMask.put(new Integer(Constants.ITEM), new Integer(ITEM));
        objMaskToType.put(new Integer(ITEM), new Integer(Constants.ITEM));

        objTypeToMask.put(new Integer(Constants.COLLECTION), new Integer(
                COLLECTION));
        objMaskToType.put(new Integer(COLLECTION), new Integer(
                Constants.COLLECTION));

        objTypeToMask.put(new Integer(Constants.COMMUNITY), new Integer(
                COMMUNITY));
        objMaskToType.put(new Integer(COMMUNITY), new Integer(
                Constants.COMMUNITY));

        objTypeToMask.put(new Integer(Constants.SITE), new Integer(SITE));
        objMaskToType.put(new Integer(SITE), new Integer(Constants.SITE));

        objTypeToMask.put(new Integer(Constants.GROUP), new Integer(GROUP));
        objMaskToType.put(new Integer(GROUP), new Integer(Constants.GROUP));

        objTypeToMask.put(new Integer(Constants.EPERSON), new Integer(EPERSON));
        objMaskToType.put(new Integer(EPERSON), new Integer(Constants.EPERSON));
    }

    /** ---------- Event Fields ------------- * */

    /** identifier of Dispatcher that created this event (hash of its name) */
    private int dispatcher;

    /** event (action) type - above enumeration */
    private int eventType;

    /** object-type of SUBJECT - see above enumeration */
    private int subjectType;

    /** content model identifier */
    private int subjectID;

    /** object-type of SUBJECT - see above enumeration */
    private int objectType = NONE;

    /** content model identifier */
    private int objectID = -1;

    /** timestamp */
    private long timeStamp;

    /** "detail" - arbitrary field for relevant detail, */
    /** e.g. former handle for DELETE event since obj is no longer available. */
    /**
     * FIXME This field is not a complete view of the DSpaceObject that was
     * modified. Providing these objects to the consumer (e.g. by storing
     * lifecycle versions of the changed objects in the context) would provide
     * for more complex consumer abilities that are beyond our purview.
     */
    private String detail;

    /** unique key to bind together events from one context's transaction */
    private String transactionID;

    /** identity of authenticated user, i.e. context.getCurrentUser() */
    /** only needed in the event for marshalling for asynch event messages */
    private int currentUser = -1;

    /** copy of context's "extraLogInfo" filed, used only for */
    /** marshalling for asynch event messages */
    private String extraLogInfo = null;

    private BitSet consumedBy = new BitSet();

    /** log4j category */
    private static Logger log = Logger.getLogger(Event.class);

    /**
     * Constructor.
     * 
     * @param eventType
     *            action type, e.g. Event.ADD
     * @param subjectType
     *            DSpace Object Type of subject e.g. Constants.ITEM.
     * @param subjectID
     *            database ID of subject instance.
     * @param detail
     *            detail information that depends on context.
     */
    public Event(int eventType, int subjectType, int subjectID, String detail)
    {
        this.eventType = eventType;
        this.subjectType = coreTypeToMask(subjectType);
        this.subjectID = subjectID;
        timeStamp = System.currentTimeMillis();
        this.detail = detail;
    }

    /**
     * Constructor.
     * 
     * @param eventType
     *            action type, e.g. Event.ADD
     * @param subjectType
     *            DSpace Object Type of subject e.g. Constants.ITEM.
     * @param subjectID
     *            database ID of subject instance.
     * @param objectType
     *            DSpace Object Type of object e.g. Constants.BUNDLE.
     * @param objectID
     *            database ID of object instance.
     * @param detail
     *            detail information that depends on context.
     * @param
     */
    public Event(int eventType, int subjectType, int subjectID, int objectType,
            int objectID, String detail)
    {
        this.eventType = eventType;
        this.subjectType = coreTypeToMask(subjectType);
        this.subjectID = subjectID;
        this.objectType = coreTypeToMask(objectType);
        this.objectID = objectID;
        timeStamp = System.currentTimeMillis();
        this.detail = detail;
    }

    /**
     * Compare two events. Ignore any difference in the timestamps. Also ignore
     * transactionID since that is not always set initially.
     * 
     * @param other
     *            the event to compare this one to
     * @returns true if events are "equal", false otherwise.
     */
    public boolean equals(Event other)
    {
        return (this.detail == null ? other.detail == null : this.detail
                .equals(other.detail))
                && this.eventType == other.eventType
                && this.subjectType == other.subjectType
                && this.subjectID == other.subjectID
                && this.objectType == other.objectType
                && this.objectID == other.objectID;
    }

    /**
     * Set the identifier of the dispatcher that first processed this event.
     * 
     * @param id
     *            the unique (hash code) value characteristic of the dispatcher.
     */
    public void setDispatcher(int id)
    {
        dispatcher = id;
    }

    // translate a "core.Constants" object type value to local bitmask value.
    private static int coreTypeToMask(int core)
    {
        Integer mask = (Integer) objTypeToMask.get(new Integer(core));
        if (mask == null)
            return -1;
        else
            return mask.intValue();
    }

    // translate bitmask object-type to "core.Constants" object type.
    private static int maskTypeToCore(int mask)
    {
        Integer core = (Integer) objMaskToType.get(new Integer(mask));
        if (core == null)
            return -1;
        else
            return core.intValue();
    }

    /**
     * Get the DSpace object which is the "object" of an event.
     * 
     * @returns DSpaceObject or null if none can be found or no object was set.
     */
    public DSpaceObject getObject(Context context)
    {
        int type = getObjectType();
        int id = getObjectID();
        if (type < 0 || id < 0)
        {
            return null;
        }
        else
        {
            ObjectIdentifier oid = ObjectIdentifierMint.get(context, type, id);
            // ObjectIdentifier oid = new ObjectIdentifier(id, type);
            return oid.getObject(context);
        }
    }

    /**
     * Syntactic sugar to get the DSpace object which is the "subject" of an
     * event.
     * 
     * @returns DSpaceObject or null if none can be found.
     */
    public DSpaceObject getSubject(Context context)
    {
        ObjectIdentifier oid = ObjectIdentifierMint.get(context, getSubjectType(), getSubjectID());
        // ObjectIdentifier oid = new ObjectIdentifier(getSubjectID(), getSubjectType());
        return oid.getObject(context);
    }

    /**
     * @returns database ID of subject of this event.
     */
    public int getSubjectID()
    {
        return subjectID;
    }

    /**
     * @returns database ID of object of this event, or -1 if none was set.
     */
    public int getObjectID()
    {
        return objectID;
    }

    /**
     * @returns type number (e.g. Constants.ITEM) of subject of this event.
     */
    public int getSubjectType()
    {
        return maskTypeToCore(subjectType);
    }

    /**
     * @returns type number (e.g. Constants.ITEM) of object of this event, or -1
     *          if none was set.
     */
    public int getObjectType()
    {
        return maskTypeToCore(objectType);
    }

    /**
     * @returns type of subject of this event as a String, e.g. for logging.
     */
    public String getSubjectTypeAsString()
    {
        int i = log2(subjectType);
        if (i >= 0 && i < Constants.typeText.length)
            return Constants.typeText[i];
        else
            return "(Unknown)";
    }

    /**
     * @returns type of object of this event as a String, e.g. for logging.
     */
    public String getObjectTypeAsString()
    {
        int i = log2(objectType);
        if (i >= 0 && i < Constants.typeText.length)
            return Constants.typeText[i];
        else
            return "(Unknown)";
    }

    /**
     * Translate a textual DSpace Object type name into an event subject-type
     * mask. NOTE: This returns a BIT-MASK, not a numeric type value; the mask
     * is only used within the event system.
     * 
     * @param s
     *            text name of object type.
     * @returns numeric value of object type or 0 for error.
     */
    public static int parseObjectType(String s)
    {
        if (s.equals("*") | s.equalsIgnoreCase("all"))
            return ALL_OBJECTS_MASK;
        else
        {
            int id = Constants.getTypeID(s.toUpperCase());
            if (id >= 0)
                return 1 << id;
        }
        return 0;
    }

    /**
     * @returns event-type (i.e. action) this event, one of the masks like
     *          Event.ADD defined above.
     */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * Get the text name of event (action) type.
     * 
     * @returns event-type (i.e. action) this event as a String, e.g. for
     *          logging.
     */
    public String getEventTypeAsString()
    {
        int i = log2(eventType);
        if (i >= 0 && i < eventTypeText.length)
            return eventTypeText[i];
        else
            return "(Unknown)";
    }

    /**
     * Interpret named event type.
     * 
     * @param text
     *            name of event type.
     * @returns numeric value of event type or 0 for error.
     */
    public static int parseEventType(String s)
    {
        if (s.equals("*") | s.equalsIgnoreCase("all"))
        {
            int result = 0;
            for (int i = 0; i < eventTypeText.length; ++i)
                result |= (1 << i);
            return result;
        }

        for (int i = 0; i < eventTypeText.length; ++i)
            if (eventTypeText[i].equalsIgnoreCase(s))
                return 1 << i;
        return 0;
    }

    /**
     * @returns timestamp at which event occurred, as a count of milliseconds
     *          since the epoch (standard Java format).
     */
    public long getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * @returns hashcode identifier of name of Dispatcher which first dispatched
     *          this event. (Needed by asynch dispatch code.)
     */
    public int getDispatcher()
    {
        return dispatcher;
    }

    /**
     * @returns value of detail element of the event.
     */
    public String getDetail()
    {
        return detail;
    }

    /**
     * @returns value of transactionID element of the event.
     */
    public String getTransactionID()
    {
        return transactionID;
    }

    /**
     * Sets value of transactionID element of the event.
     * 
     * @param tid
     *            new value of transactionID.
     */
    public void setTransactionID(String tid)
    {
        transactionID = tid;
    }

    public void setCurrentUser(int uid)
    {
        currentUser = uid;
    }

    public int getCurrentUser()
    {
        return currentUser;
    }

    public void setExtraLogInfo(String info)
    {
        extraLogInfo = info;
    }

    public String getExtraLogInfo()
    {
        return extraLogInfo;
    }

    /**
     * @param filters
     *            list of filter masks; each one is an Array of two ints.
     * @returns true if this event would be passed through the given filter
     *          list.
     */
    public boolean pass(List filters)
    {
        boolean result = false;

        for (Iterator fi = filters.iterator(); fi.hasNext();)
        {
            int filter[] = (int[]) fi.next();
            if ((subjectType & filter[SUBJECT_MASK]) != 0
                    && (eventType & filter[EVENT_MASK]) != 0)
                result = true;
        }

        if (log.isDebugEnabled())
            log.debug("Filtering event: " + "eventType="
                    + String.valueOf(eventType) + ", subjectType="
                    + String.valueOf(subjectType) + ", result="
                    + String.valueOf(result));

        return result;
    }

    // dumb integer "log base 2", returns -1 if there are no 1's in number.
    private static int log2(int n)
    {
        for (int i = 0; i < 32; ++i)
            if (n == 1)
                return i;
            else
                n = n >> 1;
        return -1;
    }

    /**
     * Keeps track of which consumers the event has been consumed by. Should be
     * called by a dispatcher when calling consume(Context ctx, String name,
     * Event event) on an event.
     * 
     * @param consumerName
     */
    public void setBitSet(String consumerName)
    {
        consumedBy.set(EventManager.getConsumerIndex(consumerName));

    }

    public BitSet getBitSet()
    {
        return consumedBy;
    }

    /**
     * @returns Detailed string representation of contents of this event, to
     *          help in logging and debugging.
     */
    public String toString()
    {
        return "org.dspace.event.Event(eventType="
                + this.getEventTypeAsString()
                + ", SubjectType="
                + this.getSubjectTypeAsString()
                + ", SubjectID="
                + String.valueOf(subjectID)
                + ", ObjectType="
                + this.getObjectTypeAsString()
                + ", ObjectID="
                + String.valueOf(objectID)
                + ", TimeStamp="
                + String.valueOf(timeStamp)
                + ", dispatcher="
                + String.valueOf(dispatcher)
                + ", detail="
                + (detail == null ? "[null]" : "\"" + detail + "\"")
                + ", transactionID="
                + (transactionID == null ? "[null]" : "\"" + transactionID
                        + "\"") + ")";
    }
}
