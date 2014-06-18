/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;


/**
 * A class implementing this interface can crosswalk metadata directly
 * from a stream (assumed to be in a specific format) to the object.
 * <p>
 * Stream-oriented crosswalks are intended to be used for metadata
 * formats which are either (a) not XML-based, or (b) too bulky for the
 * DOM-ish in-memory model developed for the METS and IMSCP packagers.
 * The METS packagers (all subclasses of AbstractMETSDisseminator / AbstractMETSIngester
 * are equipped to call these crosswalks as well as the XML-based ones,
 * just refer to the desired crosswalk by its plugin name.
 *
 * @author  Larry Stone
 * @version $Revision$
 */
public interface StreamIngestionCrosswalk
{
    /**
     * Execute crosswalk on the given object, taking input from the stream.
     *
     * @param context the DSpace context
     * @param dso the  DSpace Object whose metadata is being ingested.
     * @param in input stream containing the metadata.
     * @param MIMEType MIME type of the ???
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public void ingest(Context context, DSpaceObject dso, InputStream in, String MIMEType)
        throws CrosswalkException, IOException, SQLException, AuthorizeException;
}
