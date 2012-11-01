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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 * An abstract implementation of a DSpace Package Ingester, which
 * implements a few helper/utility methods that most (all?) PackageIngesters
 * may find useful.
 * <P>
 * First, implements recursive functionality in ingestAll() and replaceAll()
 * methods of the PackageIngester interface.  These methods are setup to
 * recursively call ingest() and replace() respectively.
 * <P>
 * Finally, it also implements several utility methods (createDSpaceObject(),
 * finishCreateItem(), updateDSpaceObject()) which subclasses may find useful.
 * This classes will allow subclasses to easily create/update objects without
 * having to worry too much about normal DSpace submission workflows (which is
 * taken care of in these utility methods).
 * <P>
 * All Package ingesters should either extend this abstract class
 * or implement <code>PackageIngester</code> to better suit their needs.
 *
 * @author Tim Donohue
 * @see PackageIngester
 */
public abstract class AbstractPackageIngester
        implements PackageIngester
{
    /** log4j category */
    private static Logger log = Logger.getLogger(AbstractPackageIngester.class);

    /** 
     * References to other packages -- these are the next packages to ingest recursively
     * Key = DSpace Object just ingested, Value = List of all packages relating to a DSpaceObject
     **/
    private Map<DSpaceObject,List<String>> packageReferences = new HashMap<DSpaceObject,List<String>>();

    /**  List of all successfully ingested/replaced DSpace objects */
    private List<DSpaceObject> dsoIngestedList = new ArrayList<DSpaceObject>();

    /**
     * Recursively create one or more DSpace Objects out of the contents
     * of the ingested package (and all other referenced packages).
     * The initial object is created under the indicated parent.  All other
     * objects are created based on their relationship to the initial object.
     * <p>
     * For example, a scenario may be to create a Collection based on a
     * collection-level package, and also create an Item for every item-level
     * package referenced by the collection-level package.
     * <p>
     * The output of this method is one or more newly created <code>DspaceObject<code>s.
     * <p>
     * The packager <em>may</em> choose not to implement <code>ingestAll</code>,
     * or simply forward the call to <code>ingest</code> if it is unable to support
     * recursive ingestion.
     * <p>
     * The deposit license (Only significant for Item) is passed
     * explicitly as a string since there is no place for it in many
     * package formats.  It is optional and may be given as
     * <code>null</code>.
     *
     * @param context  DSpace context.
     * @param parent parent under which to create the initial object
     *        (may be null -- in which case ingester must determine parent from package
     *         or throw an error).
     * @param pkgFile  The initial package file to ingest
     * @param params Properties-style list of options (interpreted by each packager).
     * @param license  may be null, which takes default license.
     * @return List of DSpaceObjects created
     *
     * @throws PackageValidationException if initial package (or any referenced package)
     *          is unacceptable or there is a fatal error in creating a DSpaceObject
     * @throws UnsupportedOperationException if this packager does not
     *  implement <code>ingestAll</code>
     */
    @Override
    public List<DSpaceObject> ingestAll(Context context, DSpaceObject parent, File pkgFile,
                                PackageParameters params, String license)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException
    {
        //If unset, make sure the Parameters specifies this is a recursive ingest
        if(!params.recursiveModeEnabled())
        {
            params.setRecursiveModeEnabled(true);
        }

        //Initial DSpace Object to ingest
        DSpaceObject dso = null;

        //try to ingest the first package
        try
        {
            //actually ingest pkg using provided PackageIngester
            dso = ingest(context, parent, pkgFile, params, license);
        }
        catch(IllegalStateException ie)
        {
            // NOTE: if we encounter an IllegalStateException, this means the
            // handle is already in use and this object already exists.

            //if we are skipping over (i.e. keeping) existing objects
            if(params.keepExistingModeEnabled())
            {
                log.warn(LogManager.getHeader(context, "skip_package_ingest", "Object already exists, package-skipped=" + pkgFile));
            }
            else // Pass this exception on -- which essentially causes a full rollback of all changes (this is the default)
            {
                throw ie;
            }
        }

        //as long as our first object was ingested successfully
        if(dso!=null)
        {
            //add to list of successfully ingested objects
            addToIngestedList(dso);

            //We can only recursively ingest non-Items
            //(NOTE: Items have no children, as Bitstreams/Bundles are created from Item packages)
            if(dso.getType()!=Constants.ITEM)
            {
                //Check if we found child package references when ingesting this latest DSpaceObject
                List<String> childPkgRefs = getPackageReferences(dso);
                
                //we can only recursively ingest child packages
                //if we have references to them 
                if(childPkgRefs!=null && !childPkgRefs.isEmpty())
                {
                    //Recursively ingest each child package, using this current object as the parent DSpace Object
                    for(String childPkgRef : childPkgRefs)
                    {
                        // Remember where the additions start
                        int oldSize = dsoIngestedList.size();

                        //Assume package reference is relative to current package location
                        File childPkg = new File(pkgFile.getAbsoluteFile().getParent(), childPkgRef);
                        
                        //fun, it's recursive! -- ingested referenced package as a child of current object
                        ingestAll(context, dso, childPkg, params, license);

                        // A Collection can map to Items that it does not "own".
                        // If a Collection package has an Item as a child, it
                        // should be mapped regardless of ownership.
                        // Note: Only perform this mapping if new items were ingested to this collection
                        if (Constants.COLLECTION == dso.getType() && dsoIngestedList.size()>oldSize)
                        {
                            // Since running 'ingestAll' on an item, will only ingest one Item at most,
                            // Just make sure that item is mapped to this collection.
                            Item childItem = (Item)dsoIngestedList.get(oldSize);
                            Collection collection = (Collection)dso;
                            if (!childItem.isIn(collection))
                            {
                                collection.addItem(childItem);
                            }
                        }
                    }
                }//end if child pkgs
            }//end if not an Item
        }//end if DSpaceObject not null

        //Return list of all objects ingested
        return getIngestedList();
    }


    /**
     * Recursively replace one or more DSpace Objects out of the contents
     * of the ingested package (and all other referenced packages).
     * The initial object to replace is indicated by <code>dso</code>.  All other
     * objects are replaced based on information provided in the referenced packages.
     * <p>
     * For example, a scenario may be to replace a Collection based on a
     * collection-level package, and also replace *every* Item in that collection
     * based on the item-level packages referenced by the collection-level package.
     * <p>
     * Please note that since the <code>dso</code> input only specifies the
     * initial object to replace, any additional objects to replace must be
     * determined based on the referenced packages (or initial package itself).
     * <p>
     * The output of this method is one or more replaced <code>DspaceObject<code>s.
     * <p>
     * The packager <em>may</em> choose not to implement <code>replaceAll</code>,
     * since it somewhat contradicts the archival nature of DSpace. It also
     * may choose to forward the call to <code>replace</code> if it is unable to
     * support recursive replacement.
     *
     * @param context  DSpace context.
     * @param dso initial existing DSpace Object to be replaced, may be null
     *            if object to replace can be determined from package
     * @param pkgFile  The package file to ingest.
     * @param params Properties-style list of options specific to this packager
     * @return List of DSpaceObjects replaced
     *
     * @throws PackageValidationException if initial package (or any referenced package)
     *          is unacceptable or there is a fatal error in creating a DSpaceObject
     * @throws UnsupportedOperationException if this packager does not
     *  implement <code>replaceAll</code>
     */
    @Override
    public List<DSpaceObject> replaceAll(Context context, DSpaceObject dso,
                                File pkgFile, PackageParameters params)
        throws PackageException, UnsupportedOperationException,
               CrosswalkException, AuthorizeException,
               SQLException, IOException
    {
        //If unset, make sure the Parameters specifies this is a recursive replace
        if(!params.recursiveModeEnabled())
        {
            params.setRecursiveModeEnabled(true);
        }

        //actually ingest pkg using provided PackageIngester, and replace object
        //NOTE: 'dso' may be null! If it is null, the PackageIngester must determine
        //      the object to be replaced from the package itself.
        DSpaceObject replacedDso = replace(context, dso, pkgFile, params);

        //as long as our object was replaced successfully
        if(replacedDso!=null)
        {
            //add to list of successfully replaced objects
            addToIngestedList(replacedDso);

            //We can only recursively replace non-Items
            //(NOTE: Items have no children, as Bitstreams/Bundles are created from Item packages)
            if(replacedDso.getType()!=Constants.ITEM)
            {
                //Check if we found child package references when replacing this latest DSpaceObject
                List<String> childPkgRefs = getPackageReferences(replacedDso);

                //we can only recursively ingest child packages
                //if we have references to them
                if(childPkgRefs!=null && !childPkgRefs.isEmpty())
                {
                    //Recursively replace each child package
                    for(String childPkgRef : childPkgRefs)
                    {
                        // Remember where the additions start
                        int oldSize = dsoIngestedList.size();

                        //Assume package reference is relative to current package location
                        File childPkg = new File(pkgFile.getAbsoluteFile().getParent(), childPkgRef);

                        //fun, it's recursive! -- replaced referenced package as a child of current object
                        // Pass object to replace as 'null', as we don't know which object to replace.
                        replaceAll(context, null, childPkg, params);

                        // A Collection can map to Items that it does not "own".
                        // If a Collection package has an Item as a child, it
                        // should be mapped regardless of ownership.

                        // If a Collection package has an Item as a child, it
                        // should be mapped regardless of ownership.
                        // Note: Only perform this mapping if new items were ingested to this collection
                        if (Constants.COLLECTION == replacedDso.getType() && dsoIngestedList.size()>oldSize)
                        {
                            // Since running 'replaceAll' on an item, will only ingest one Item at most,
                            // Just make sure that item is mapped to this collection.
                            Item childItem = (Item)dsoIngestedList.get(oldSize);
                            Collection collection = (Collection)replacedDso;
                            if (!childItem.isIn(collection))
                            {
                                collection.addItem(childItem);
                            }
                        }
                    }
                }//end if child pkgs
            }//end if not an Item
        }//end if DSpaceObject not null

        //Return list of all objects replaced
        return getIngestedList();
    }

   
    /**
     * During ingestion process, some submission information packages (SIPs)
     * may reference other packages to be ingested (recursively).
     * <P>
     * This method collects all references to other packages, so that we
     * can choose to recursively ingest them, as necessary, alongside the
     * DSpaceObject created from the original SIP.
     * <P>
     * References are collected based on the DSpaceObject created from the SIP
     * (this way we keep the context of these references).
     *
     * @param dso DSpaceObject whose SIP referenced another package
     * @param packageRef A reference to another package, which can be ingested after this one
     */
    public void addPackageReference(DSpaceObject dso, String packageRef)
    {
        List<String> packageRefValues = null;

        // Check if we already have an entry for packages reference by this object
        if(packageReferences.containsKey(dso))
        {
            packageRefValues = packageReferences.get(dso);
        }
        else
        {
            //Create a new empty list of references
            packageRefValues = new ArrayList<String>();
        }

        //add this package reference to existing list and save
        packageRefValues.add(packageRef);
        packageReferences.put(dso, packageRefValues);
    }

    /**
     * Return a list of known SIP references from a newly created DSpaceObject.
     * <P>
     * These references should detail where another package exists which
     * should be ingested alongside the current DSpaceObject.
     * <P>
     * The <code>AbstractPackageIngester</code> or an equivalent SIP handler is expected
     * to understand how to deal with these package references.
     *
     * @param dso DSpaceObject whose SIP referenced other SIPs
     * @return List of Strings which are the references to external submission ingestion packages
     *         (may be null if no SIPs were referenced)
     */
    public List<String> getPackageReferences(DSpaceObject dso)
    {
        return packageReferences.get(dso);
    }

    /**
     * Add DSpaceObject to list of successfully ingested/replaced objects
     * @param dso DSpaceObject
     */
    protected void addToIngestedList(DSpaceObject dso)
    {
        //add to list of successfully ingested objects
        if(!dsoIngestedList.contains(dso))
        {
            dsoIngestedList.add(dso);
        }
    }

    /**
     * Return List of all DSpaceObjects which have been ingested/replaced by
     * this instance of the Ingester.
     * <P>
     * This list can be useful in reporting back to the user what content has
     * been added or replaced.  It's used by ingestAll() and replaceAll() to
     * return this list of everything that was ingested/replaced.
     *
     * @return List of DSpaceObjects which have been added/replaced
     */
    protected List<DSpaceObject> getIngestedList()
    {
        return dsoIngestedList;
    }
}
