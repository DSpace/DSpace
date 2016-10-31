/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.factory.EventServiceFactory;

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
 * <ul>
 * <li>(Action) Type</li>
 * <li>Subject -- DSpace object to which the action applies, e.g. the Collection
 * to which an ADD adds a member.</li>
 * <li>Object -- optional, when present it is the other object effected by an
 * action, e.g. the Item ADDed to a Collection by an ADD.</li>
 * <li>detail -- a textual summary of what changed.  Content and its
 * significance varies by the combination of action and subject type.</li>
 * <li> - timestamp -- exact millisecond timestamp at which event was logged.</li>
 * </ul>
 * 
 * @version $Revision$
 */
public class Event implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** ---------- Constants ------------- * */

    /** Event (Action) types */
    public static final int CREATE = 1 << 0; // create new object

    public static final int MODIFY = 1 << 1; // modify object

    public static final int MODIFY_METADATA = 1 << 2; // modify object

    public static final int ADD = 1 << 3; // add content to container

    public static final int REMOVE = 1 << 4; // remove content from container

    public static final int DELETE = 1 << 5; // destroy object

    public static final int INSTALL = 1 << 6; // object exits workspace/flow

    /** Index of filter parts in their array: */
    public static final int SUBJECT_MASK = 0; // mask of subject types

    public static final int EVENT_MASK = 1; // mask of event type

    // XXX NOTE: keep this up to date with any changes to event (action) types.
    protected static final String eventTypeText[] = { "CREATE", "MODIFY",
            "MODIFY_METADATA", "ADD", "REMOVE", "DELETE", "INSTALL" };

    /** XXX NOTE: These constants must be kept synchronized * */
    /** XXX NOTE: with ALL_OBJECTS_MASK *AND* objTypeToMask hash * */
    protected static final int NONE = 0;

    protected static final int BITSTREAM = 1 << Constants.BITSTREAM; // 0

    protected static final int BUNDLE = 1 << Constants.BUNDLE; // 1

    protected static final int ITEM = 1 << Constants.ITEM; // 2

    protected static final int COLLECTION = 1 << Constants.COLLECTION; // 3

    protected static final int COMMUNITY = 1 << Constants.COMMUNITY; // 4

    protected static final int SITE = 1 << Constants.SITE; // 5

    protected static final int GROUP = 1 << Constants.GROUP; // 6

    protected static final int EPERSON = 1 << Constants.EPERSON; // 7

    protected static final int ALL_OBJECTS_MASK = BITSTREAM | BUNDLE | ITEM
            | COLLECTION | COMMUNITY | SITE | GROUP | EPERSON;

    protected static Map<Integer, Integer> objTypeToMask = new HashMap<Integer, Integer>();

    protected static Map<Integer, Integer> objMaskToType = new HashMap<Integer, Integer>();
    static
    {
        objTypeToMask.put(Constants.BITSTREAM, BITSTREAM);
        objMaskToType.put(BITSTREAM, Constants.BITSTREAM);

        objTypeToMask.put(Constants.BUNDLE, BUNDLE);
        objMaskToType.put(BUNDLE, Constants.BUNDLE);

        objTypeToMask.put(Constants.ITEM, ITEM);
        objMaskToType.put(ITEM, Constants.ITEM);

        objTypeToMask.put(Constants.COLLECTION, COLLECTION);
        objMaskToType.put(COLLECTION, Constants.COLLECTION);

        objTypeToMask.put(Constants.COMMUNITY, COMMUNITY);
        objMaskToType.put(COMMUNITY, Constants.COMMUNITY);

        objTypeToMask.put(Constants.SITE, SITE);
        objMaskToType.put(SITE, Constants.SITE);

        objTypeToMask.put(Constants.GROUP, GROUP);
        objMaskToType.put(GROUP, Constants.GROUP);

        objTypeToMask.put(Constants.EPERSON, EPERSON);
        objMaskToType.put(EPERSON, Constants.EPERSON);
    }

    /** ---------- Event Fields ------------- * */

    /** identifier of Dispatcher that created this event (hash of its name) */
    private int dispatcher;

    /** event (action) type - above enumeration */
    private int eventType;

    /** object-type of SUBJECT - see above enumeration */
    private int subjectType;

    /** content model identifier */
    private UUID subjectID;

    /** object-type of SUBJECT - see above enumeration */
    private int objectType = NONE;

    /** content model identifier */
    private UUID objectID = null;

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

    /**
     * Contains all identifiers of the DSpaceObject that was changed (added,
     * modified, deleted, ...).
     *
     * All events gets fired when a context that contains events gets commited.
     * When the delete event is fired, a deleted DSpaceObject is already gone.
     * This array contains all identifiers of the object, not only the handle
     * as the detail field does. The field may be an empty array if no
     * identifiers could be found.
     *
     * FIXME: As the detail field describes it would be even better if all
     * metadata would be available to a consumer, but the identifiers are the
     * most important once.
     */
    private ArrayList<String> identifiers;

    /** unique key to bind together events from one context's transaction */
    private String transactionID;

    /** identity of authenticated user, i.e. context.getCurrentUser(). */
    /** Only needed in the event for marshalling for asynch event messages */
    private int currentUser = -1;

    /** copy of context's "extraLogInfo" field.  Used only for */
    /** marshalling for asynch event messages. */
    private String extraLogInfo = null;

    private BitSet consumedBy = new BitSet();

    /** log4j category */
    private static Logger log = Logger.getLogger(Event.class);

    /**
     * Constructor.
     * 
     * You should consider to use 
     * {@link Event#Event(int, int, UUID, java.lang.String)}.
     * 
     * @param eventType
     *            action type, e.g. Event.ADD.
     * @param subjectType
     *            DSpace Object Type of subject e.g. Constants.ITEM.
     * @param subjectID
     *            database ID of subject instance.
     * @param detail
     *            detail information that depends on context.
     */
    public Event(int eventType, int subjectType, UUID subjectID, String detail)
    {
        this(eventType, subjectType, subjectID, detail, new ArrayList<String>());
    }
    
    /**
     * Constructor.
     * 
     * @param eventType
     *            action type, e.g. Event.ADD.
     * @param subjectType
     *            DSpace Object Type of subject e.g. Constants.ITEM.
     * @param subjectID
     *            database ID of subject instance.
     * @param detail
     *            detail information that depends on context.
     * @param identifiers
     *            array containing all identifiers of the dso or an empty array
     */
    public Event(int eventType, int subjectType, UUID subjectID, String detail, ArrayList<String> identifiers)
    {
        this.eventType = eventType;
        this.subjectType = coreTypeToMask(subjectType);
        this.subjectID = subjectID;
        timeStamp = System.currentTimeMillis();
        this.detail = detail;
        this.identifiers = (ArrayList<String>) identifiers.clone();
    }
    
    /**
     * Constructor.
     * 
     * You should consider to use 
     * {@link Event#Event(int, int, UUID, int, UUID, java.lang.String)} instead.
     * 
     * @param eventType
     *            action type, e.g. Event.ADD.
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
     */
    public Event(int eventType, int subjectType, UUID subjectID, int objectType,
                 UUID objectID, String detail)
    {
        this(eventType, subjectType, subjectID, objectType, objectID, detail, 
                new ArrayList<String>());
    }

    /**
     * Constructor.
     * 
     * @param eventType
     *            action type, e.g. Event.ADD.
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
     * @param identifiers
     *            array containing all identifiers of the dso or an empty array
     */
    public Event(int eventType, int subjectType, UUID subjectID, int objectType,
                 UUID objectID, String detail, ArrayList<String> identifiers)
    {
        this.eventType = eventType;
        this.subjectType = coreTypeToMask(subjectType);
        this.subjectID = subjectID;
        this.objectType = coreTypeToMask(objectType);
        this.objectID = objectID;
        timeStamp = System.currentTimeMillis();
        this.detail = detail;
        this.identifiers = (ArrayList<String>) identifiers.clone();
    }

    /**
     * Compare two events. Ignore any difference in the timestamps. Also ignore
     * transactionID since that is not always set initially.
     * 
     * @param other
     *            the event to compare this one to
     * @return true if events are "equal", false otherwise.
     */
    public boolean equals(Object other)
    {
        if (other instanceof Event)
        {
            Event otherEvent = (Event)other;
            return (this.detail == null ? otherEvent.detail == null : this.detail
                    .equals(otherEvent.detail))
                    && this.eventType == otherEvent.eventType
                    && this.subjectType == otherEvent.subjectType
                    && this.subjectID == otherEvent.subjectID
                    && this.objectType == otherEvent.objectType
                    && this.objectID == otherEvent.objectID;
        }

        return false;
    }

    public int hashCode()
    {
        return new HashCodeBuilder().append(this.detail)
                                    .append(eventType)
                                    .append(subjectType)
                                    .append(subjectID)
                                    .append(objectType)
                                    .append(objectID)
                                    .toHashCode();
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
    protected int coreTypeToMask(int core)
    {
        Integer mask = objTypeToMask.get(core);
        if (mask == null)
        {
            return -1;
        }
        else
        {
            return mask.intValue();
        }
    }

    // translate bitmask object-type to "core.Constants" object type.
    protected int maskTypeToCore(int mask)
    {
        Integer core = objMaskToType.get(mask);
        if (core == null)
        {
            return -1;
        }
        else
        {
            return core.intValue();
        }
    }

    /**
     * Get the DSpace object which is the "object" of an event.
     * 
     * @param context
     *     The relevant DSpace Context.
     * @return DSpaceObject or null if none can be found or no object was set.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public DSpaceObject getObject(Context context) throws SQLException
    {
        int type = getObjectType();
        UUID id = getObjectID();
        if (type < 0 || id == null)
        {
            return null;
        }
        else
        {
            return ContentServiceFactory.getInstance().getDSpaceObjectService(type).find(context, id);
        }
    }

    /**
     * Syntactic sugar to get the DSpace object which is the "subject" of an
     * event.
     * 
     * @param context
     *     The relevant DSpace Context.
     * @return DSpaceObject or null if none can be found.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public DSpaceObject getSubject(Context context) throws SQLException
    {
        return ContentServiceFactory.getInstance().getDSpaceObjectService(getSubjectType()).find(context, getSubjectID());
    }

    /**
     * @return database ID of subject of this event.
     */
    public UUID getSubjectID()
    {
        return subjectID;
    }

    /**
     * @return database ID of object of this event, or -1 if none was set.
     */
    public UUID getObjectID()
    {
        return objectID;
    }

    /**
     * @return type number (e.g. Constants.ITEM) of subject of this event.
     */
    public int getSubjectType()
    {
        return maskTypeToCore(subjectType);
    }

    /**
     * @return type number (e.g. Constants.ITEM) of object of this event, or -1
     *          if none was set.
     */
    public int getObjectType()
    {
        return maskTypeToCore(objectType);
    }

    /**
     * @return type of subject of this event as a String, e.g. for logging.
     */
    public String getSubjectTypeAsString()
    {
        int i = log2(subjectType);
        if (i >= 0 && i < Constants.typeText.length)
        {
            return Constants.typeText[i];
        }
        else
        {
            return "(Unknown)";
        }
    }

    /**
     * @return type of object of this event as a String, e.g. for logging.
     */
    public String getObjectTypeAsString()
    {
        int i = log2(objectType);
        if (i >= 0 && i < Constants.typeText.length)
        {
            return Constants.typeText[i];
        }
        else
        {
            return "(Unknown)";
        }
    }

    /**
     * Translate a textual DSpace Object type name into an event subject-type
     * mask. NOTE: This returns a BIT-MASK, not a numeric type value; the mask
     * is only used within the event system.
     * 
     * @param s
     *            text name of object type.
     * @return numeric value of object type or 0 for error.
     */
    public static int parseObjectType(String s)
    {
        if ("*".equals(s) || "all".equalsIgnoreCase(s))
        {
            return ALL_OBJECTS_MASK;
        }
        else
        {
            int id = Constants.getTypeID(s.toUpperCase());
            if (id >= 0)
            {
                return 1 << id;
            }
        }
        return 0;
    }

    /**
     * @return event-type (i.e. action) this event, one of the masks like
     *          Event.ADD defined above.
     */
    public int getEventType()
    {
        return eventType;
    }

    /**
     * Get the text name of event (action) type.
     * 
     * @return event-type (i.e. action) this event as a String, e.g. for
     *          logging.
     */
    public String getEventTypeAsString()
    {
        int i = log2(eventType);
        if (i >= 0 && i < eventTypeText.length)
        {
            return eventTypeText[i];
        }
        else
        {
            return "(Unknown)";
        }
    }

    /**
     * Interpret named event type.
     * 
     * @param s
     *            name of event type.
     * @return numeric value of event type or 0 for error.
     */
    public static int parseEventType(String s)
    {
        if ("*".equals(s) || "all".equalsIgnoreCase(s))
        {
            int result = 0;
            for (int i = 0; i < eventTypeText.length; ++i)
            {
                result |= (1 << i);
            }
            return result;
        }

        for (int i = 0; i < eventTypeText.length; ++i)
        {
            if (eventTypeText[i].equalsIgnoreCase(s))
            {
                return 1 << i;
            }
        }
        return 0;
    }

    /**
     * @return timestamp at which event occurred, as a count of milliseconds
     *         since the epoch (standard Java format).
     */
    public long getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * @return hashcode identifier of name of Dispatcher which first dispatched
     *         this event. (Needed by asynch dispatch code.)
     */
    public int getDispatcher()
    {
        return dispatcher;
    }

    /**
     * @return value of detail element of the event.
     */
    public String getDetail()
    {
        return detail;
    }
    
    /**
     * @return array of identifiers of this event's subject.
     */
    public List<String> getIdentifiers()
    {
        // don't return a reference to our private array, clone it.
        return (List<String>) identifiers.clone();
    }

    /**
     * @return value of transactionID element of the event.
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
     * Test whether this event would pass through a list of filters.
     * 
     * @param filters
     *            list of filter masks; each one is an Array of two ints.
     * @return true if this event would be passed through the given filter
     *         list.
     */
    public boolean pass(List<int[]> filters)
    {
        boolean result = false;

        for (int filter[] : filters)
        {
            if ((subjectType & filter[SUBJECT_MASK]) != 0 && (eventType & filter[EVENT_MASK]) != 0)
            {
                result = true;
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("Filtering event: " + "eventType="
                    + String.valueOf(eventType) + ", subjectType="
                    + String.valueOf(subjectType) + ", result="
                    + String.valueOf(result));
        }

        return result;
    }

    // dumb integer "log base 2", returns -1 if there are no 1's in number.
    protected int log2(int n)
    {
        for (int i = 0; i < 32; ++i)
        {
            if (n == 1)
            {
                return i;
            }
            else
            {
                n = n >> 1;
            }
        }
        return -1;
    }

    /**
     * Keeps track of which consumers have consumed the event. Should be
     * called by a dispatcher when calling consume(Context ctx, String name,
     * Event event) on an event.
     * 
     * @param consumerName
     *     name of consumer which has consumed the event
     */
    public void setBitSet(String consumerName)
    {
        consumedBy.set(EventServiceFactory.getInstance().getEventService().getConsumerIndex(consumerName));
    }

    /**
     * @return the set of consumers which have consumed this Event.
     */
    public BitSet getBitSet()
    {
        return consumedBy;
    }

    /**
     * @return Detailed string representation of contents of this event, to
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
