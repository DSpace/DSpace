/*
 * CommunityGroup.java
 */

package org.dspace.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class representing a community group.
 */
public class CommunityGroup
{
    public static final int FACULTY   = 0;
    public static final int LIBRARIES = 1;
    public static final int UM        = 2;

    private static final Map<Integer, String> names;
    static {
        Map<Integer, String> staticNames = new HashMap<>();
        staticNames.put(FACULTY, "Collections Organized by Department");
        staticNames.put(LIBRARIES, "Collections Managed by UM Libraries");
        staticNames.put(UM, "UM Community-managed Collections");
        names = Collections.unmodifiableMap(staticNames);
    }

    private static final Map<Integer, String> shortNames;
    static {
        Map<Integer, String> staticShortNames = new HashMap<>();
        staticShortNames.put(FACULTY, "UM Faculty");  
        staticShortNames.put(LIBRARIES, "UM Libraries");
        staticShortNames.put(UM, "UM Community");
        shortNames = Collections.unmodifiableMap(staticShortNames);
    }

    protected static final Map<Integer, CommunityGroup> communityGroups;
    static {
        Map<Integer, CommunityGroup> staticCommunityGroups = new HashMap<>();
        staticCommunityGroups.put(FACULTY, new CommunityGroup(FACULTY));
        //staticCommunityGroups.put(LIBRARIES, new CommunityGroup(LIBRARIES));
        staticCommunityGroups.put(UM, new CommunityGroup(UM));
        communityGroups = Collections.unmodifiableMap(staticCommunityGroups);
    }

    private int id;

    /**
     * Construct a group object.
     */
    private CommunityGroup(int id) {
        this.id = id;
    }

    /**
     * Get the name of this community group.
     * @return the name of the group
     */
    public String getName()
    {
        return CommunityGroup.names.get(id);
    }

    /**
     * Get the short name of this community group.
     *
     * @param group_id the group id
     *
     * @return the name of the group
     */
    public String getShortName()
    {
      return CommunityGroup.shortNames.get(id);
    }

    /**
     * Get the internal ID of this community group
     *
     * @return the internal identifier
     */
    public int getID()
    {
        return this.id;
    }
}
