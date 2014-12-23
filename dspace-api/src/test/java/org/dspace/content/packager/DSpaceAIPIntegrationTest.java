/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import mockit.NonStrictExpectations;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Basic integration testing for the AIP Backup and Restore feature
 * https://wiki.duraspace.org/display/DSDOC5x/AIP+Backup+and+Restore
 * 
 * @author Tim Donohue
 */
public class DSpaceAIPIntegrationTest extends AbstractUnitTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(DSpaceAIPIntegrationTest.class);
   
    /** InfoMap multiple value separator (see saveObjectInfo() and assertObject* methods) **/
    private static final String valueseparator = "::";
    
    /** Test objects **/
    private static String topCommunityHandle = null;
    private static String testCollectionHandle = null;
    private static String testItemHandle = null;
    
    /** Create a temporary folder which will be cleaned up automatically by JUnit **/
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @BeforeClass
    public static void setUpClass()
    {      
        try
        {
            Context context = new Context();
            // Create a dummy Community hierarchy to test with
            // Turn off authorization temporarily to create some test objects.
            context.turnOffAuthorisationSystem();

            log.info("setUpClass() - CREATE TEST HIERARCHY");
            // Create a hierachy of sub-Communities and Collections and Items, 
            // which looks like this:
            //  "Parent Community"
            //      - "Child Community"
            //          - "Grandchild Community"
            //              - "GreatGrandchild Collection"
            //                  - "GreatGrandchild Collection Item #1"
            //                  - "GreatGrandchild Collection Item #2"
            //          - "Grandchild Collection"
            //              - "Grandchild Collection Item #1"
            //              - "Grandchild Collection Item #2"
            //
            Community topCommunity = Community.create(null,context);
            topCommunity.addMetadata(MetadataSchema.DC_SCHEMA, "title", null, null, "Top Community");
            topCommunity.update();
            topCommunityHandle = topCommunity.getHandle();
            
            Community child = topCommunity.createSubcommunity();
            child.addMetadata(MetadataSchema.DC_SCHEMA, "title", null, null, "Child Community");
            child.update();
            
            Community grandchild = child.createSubcommunity();
            grandchild.addMetadata(MetadataSchema.DC_SCHEMA, "title", null, null, "Grandchild Community");
            grandchild.update();

            Collection grandchildCol = child.createCollection();
            grandchildCol.addMetadata("dc", "title", null, null, "Grandchild Collection");
            grandchildCol.update();
            testCollectionHandle = grandchildCol.getHandle();

            Collection greatgrandchildCol = grandchild.createCollection();
            greatgrandchildCol.addMetadata("dc", "title", null, null, "GreatGrandchild Collection");
            greatgrandchildCol.update();

            WorkspaceItem wsItem = WorkspaceItem.create(context, grandchildCol, false);
            Item item = InstallItem.installItem(context, wsItem);
            item.addMetadata("dc", "title", null, null, "Grandchild Collection Item #1");
            item.update();

            WorkspaceItem wsItem2 = WorkspaceItem.create(context, grandchildCol, false);
            Item item2 = InstallItem.installItem(context, wsItem2);
            item2.addMetadata("dc", "title", null, null, "Grandchild Collection Item #2");
            item2.update();

            WorkspaceItem wsItem3 = WorkspaceItem.create(context, grandchildCol, false);
            Item item3 = InstallItem.installItem(context, wsItem3);
            item3.addMetadata("dc", "title", null, null, "GreatGrandchild Collection Item #1");
            item3.update();

            WorkspaceItem wsItem4 = WorkspaceItem.create(context, grandchildCol, false);
            Item item4 = InstallItem.installItem(context, wsItem4);
            item4.addMetadata("dc", "title", null, null, "GreatGrandchild Collection Item #2");
            item4.update();
            testItemHandle = item4.getHandle();

            // Commit these changes to our DB
            context.restoreAuthSystemState();
            context.complete();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (IOException ex)
        {
            log.error("IO Error in init", ex);
            fail("IO Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
    }
    
    /**
     * This method will be run once at the very end
     */
    @AfterClass
    public static void tearDownClass()
    {
        try
        {
            Context context = new Context();
            Community topCommunity = (Community) HandleManager.resolveToObject(context, topCommunityHandle);
            
            // Delete parent community and hierarchy under it
            if(topCommunity!=null)
            {
                log.info("tearDownClass() - DESTROY TEST HIERARCHY");
                context.turnOffAuthorisationSystem();
                topCommunity.delete();
                context.restoreAuthSystemState();
                context.complete();
            }
          
            if(context.isValid())
                context.abort();
        }
        catch (Exception ex)
        {
            log.error("Error in destroyTestData", ex);
        }
    }
    
    /**
     * Test restoration from AIP of entire Community Hierarchy
     */
    @Test
    public void testRestoreCommunityHierarchy() throws Exception
    {
        log.info("testRestoreCommunityHierarchy() - BEGIN");

        // Locate the top level community (from our test data)
        Community topCommunity = (Community) HandleManager.resolveToObject(context, topCommunityHandle);

        // Get parent object, so that we can restore to same parent later
        DSpaceObject parent = topCommunity.getParentObject();

        // Save basic info about top community (and children) to an infoMap
        HashMap<String,String> infoMap = new HashMap<String,String>();
        saveObjectInfo(topCommunity, infoMap);

        // Export this Community (recursively) to AIPs
        log.info("testRestoreCommunityHierarchy() - CREATE AIPs");
        File aipFile = createAIP(topCommunity, null, true, false);

        // Delete everything from parent community on down
        log.info("testRestoreCommunityHierarchy() - DELETE Community Hierarchy");
        context.turnOffAuthorisationSystem();
        topCommunity.delete();
        // Commit these changes to our DB
        context.commit();

        // Assert all objects in infoMap no longer exist in DSpace
        assertObjectsNotExist(infoMap);

        // Restore this Community (recursively) from AIPs
        log.info("testRestoreCommunityHierarchy() - RESTORE Community Hierarchy");
        restoreFromAIP(parent, aipFile, null, true);
        // Commit these changes to our DB
        context.commit();
        context.restoreAuthSystemState();
        
        // Assert all objects in infoMap now exist again!
        assertObjectsExist(infoMap);
        
        log.info("testRestoreCommunityHierarchy() - END");
    }
    
    /**
     * Test restoration from AIP of entire Community Hierarchy
     */
    @Test
    public void testReplaceCommunityHierarchy() throws Exception
    {
        log.info("testReplaceCommunityHierarchy() - BEGIN");

        // Locate the top level community (from our test data)
        Community topCommunity = (Community) HandleManager.resolveToObject(context, topCommunityHandle);

        // Get a list of all Collections under this Community or any Sub-Communities
        Collection[] collections = topCommunity.getAllCollections();
        
        // Get the count of collections
        int numberOfCollections = collections.length;
        
        // Export this Community (recursively) to AIPs
        log.info("testReplaceCommunityHierarchy() - CREATE AIPs");
        File aipFile = createAIP(topCommunity, null, true, false);
        
        // Get some basic info about Collection to be deleted
        Community parent = (Community) collections[0].getParentObject();
        String deletedColHandle = collections[0].getHandle();
        int numberOfItems = collections[0].countItems();
        
        // Now, delete that one collection
        log.info("testReplaceCommunityHierarchy() - DELETE Collection");
        context.turnOffAuthorisationSystem();
        parent.removeCollection(collections[0]);
        context.commit();
        
        // Assert the deleted collection no longer exists
        DSpaceObject obj = HandleManager.resolveToObject(context, deletedColHandle);
        assertThat("testReplaceCommunityHierarchy() collection " + deletedColHandle + " doesn't exist", obj, nullValue());
        
        // Replace Community (and all child objects, recursively) from AIPs
        log.info("testReplaceCommunityHierarchy() - REPLACE Community Hierarchy");
        replaceFromAIP(topCommunity, aipFile, null, true);
        // Commit these changes to our DB
        context.commit();
        context.restoreAuthSystemState();
        
        // Assert the deleted collection is RESTORED
        DSpaceObject objRestored = HandleManager.resolveToObject(context, deletedColHandle);
        assertThat("testReplaceCommunityHierarchy() collection " + deletedColHandle + " exists", objRestored, notNullValue());
        
        // Assert the Collection count and Item count are same as before
        assertEquals("testReplaceCommunityHierarchy() collection count", numberOfCollections, topCommunity.getAllCollections().length);
        assertEquals("testReplaceCommunityHierarchy() item count", numberOfItems, ((Collection)objRestored).countItems());
    
        log.info("testReplaceCommunityHierarchy() - END");
    }
    
    /**
     * Create AIP(s) based on a given DSpaceObject.
     * @param dso DSpaceObject to create AIP(s) for
     * @param pkParams any special PackageParameters to pass (if any)
     * @param recursive whether to recursively create AIPs or just a single AIP
     * @param overwrite whether to overwrite the local AIP file if it is found
     * @return exported root AIP file
     */
    private File createAIP(DSpaceObject dso, PackageParameters pkgParams, boolean recursive, boolean overwrite)
            throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        new NonStrictExpectations(ConfigurationManager.class)
        {{
            // Override default value of configured temp directory to point at our
            // JUnit TemporaryFolder. This ensures Crosswalk classes like RoleCrosswalk 
            // store their temp files in a place where JUnit can clean them up automatically.
            ConfigurationManager.getProperty("upload.temp.dir"); result = testFolder.getRoot().getAbsolutePath();
        }};
        
        // Get a reference to the configured "AIP" package disseminator
        PackageDisseminator dip = (PackageDisseminator) PluginManager
                    .getNamedPlugin(PackageDisseminator.class, "AIP");
        if (dip == null)
        {
            fail("Could not find a disseminator for type 'AIP'");
        }
        
        // Export file (this is placed in JUnit's temporary folder, so that it can be cleaned up after tests complete)
        File exportAIPFile = new File(testFolder.getRoot().getAbsolutePath() + File.separator + PackageUtils.getPackageName(dso, "zip"));
        
        // To save time, we'll skip re-exporting AIPs, unless overwrite == true
        if(!exportAIPFile.exists() || overwrite)
        {
            // If unspecified, set default PackageParameters
            if (pkgParams==null)
                pkgParams = new PackageParameters();

            // Actually disseminate the object(s) to AIPs
            if(recursive)
                dip.disseminateAll(context, dso, pkgParams, exportAIPFile);
            else
                dip.disseminate(context, dso, pkgParams, exportAIPFile);
        }
        
        return exportAIPFile;
    }
    
    /**
     * Restore DSpaceObject(s) from AIP(s).
     * @param parent The DSpaceObject which will be the parent object of the newly restored object(s)
     * @param aipFile AIP file to start restoration from
     * @param pkParams any special PackageParameters to pass (if any)
     * @param recursive whether to recursively restore AIPs or just a single AIP
     */
    private void restoreFromAIP(DSpaceObject parent, File aipFile, PackageParameters pkgParams, boolean recursive)
            throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        // Get a reference to the configured "AIP" package ingestor
        PackageIngester sip = (PackageIngester) PluginManager
                    .getNamedPlugin(PackageIngester.class, "AIP");
        if (sip == null)
        {
            fail("Could not find a ingestor for type 'AIP'");
        }
        
        // If unspecified, set default PackageParameters
        if (pkgParams==null)
            pkgParams = new PackageParameters();
        
        // Ensure restore mode is enabled
        pkgParams.setRestoreModeEnabled(true);
        
        // Actually disseminate the object(s) to AIPs
        if(recursive)
            sip.ingestAll(context, parent, aipFile, pkgParams, null);
        else
            sip.ingest(context, parent, aipFile, pkgParams, null);
    }
    
    /**
     * Replace DSpaceObject(s) from AIP(s).
     * @param dso The DSpaceObject to be replaced from AIP
     * @param aipFile AIP file to start replacement from
     * @param pkParams any special PackageParameters to pass (if any)
     * @param recursive whether to recursively restore AIPs or just a single AIP
     */
    private void replaceFromAIP(DSpaceObject dso, File aipFile, PackageParameters pkgParams, boolean recursive)
            throws PackageException, CrosswalkException, AuthorizeException, SQLException, IOException
    {
        // Get a reference to the configured "AIP" package ingestor
        PackageIngester sip = (PackageIngester) PluginManager
                    .getNamedPlugin(PackageIngester.class, "AIP");
        if (sip == null)
        {
            fail("Could not find a ingestor for type 'AIP'");
        }
        
        // If unspecified, set default PackageParameters
        if (pkgParams==null)
            pkgParams = new PackageParameters();
        
        // Ensure restore mode is enabled
        pkgParams.setRestoreModeEnabled(true);
        
        // Actually disseminate the object(s) to AIPs
        if(recursive)
            sip.replaceAll(context, dso, aipFile, pkgParams);
        else
            sip.replace(context, dso, aipFile, pkgParams);
    }
    
    /**
     * Save Object hierarchy info to the given HashMap. 
     * <P>
     * In HashMap, Key is the object handle, and Value is "[type-text]||[title]".
     * @param dso DSpaceObject
     * @param infoMap HashMap
     * @throws SQLException 
     */
    private void saveObjectInfo(DSpaceObject dso, HashMap<String,String> infoMap)
            throws SQLException
    {
        // We need the HashMap to be non-null
        if(infoMap==null)
            return;
        
        if(dso instanceof Community)
        {
            // Save this Community's info to the infoMap
            Community community = (Community) dso;
            infoMap.put(community.getHandle(), community.getTypeText() + valueseparator + community.getName());
            
            // Recursively call method for each SubCommunity
            Community[] subCommunities = community.getSubcommunities();
            for(Community c : subCommunities)
            {
                saveObjectInfo(c, infoMap);
            }
            
            // Recursively call method for each Collection
            Collection[] collections = community.getCollections();
            for(Collection c : collections)
            {
                saveObjectInfo(c, infoMap);
            }
        }
        else if(dso instanceof Collection)
        {
            // Save this Collection's info to the infoMap
            Collection collection = (Collection) dso;
            infoMap.put(collection.getHandle(), collection.getTypeText() + valueseparator + collection.getName());
            
            // Recursively call method for each Item in Collection
            ItemIterator items = collection.getItems();
            while(items.hasNext())
            {
                Item i = items.next();
                saveObjectInfo(i, infoMap);
            }
        }
        else if(dso instanceof Item)
        {
            // Save this Item's info to the infoMap
            Item item = (Item) dso;
            infoMap.put(item.getHandle(), item.getTypeText() + valueseparator + item.getName());
        }
    }
    
    /**
     * Assert the objects listed in a HashMap all exist in DSpace and have 
     * properties equal to HashMap value(s).
     * <P>
     * In HashMap, Key is the object handle, and Value is "[type-text]||[title]".
     * @param infoMap HashMap of objects to check for
     * @throws SQLException 
     */
    private void assertObjectsExist(HashMap<String,String> infoMap)
            throws SQLException
    {
        if(infoMap==null || infoMap.isEmpty())
            fail("Cannot assert against an empty infoMap");
        
        // Loop through everything in infoMap, and ensure it all exists
        for(String key : infoMap.keySet())
        {
            // The Key is the Handle, so make sure this object exists
            DSpaceObject obj = HandleManager.resolveToObject(context, key);
            assertThat("compareObjectInfo object " + key + " exists", obj, notNullValue());
            
            // Get the typeText & name of this object from the values
            String info = infoMap.get(key);
            String[] values = info.split(valueseparator);
            String typeText = values[0];
            String name = values[1];
            //log.info("For obj " + key + " found typeText=" + typeText + " and name=" + name);
            
            assertEquals("compareObjectInfo object " + key + " type", obj.getTypeText(), typeText);
            assertEquals("compareObjectInfo object " + key + " name", obj.getName(), name);
        }
        
    }
    
    /**
     * Assert the objects listed in a HashMap do NOT exist in DSpace.
     * @param infoMap HashMap of objects to check for
     * @throws SQLException 
     */
    public void assertObjectsNotExist(HashMap<String,String> infoMap)
            throws SQLException
    {
        if(infoMap==null || infoMap.isEmpty())
            fail("Cannot assert against an empty infoMap");
        
        // Loop through everything in infoMap, and ensure it all exists
        for(String key : infoMap.keySet())
        {
            // The key is the Handle, so make sure this object does NOT exist
            DSpaceObject obj = HandleManager.resolveToObject(context, key);
            assertThat("compareObjectInfo object " + key + " exists", obj, nullValue());
        }
    }
}
