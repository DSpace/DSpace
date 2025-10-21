/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

/**
 * A command-line tool for setting/removing community/sub-community
 * relationships. Takes community DB Id or handle arguments as inputs.
 *
 * @author rrodgers
 * @version $Revision$
 */

public class CommunityFiliator {

    protected CommunityService communityService;
    protected CollectionService collectionService;
    protected HandleService handleService;

    public CommunityFiliator() {
        communityService = ContentServiceFactory.getInstance().getCommunityService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        handleService = HandleServiceFactory.getInstance().getHandleService();
    }

    /**
     * @param argv the command line arguments given
     * @throws Exception if error
     */
    public static void main(String[] argv) throws Exception {
        // create an options object and populate it
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();

        options.addOption("s", "set", false, "set a parent/child relationship");
        options.addOption("r", "remove", false,
                          "remove a parent/child relationship");
        options.addOption("p", "parent", true,
                          "parent community (handle or database ID)");
        options.addOption("c", "child", true,
                          "child community (handle or databaseID)");
        options.addOption("h", "help", false, "help");

        CommandLine line = parser.parse(options, argv);

        String command = null; // set or remove
        String parentID = null;
        String childID = null;

        if (line.hasOption('h')) {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("CommunityFiliator\n", options);
            System.out
                .println("\nestablish a relationship: CommunityFiliator -s -p parentID -c childID");
            System.out
                .println("remove a relationship: CommunityFiliator -r -p parentID -c childID");

            System.exit(0);
        }

        if (line.hasOption('s')) {
            command = "set";
        }

        if (line.hasOption('r')) {
            command = "remove";
        }

        if (line.hasOption('p')) { // parent
            parentID = line.getOptionValue('p');
        }

        if (line.hasOption('c')) { // child
            childID = line.getOptionValue('c');
        }

        // now validate
        // must have a command set
        if (command == null) {
            System.out
                .println("Error - must run with either set or remove (run with -h flag for details)");
            System.exit(1);
        }

        if ("set".equals(command) || "remove".equals(command)) {
            if (parentID == null) {
                System.out.println("Error - a parentID must be specified (run with -h flag for details)");
                System.exit(1);
            }

            if (childID == null) {
                System.out.println("Error - a childID must be specified (run with -h flag for details)");
                System.exit(1);
            }
        }

        CommunityFiliator filiator = new CommunityFiliator();
        Context c = new Context();

        // we are superuser!
        c.turnOffAuthorisationSystem();

        try {
            // validate and resolve the parent and child IDs into commmunities or collections
            Community parent = filiator.resolveCommunity(c, parentID);
            DSpaceObject child = filiator.resolveCommunityOrCollection(c, childID);

            if (parent == null) {
                System.out.println("Error, parent community cannot be found: "
                                       + parentID);
                System.exit(1);
            }

            if (child == null) {
                System.out.println("Error, child object cannot be found: "
                                       + childID);
                System.exit(1);
            }

            if ("set".equals(command)) {
                filiator.filiate(c, parent, child);
            } else {
                filiator.defiliate(c, parent, child);
            }
        } catch (SQLException sqlE) {
            System.out.println("Error - SQL exception: " + sqlE.toString());
        } catch (AuthorizeException authE) {
            System.out.println("Error - Authorize exception: "
                                   + authE.toString());
        }
    }

    /**
     * @param c      context
     * @param parent parent Community
     * @param child  child DSpaceObject
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorize error
     */
    public void filiate(Context c, Community parent, DSpaceObject child)
        throws SQLException, AuthorizeException {

        if (child.getType() == Constants.COMMUNITY) {
            Community childCommunity = (Community)child;
            filiate(c, parent, childCommunity);
        } else if (child.getType() == Constants.COLLECTION) {
            Collection childCollection = (Collection)child;
            filiate(c, parent, childCollection);
        } else {
            throw new IllegalArgumentException("Invalid child object type: "
                                                   + child.getType());
        }
    }

    /**
     * @param c      context
     * @param parent parent Community
     * @param child  child community
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorize error
     */
    public void filiate(Context c, Community parent, Community child)
        throws SQLException, AuthorizeException {
        // check that a valid filiation would be established
        // first test - proposed child must currently be an orphan (i.e.
        // top-level)
        Community childDad = CollectionUtils.isNotEmpty(child.getParentCommunities()) ? child.getParentCommunities()
                                                                                             .iterator().next() : null;

        if (childDad != null) {
            System.out.println("Error, child community: " + child.getID()
                                   + " already a child of: " + childDad.getID());
            System.exit(1);
        }

        // second test - circularity: parent's parents can't include proposed
        // child
        List<Community> parentDads = parent.getParentCommunities();
        if (parentDads.contains(child)) {
            System.out.println("Error, circular parentage - child is parent of parent");
            System.exit(1);
        }

        // everthing's OK
        communityService.addSubcommunity(c, parent, child);

        // complete the pending transaction
        c.complete();
        System.out.println("Filiation complete. Community: '" + parent.getID()
                               + "' is parent of community: '" + child.getID() + "'");
    }

