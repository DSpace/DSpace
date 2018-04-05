/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.springframework.util.StopWatch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peterdietz
 * A command line tool to verify/test the accuracy and speed gains of Collection.findAuthorizedOptimized()
 * Invocation: dsrun org.dspace.app.util.OptimizeSelectCollection
 */
public class OptimizeSelectCollection {
    private static final Logger log = Logger.getLogger(OptimizeSelectCollection.class);
    private static Context context;

    private static ArrayList<EPerson> brokenPeople;
    private static Long timeSavedMS = 0L;
    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    public static void main(String[] argv) throws Exception
    {
        System.out.println("OptimizeSelectCollection tool.");
        System.out.println("We want to verify that the optimized version of select collection logic produces the same " +
                "values as the legacy select-collection logic.");

        context = new Context();
        brokenPeople = new ArrayList<EPerson>();
        int peopleChecked = 0;
        timeSavedMS = 0L;

        if(argv != null && argv.length > 0) {
            for(String email : argv) {
                EPerson person = ePersonService.findByEmail(context, email);
                checkSelectCollectionForUser(person);
                peopleChecked++;
            }
        } else {
            //default case, run as specific user, or run all...
            List<EPerson> people = ePersonService.findAll(context, EPerson.EMAIL);
            for(EPerson person : people) {
                checkSelectCollectionForUser(person);
                peopleChecked++;
            }
        }

        if(brokenPeople.size() > 0) {
            System.out.println("NOT DONE YET!!! Some people don't have all their collections.");
            for(EPerson person : brokenPeople) {
                System.out.println("-- " + person.getEmail());
            }
        } else {
            System.out.println("All Good: " + peopleChecked + " people have been checked, with same submission powers. TimeSaved(ms): " + timeSavedMS);
        }

    }

    private static void checkSelectCollectionForUser(EPerson person) throws SQLException {
        context.setCurrentUser(person);

        StopWatch stopWatch = new StopWatch("SelectCollectionStep Optimization (" + person.getEmail() + ")");
        System.out.println("User: " + person.getEmail());

        stopWatch.start("findAuthorized");
        List<Collection> collections = collectionService.findAuthorized(context, null, Constants.ADD);
        stopWatch.stop();
        Long defaultMS = stopWatch.getLastTaskTimeMillis();

        stopWatch.start("ListingCollections");
        System.out.println("Legacy Find Authorized");
        reportCollections(collections);
        stopWatch.stop();

        stopWatch.start("findAuthorizedOptimized");
        List<Collection> collectionsOptimized = collectionService.findAuthorizedOptimized(context, Constants.ADD);
        stopWatch.stop();
        Long optimizedMS = stopWatch.getLastTaskTimeMillis();
        timeSavedMS += defaultMS - optimizedMS;


        stopWatch.start("ListingCollectionsWithOptimizedCollections");
        System.out.println("Find Authorized Optimized");
        reportCollections(collectionsOptimized);
        stopWatch.stop();

        if (collections.size() == collectionsOptimized.size()) {
            System.out.println("Number of collections matches - Good");
        } else {
            System.out.println("Number of collections doesn't match -- Bad");
            brokenPeople.add(person);
        }

        System.out.println(stopWatch.prettyPrint());
    }

    private static void reportCollections(List<Collection> collections) {
        System.out.println("====================================");
        System.out.println("This user is permitted to submit to the following collections.");

        for(Collection collection : collections) {
            System.out.println(" - " + collection.getHandle() + " -- " + collection.getName());
        }
        System.out.println("Total: " + collections.size());
    }
}
