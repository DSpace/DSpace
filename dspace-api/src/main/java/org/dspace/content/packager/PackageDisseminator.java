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

/**
 * Plugin Interface to produce Dissemination Information Package (DIP)
 * of a DSpace object.
 * <p>
 * An implementation translates DSpace objects to some external
 * "package" format.  A package is a single data stream (or file)
 * containing enough information to reconstruct the object.  It can be
 * anything from an archive like a Zip file with a manifest and metadata,
 * to a simple manifest containing external references to the content,
 * to a self-contained file such as a PDF.
 * <p>
 * A DIP implementation has two methods: <code>disseminate</code>
 * to produce the package itself, and <code>getMIMEType</code> to
 * identify its Internet format type (needed when transmitting the package
 * over HTTP).
 * <p>
 * Both of these methods are given an attribute-values
 * list of "parameters", which may modify their actions.  Since the
 * format output by <code>disseminate</code> may be affected by
 * parameters, it is given to the <code>getMIMEType</code> method as well.
 * The parameters list is a generalized mechanism to pass parameters
 * from the package requestor to the packager, since different packagers will
 * understand different sets of parameters.
 *
 * @author Larry Stone
 * @version $Revision$
 * @see PackageParameters
 */
public interface PackageDisseminator
{
    /**
     * Export the object (Item, Collection, or Community) as a
     * "package" on the indicated OutputStream.  Package is any serialized
     * representation of the item, at the discretion of the implementing
     * class.  It does not have to include content bitstreams.
     * <p>
     * Use the <code>params</code> parameter list to adjust the way the
     * package is made, e.g. including a "<code>metadataOnly</code>"
     * parameter might make the package a bare manifest in XML
     * instead of a Zip file including manifest and contents.
     * <p>
     * Throws an exception of the chosen object is not acceptable or there is
     * a failure creating the package.
     *
     * @param context  DSpace context.
     * @param object  DSpace object (item, collection, etc)
     * @param params Properties-style list of options specific to this packager
     * @param pkgFile File where export package should be written
     * @throws PackageValidationException if package cannot be created or there is
     *  a fatal error in creating it.
     * @throws CrosswalkException if crosswalk error
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    void disseminate(Context context, DSpaceObject object,
                     PackageParameters params, File pkgFile)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;

    /**
     * Recursively export one or more DSpace Objects as a series of packages.
     * This method will export the given DSpace Object as well as all referenced
     * DSpaceObjects (e.g. child objects) into a series of packages. The
     * initial object is exported to the location specified by the pkgFile.
     * All other generated packages are recursively exported to the same directory.
     * <p>
     * Package is any serialized representation of the item, at the discretion
     * of the implementing class.  It does not have to include content bitstreams.
     * <p>
     * Use the <code>params</code> parameter list to adjust the way the
     * package is made, e.g. including a "<code>metadataOnly</code>"
     * parameter might make the package a bare manifest in XML
     * instead of a Zip file including manifest and contents.
     * <p>
     * Throws an exception of the initial object is not acceptable or there is
     * a failure creating the packages.
     * <p>
     * A packager <em>may</em> choose not to implement <code>disseminateAll</code>,
     * or simply forward the call to <code>disseminate</code> if it is unable to
     * support recursive dissemination.
     *
     * @param context  DSpace context.
     * @param dso  initial DSpace object
     * @param params Properties-style list of options specific to this packager
     * @param pkgFile File where initial package should be written. All other
     *          packages will be written to the same directory as this File.
     * @return List of all package Files which were successfully disseminated
     * @throws PackageValidationException if package cannot be created or there is
     *  a fatal error in creating it.
     * @throws CrosswalkException if crosswalk error
     * @throws AuthorizeException if authorization error
     * @throws SQLException if database error
     * @throws IOException if IO error
     */
    List<File> disseminateAll(Context context, DSpaceObject dso,
                     PackageParameters params, File pkgFile)
        throws PackageException, CrosswalkException,
               AuthorizeException, SQLException, IOException;


    /**
     * Identifies the MIME-type of this package, e.g. <code>"application/zip"</code>.
     * Required when sending the package via HTTP, to
     * provide the Content-Type header.
     *
     * @param params Package Parameters
     * @return the MIME type (content-type header) of the package to be returned
     */
    String getMIMEType(PackageParameters params);


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