    /**
     * @param c      context
     * @param parent parent Community
     * @param child  child collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorize error
     */
    public void filiate(Context c, Community parent, Collection child)
        throws SQLException, AuthorizeException {
        // check that a valid filiation would be established
        // proposed child must currently be an orphan (i.e. top-level)
        Community childDad = CollectionUtils.isNotEmpty(child.getCommunities()) ? child.getCommunities()
                                                                                       .iterator().next() : null;

        if (childDad != null) {
            System.out.println("Error, collection: " + child.getID()
                                   + " already a child of: " + childDad.getID());
            System.exit(1);
        }

        // everthing's OK
        communityService.addCollection(c, parent, child);

        // complete the pending transaction
        c.complete();
        System.out.println("Filiation complete. Community: '" + parent.getID()
                               + "' is parent of collection: '" + child.getID() + "'");
    }

    /**
     * @param c      context
     * @param parent parent Community
     * @param child  child DSpaceObject
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorize error
     */
    public void defiliate(Context c, Community parent, DSpaceObject child)
        throws SQLException, AuthorizeException {

        if (child.getType() == Constants.COMMUNITY) {
            Community childCommunity = (Community)child;
            defiliate(c, parent, childCommunity);
        } else if (child.getType() == Constants.COLLECTION) {
            Collection childCollection = (Collection)child;
            defiliate(c, parent, childCollection);
        } else {
            throw new IllegalArgumentException("Invalid child object type: "
                                                   + child.getType());
        }
    }

    /**
     * @param c      context
     * @param parent parent Community
     * @param child  child community
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorize error
     */
    public void defiliate(Context c, Community parent, Community child)
        throws SQLException, AuthorizeException {
        // verify that child is indeed a child of parent
        List<Community> parentKids = parent.getSubcommunities();
        if (!parentKids.contains(child)) {
            System.out.println("Error, child community not a child of parent community");
            System.exit(1);
        }

        // OK remove the mappings - but leave the community, which will become
        // top-level
        child.removeParentCommunity(parent);
        parent.removeSubCommunity(child);
        communityService.update(c, child);
        communityService.update(c, parent);

        // complete the pending transaction
        c.complete();
        System.out.println("Defiliation complete. Community: '" + child.getID()
                               + "' is no longer a child of community: '" + parent.getID()
                               + "'");
    }

    /**
     * @param c      context
     * @param parent parent Community
     * @param child  child collection
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorize error
     */
    public void defiliate(Context c, Community parent, Collection child)
        throws SQLException, AuthorizeException {
        // verify that child is indeed a child of parent
        List<Collection> parentKids = parent.getCollections();
        if (!parentKids.contains(child)) {
            System.out
                .println("Error, collection not a child of parent community");
            System.exit(1);
        }

        // OK remove the mappings - but leave the community, which will become
        // top-level
        child.removeCommunity(parent);
        parent.removeCollection(child);
        collectionService.update(c, child);
        communityService.update(c, parent);

        // complete the pending transaction
        c.complete();
        System.out.println("Defiliation complete. Collection: '" + child.getID()
                               + "' is no longer a child of community: '" + parent.getID()
                               + "'");
    }

    /**
     * Find a community by ID
     *
     * @param c           context
     * @param communityID community ID
     * @return Community object
     * @throws SQLException if database error
     */
    protected Community resolveCommunity(Context c, String communityID)
        throws SQLException {
        Community community = null;

        if (communityID.indexOf('/') != -1) {
            // has a / must be a handle
            DSpaceObject dso = handleService.resolveToObject(c,
                                                             communityID);

            // ensure it's a community
            if ((dso != null) && (dso.getType() == Constants.COMMUNITY)) {
                community = (Community)dso;
            }
        } else {
            community = communityService.find(c, UUID.fromString(communityID));
        }

        return community;
    }

    /**
     * Find a community or collection by ID
     *
     * @param c     context
     * @param dsoID community or collection ID
     * @return DSpaceObject object
     * @throws SQLException if database error
     */
    protected DSpaceObject resolveCommunityOrCollection(Context c, String dsoID)
        throws SQLException {
        DSpaceObject dso = null;

        if (dsoID.indexOf('/') != -1) {
            // has a / must be a handle
            dso = handleService.resolveToObject(c, dsoID);

            // ensure it's a community or collection
            if ((dso != null)
                && (dso.getType() != Constants.COMMUNITY)
                && (dso.getType() != Constants.COLLECTION)) {
                dso = null;
            }
        } else {
            Community community = communityService.find(c, UUID.fromString(dsoID));

            if (community != null) {
                dso = community;
            } else {
                dso = collectionService.find(c, UUID.fromString(dsoID));
            }
        }

        return dso;
    }
}
