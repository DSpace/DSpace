/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract implementation of a DSpace Package Disseminator, which
 * implements a few helper/utility methods that most (all?) PackageDisseminators
 * may find useful.
 * <P>
 * First, implements recursive functionality in the disseminateAll()
 * method of the PackageIngester interface.  This method is setup to
 * recursively call disseminate() method.
 * <P>
 * All Package disseminators should either extend this abstract class
 * or implement <code>PackageDisseminator</code> to better suit their needs.
 *
 * @author Tim Donohue
 * @see PackageDisseminator
 * @see org.dspace.core.service.PluginService
 */
public abstract class AbstractPackageDisseminator
        implements PackageDisseminator
{
    /**  List of all successfully disseminated package files */
    private List<File> packageFileList = new ArrayList<File>();

    protected final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Recursively export one or more DSpace Objects as a series of packages.
     * This method will export the given DSpace Object as well as all referenced
     * DSpaceObjects (e.g. child objects) into a series of packages. The
     * initial object is exported to the location specified by the OutputStream.
     * All other packages are exported to the same directory location.
     * <p>
     * Package is any serialized representation of the item, at the discretion
     * of the implementing class.  It does not have to include content bitstreams.
     * <br>
     * Use the <code>params</code> parameter list to adjust the way the
     * package is made, e.g. including a "<code>metadataOnly</code>"
     * parameter might make the package a bare manifest in XML
     * instead of a Zip file including manifest and contents.
     * <br>
     * Throws an exception of the initial object is not acceptable or there is
     * a failure creating the package.
     *
     * @param context  DSpace context.
     * @param dso  initial DSpace object
     * @param params Properties-style list of options specific to this packager
     * @param pkgFile File where initial package should be written. All other
     *          packages will be written to the same directory as this File.
     * @throws PackageValidationException if package cannot be created or there is
     *  a fatal error in creating it.
     * @throws CrosswalkException if crosswalk error
     * @throws IOException if IO error
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public List<File> disseminateAll(Context context, DSpaceObject dso,
                     PackageParameters params, File pkgFile)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException
    {
        //If unset, make sure the Parameters specifies this is a recursive dissemination
        if(!params.recursiveModeEnabled())
        {
            params.setRecursiveModeEnabled(true);
        }

        // If this object package has NOT already been disseminated
        // NOTE: This ensures we don't accidentally disseminate the same object
        // TWICE, e.g. when an Item is mapped into multiple Collections.
        if(!getPackageList().contains(pkgFile))
        {
            // Disseminate the object using provided PackageDisseminator
            disseminate(context, dso, params, pkgFile);
        }

        //check if package was disseminated
        if(pkgFile.exists())
        {
            //add to list of successfully disseminated packages
            addToPackageList(pkgFile);

            //We can only recursively disseminate non-Items
            //(NOTE: Items have no children, as Bitstreams/Bundles are created from Item packages)
            if(dso.getType()!=Constants.ITEM)
            {
                //Determine where first file package was disseminated to, as all
                //others will be written to same directory
                String pkgDirectory = pkgFile.getCanonicalFile().getParent();
                if(!pkgDirectory.endsWith(File.separator))
                {
                    pkgDirectory += File.separator;
                }
                String fileExtension = PackageUtils.getFileExtension(pkgFile.getName());

                //recursively disseminate content, based on object type
                switch (dso.getType())
                {
                    case Constants.COLLECTION :
                        //Also find all Items in this Collection and disseminate
                        Collection collection = (Collection) dso;
                        Iterator<Item> iterator = itemService.findByCollection(context, collection);
                        while(iterator.hasNext())
                        {
                            Item item = iterator.next();

                            //disseminate all items (recursively!)
                            String childFileName = pkgDirectory + PackageUtils.getPackageName(item, fileExtension);
                            disseminateAll(context, item, params, new File(childFileName));
                        }

                        break;
                    case Constants.COMMUNITY :
                        //Also find all SubCommunities in this Community and disseminate
                        Community community = (Community) dso;
                        List<Community> subcommunities = community.getSubcommunities();
                        for (Community subcommunity : subcommunities)
                        {
                            //disseminate all sub-communities (recursively!)
                            String childFileName = pkgDirectory + PackageUtils.getPackageName(subcommunity, fileExtension);
                            disseminateAll(context, subcommunity, params, new File(childFileName));
                        }

                        //Also find all Collections in this Community and disseminate
                        List<Collection> collections = community.getCollections();
                        for(int i=0; i<collections.size(); i++)
                        {
                            //disseminate all collections (recursively!)
                            String childFileName = pkgDirectory + PackageUtils.getPackageName(collections.get(i), fileExtension);
                            disseminateAll(context, collections.get(i), params, new File(childFileName));
                        }

                        break;
                    case Constants.SITE :
                        //Also find all top-level Communities and disseminate
                        List<Community> topCommunities = communityService.findAllTop(context);
                        for (Community topCommunity : topCommunities)
                        {
                            //disseminate all top-level communities (recursively!)
                            String childFileName = pkgDirectory + PackageUtils.getPackageName(topCommunity, fileExtension);
                            disseminateAll(context, topCommunity, params, new File(childFileName));
                        }

                        break;
                }//end switch
            }//end if not an Item
        }//end if pkgFile exists

        //return list of all successfully disseminated packages
        return getPackageList();
    }

    /**
     * Add File to list of successfully disseminated package files
     * @param f added File.
     */
    protected void addToPackageList(File f)
    {
        //add to list of successfully disseminated packages
        if(!packageFileList.contains(f))
        {
            packageFileList.add(f);
        }
    }

    /**
     * Return List of all package Files which have been disseminated
     * this instance of the Disseminator.
     * <P>
     * This list can be useful in reporting back to the user what content has
     * been disseminated as packages.  It's used by disseminateAll() to report
     * what packages were created.
     *
     * @return List of Files which correspond to the disseminated packages
     */
    protected List<File> getPackageList()
    {
        return packageFileList;
    }
}
