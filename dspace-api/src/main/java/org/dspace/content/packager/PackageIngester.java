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
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;


/**
 * Plugin Interface to interpret a Submission Information Package (SIP)
 * and create (or replace) a DSpace Object from its contents.
 * <p>
 * A package is a single data stream containing enough information to
 * construct an Object (i.e.  an Item, Collection, or Community).  It
 * can be anything from an archive like a Zip file with a manifest and
 * metadata, to a simple manifest containing external references to the
 * content, to a self-contained file such as a PDF.  The interpretation
 * of the package is entirely at the discretion of the implementing
 * class.
 * <p>
 * The ingest methods are also given an attribute-value
 * list of "parameters"  which may modify their actions.
 * The parameters list is a generalized mechanism to pass parameters
 * from the requestor to the packager, since different packagers will
 * understand different sets of parameters.
 *
 * @author Larry Stone
 * @author Tim Donohue
 * @version $Revision$
 * @see PackageParameters
 * @see AbstractPackageIngester
 */
public interface PackageIngester
{
    /**
     * Create new DSpaceObject out of the ingested package.  The object
     * is created under the indicated parent.  This creates a
     * <code>DSpaceObject</code>.  For Items, it is up to the caller to
     * decide whether to install it or submit it to normal DSpace Workflow.
     * <p>
     * The deposit license (Only significant for Item) is passed
     * explicitly as a string since there is no place for it in many
     * package formats.  It is optional and may be given as
     * <code>null</code>.
     * <p>
     * Use <code>ingestAll</code> method to perform a recursive ingest of all
     * packages which are referenced by an initial package.
     *
     * @param context  DSpace context.
     * @param parent parent under which to create new object
     *        (may be null -- in which case ingester must determine parent from package
     *         or throw an error).
     * @param pkgFile  The package file to ingest
     * @param params Properties-style list of options (interpreted by each packager).
     * @param license  may be null, which takes default license.
     * @return DSpaceObject created by ingest.
     *
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into a DSpaceObject.
     * @throws CrosswalkException if crosswalk error
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws WorkflowException if workflow error
     */
    DSpaceObject ingest(Context context, DSpaceObject parent, File pkgFile,
                         PackageParameters params, String license)
            throws PackageException, CrosswalkException,
            AuthorizeException, SQLException, IOException, WorkflowException;


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
     * The output of this method is one or more newly created DSpaceObject Identifiers
     * (i.e. Handles).
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
     * @return List of Identifiers of DSpaceObjects created
     *
     * @throws PackageValidationException if initial package (or any referenced package)
     *          is unacceptable or there is a fatal error in creating a DSpaceObject
     * @throws UnsupportedOperationException if this packager does not
     *  implement <code>ingestAll</code>
     * @throws CrosswalkException if crosswalk error
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws WorkflowException if workflow error
     */
    List<String> ingestAll(Context context, DSpaceObject parent, File pkgFile,
                                PackageParameters params, String license)
            throws PackageException, UnsupportedOperationException,
            CrosswalkException, AuthorizeException,
            SQLException, IOException, WorkflowException;

    /**
     * Replace an existing DSpace Object with contents of the ingested package.
     * The packager <em>may</em> choose not to implement <code>replace</code>,
     * since it somewhat contradicts the archival nature of DSpace.
     * The exact function of this method is highly implementation-dependent.
     * <p>
     * Use <code>replaceAll</code> method to perform a recursive replace of
     * objects referenced by a set of packages.
     *
     * @param context  DSpace context.
     * @param dso existing DSpace Object to be replaced, may be null
     *            if object to replace can be determined from package
     * @param pkgFile  The package file to ingest.
     * @param params Properties-style list of options specific to this packager
     * @return DSpaceObject with contents replaced
     *
     * @throws PackageValidationException if package is unacceptable or there is
     *  a fatal error turning it into an Item.
     * @throws UnsupportedOperationException if this packager does not
     *  implement <code>replace</code>.
     * @throws CrosswalkException if crosswalk error
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws WorkflowException if workflow error
     */
    DSpaceObject replace(Context context, DSpaceObject dso,
                            File pkgFile, PackageParameters params)
            throws PackageException, UnsupportedOperationException,
            CrosswalkException, AuthorizeException,
            SQLException, IOException, WorkflowException;

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
     * The output of this method is one or more replaced DSpaceObject Identifiers
     * (i.e. Handles).
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
     * @return List of Identifiers of DSpaceObjects replaced
     *
     * @throws PackageValidationException if initial package (or any referenced package)
     *          is unacceptable or there is a fatal error in creating a DSpaceObject
     * @throws UnsupportedOperationException if this packager does not
     *  implement <code>replaceAll</code>
     * @throws CrosswalkException if crosswalk error
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws WorkflowException if workflow error
     */
    List<String> replaceAll(Context context, DSpaceObject dso,
                                File pkgFile, PackageParameters params)
            throws PackageException, UnsupportedOperationException,
            CrosswalkException, AuthorizeException,
            SQLException, IOException, WorkflowException;


    /**
     * Returns a user help string which should describe the
     * additional valid command-line options that this packager
     * implementation will accept when using the <code>-o</code> or
     * <code>--option</code> flags with the Packager script.
     *
     * @return a string describing additional command-line options available
     * with this packager
     */
    String getParameterHelp();
}
