/**
 * $Id$
 * $URL$
 * Constants.java - Dspace - Sep 1, 2008 5:49:38 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.constants;


/**
 * All core dSpace constants
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public class Constants {

    /** Type of bitstream objects */
    public static final int BITSTREAM = 0;

    /** Type of bundle objects */
    public static final int BUNDLE = 1;

    /** Type of item objects */
    public static final int ITEM = 2;

    /** Type of collection objects */
    public static final int COLLECTION = 3;

    /** Type of community objects */
    public static final int COMMUNITY = 4;

    /** DSpace site type */
    public static final int SITE = 5;

    /** Type of eperson groups */
    public static final int GROUP = 6;

    /** Type of individual eperson objects */
    public static final int EPERSON = 7;

    /**
     * lets you look up type names from the type IDs
     */
    public static final String[] typeText = { "BITSTREAM", "BUNDLE", "ITEM",
            "COLLECTION", "COMMUNITY", "SITE", "GROUP", "EPERSON", };

    /**
     * Special Bundle and Bitstream Names:
     */

    /** Magic name of item license, as bitstream in LICENSE_BUNDLE_NAME  */
    public static final String LICENSE_BITSTREAM_NAME = "license.txt";

    /** Magic name of bundle containing item license */
    public static final String LICENSE_BUNDLE_NAME = "LICENSE";

    /**
     * Default bundle name for the "original" item content;
     * "derived" content such as thumbnails goes in other bundles.
     */
    public static final String DEFAULT_BUNDLE_NAME = "ORIGINAL";

    /**
     * Name of bundle for user-visible "content" (same as default for now).
     */
    public static final String CONTENT_BUNDLE_NAME = "ORIGINAL";

    /** Bundle name for structured metadata bitstreams. */
    public static final String METADATA_BUNDLE_NAME = "METADATA";


    /** Action of reading, viewing or downloading something */
    public static final int READ = 0;

    /** Action of modifying something */
    public static final int WRITE = 1;

    /**
     * Action of deleting something. Different from removing something from a
     * container. (DELETE is now obsolete)
     * 
     * @see #REMOVE
     */
    public static final int DELETE = 2;

    /**
     * Action of adding something to a container. For example, to add an item to
     * a collection, a user must have <code>ADD</code> permission on the
     * collection.
     */
    public static final int ADD = 3;

    /**
     * Action of removing something from a container. Different from deletion.
     * 
     * @see #DELETE
     */
    public static final int REMOVE = 4;

    /** Action of performing workflow step 1 */
    public static final int WORKFLOW_STEP_1 = 5;

    /** Action of performing workflow step 2 */
    public static final int WORKFLOW_STEP_2 = 6;

    /** Action of performing workflow step 3 */
    public static final int WORKFLOW_STEP_3 = 7;

    /** Action of performing a workflow */
    public static final int WORKFLOW_ABORT = 8;

    /** Default Read policies for Bitstreams submitted to container */
    public static final int DEFAULT_BITSTREAM_READ = 9;

    /** Default Read policies for Items submitted to container */
    public static final int DEFAULT_ITEM_READ = 10;

    /**
     * collection admin -- metadata, logo, item metadata, submitters, withdraw
     * items, etc.
     */
    public static final int COLLECTION_ADMIN = 11;

    /** Position of front page news item -- top box */
    public static final int NEWS_TOP = 0;

    /** Position of front page news item -- sidebar */
    public static final int NEWS_SIDE = 1;

    /**
     * lets you look up action names from the action IDs
     */
    public static final String[] actionText = { "READ", "WRITE",
            "OBSOLETE (DELETE)", "ADD", "REMOVE", "WORKFLOW_STEP_1",
            "WORKFLOW_STEP_2", "WORKFLOW_STEP_3", "WORKFLOW_ABORT",
            "DEFAULT_BITSTREAM_READ", "DEFAULT_ITEM_READ", "COLLECTION_ADMIN" };

    /**
     * constants for the relevance array generating dynamicallis is simple: just
     * 1 < < TYPE
     */
    public static final int RBITSTREAM = 1 << BITSTREAM;

    public static final int RBUNDLE = 1 << BUNDLE;

    public static final int RITEM = 1 << ITEM;

    public static final int RCOLLECTION = 1 << COLLECTION;

    public static final int RCOMMUNITY = 1 << COMMUNITY;

    /**
     * Array of relevances of actions to objects - used by the UI to only
     * display actions that are relevant to an object type To see if an action
     * is relevant to an object, just OR the relevance type above with the value
     * in actionTypeRelevance[] (To see if READ is relevant to community, just
     * test actionTypeRelevance[READ] | RCOMMUNITY, 0 = irrelevant
     */
    public static final int[] actionTypeRelevance = {
            RBITSTREAM | RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 0 - READ
            RBITSTREAM | RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 1 -
                                                                     // WRITE
            0, // 2 - DELETE (obsolete)
            RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 3 - ADD
            RBUNDLE | RITEM | RCOLLECTION | RCOMMUNITY, // 4 - REMOVE
            0, // 5 - WORKFLOW_STEP_1
            0, // 6 - WORKFLOW_STEP_2
            0, // 7 - WORKFLOW_STEP_3
            0, // 8 - WORKFLOW_ABORT
            RCOLLECTION, // 9 - DEFAULT_BITSTREAM_READ
            RCOLLECTION, // 10 - DEFAULT_ITEM_READ
            RCOLLECTION // 11 - COLLECTION_ADMIN
    };

    public static final String DEFAULT_ENCODING = "UTF-8";

}
