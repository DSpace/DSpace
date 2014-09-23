package org.dspace.app.util;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.util.StopWatch;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 3/25/13
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class OptimizeSelectCollection {
    private static final Logger log = Logger.getLogger(OptimizeSelectCollection.class);
    private static Context context;

    private static ArrayList<EPerson> brokenPeople;

    public static void main(String[] argv) throws Exception
    {
        System.out.println("OptimizeSelectCollection tool.");
        System.out.println("We want to verify that the optimized version of select collection logic produces the same " +
                "values as the legacy select-collection logic.");

        context = new Context();
        brokenPeople = new ArrayList<EPerson>();

        if(argv != null && argv.length > 0) {
            for(String email : argv) {
                EPerson person = EPerson.findByEmail(context, email);
                checkSelectCollectionForUser(person);
            }
        } else {
            //default case, run as specific user, or run all...
            EPerson[] people = EPerson.findAll(context, EPerson.EMAIL);
            for(EPerson person : people) {
                checkSelectCollectionForUser(person);
            }
        }

        if(brokenPeople.size() > 0) {
            System.out.println("NOT DONE YET!!! Some people don't have all their collections.");
            for(EPerson person : brokenPeople) {
                System.out.println("-- " + person.getEmail());
            }
        }

    }

    private static void checkSelectCollectionForUser(EPerson person) throws SQLException {
        context.setCurrentUser(person);

        StopWatch stopWatch = new StopWatch("SelectCollectionStep Optimization (" + person.getEmail() + ")");
        System.out.println("User: " + person.getEmail());

        stopWatch.start("findAuthorized");
        Collection[] collections = Collection.findAuthorized(context, null, Constants.ADD);
        stopWatch.stop();


        stopWatch.start("ListingCollections");
        System.out.println("Legacy Find Authorized");
        reportCollections(collections);
        stopWatch.stop();

        stopWatch.start("findAuthorizedOptimized");
        Collection[] collectionsOptimized = Collection.findAuthorizedOptimized(context, Constants.ADD);
        stopWatch.stop();

        stopWatch.start("ListingCollectionsWithOptimizedCollections");
        System.out.println("Find Authorized Optimized");
        reportCollections(collectionsOptimized);
        stopWatch.stop();

        if (collections.length == collectionsOptimized.length) {
            System.out.println("Number of collections matches - Good");
        } else {
            System.out.println("Number of collections doesn't match -- Bad");
            brokenPeople.add(person);
        }

        System.out.println(stopWatch.prettyPrint());
    }

    private static void reportCollections(Collection[] collections) {
        System.out.println("====================================");
        System.out.println("This user is permitted to submit to the following collections.");

        for(Collection collection : collections) {
            System.out.println(" - " + collection.getHandle() + " -- " + collection.getName());
        }
        System.out.println("Total: " + collections.length);
    }
}
