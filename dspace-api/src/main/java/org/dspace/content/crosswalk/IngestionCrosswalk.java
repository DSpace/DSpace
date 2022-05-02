/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.jdom2.Element;

/**
 * Ingestion Crosswalk plugin -- translate an external metadata format
 * into DSpace native metadata.
 * <p>
 * This describes a plugin that translates an external XML
 * metadata format (e.g.  MODS) into the DSpace internal metadata
 * representation.  A crosswalk plugin may operate on different kinds of
 * DSpace Objects, so the concept of "metadata" encompasses the
 * qualified Dublin Core fields on Items, properties of Bitstreams, and
 * metadata on Collections and Communities.
 * <p>
 *
 * @author Larry Stone
 * @version $Revision$
 */
public interface IngestionCrosswalk {
    /**
     * Crosswalk metadata from external XML representation to DSpace
     * internal representations.  This version accepts metadata as a
     * <code>List</code> of JDOM XML elements.  It interprets the
     * contents of each element and adds the appropriate values to the
     * DSpace Object's internal metadata represenation.
     * <p>
     * Note that this method may be called several times for the same target
     * Item, if the metadata comes as several lists of elements, so it should
     * not add fixed metadata values on each or they may appear multiples times.
     * <p>
     * NOTE:  <br>
     * Most XML metadata standards (e.g.  MODS) are defined as a "root"
     * element which contains a sequence of "fields" that have the
     * descriptive information.  Some metadata containers have a
     * "disembodied" list of fields, rather than the root element, so
     * this <code>ingest</code> method is intended to accept that bare
     * list of fields.  However, it must also accept a list containing
     * only the "root" element for the metadata structure (e.g.  the
     * "mods:mods" wrapper in a MODS expression) as a member of the
     * list.  It can handle this case by calling the single-element
     * version of ingest() on the "root" element.
     * <p>
     * Some callers of the crosswalk plugin may not be careful about (or
     * capable of) choosing whether the list or element version should
     * be called.
     * <p>
     *
     * @param context                     DSpace context.
     * @param dso                         DSpace Object (Item, Bitstream, etc) to which new metadata gets attached.
     * @param metadata                    List of XML Elements of metadata
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkInternalException  (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk into this kind of
     *                                     DSpace object.
     * @throws MetadataValidationException (<code>CrosswalkException</code>)  metadata format was not acceptable or
     *                                     missing required elements.
     * @throws IOException                 I/O failure in services this calls
     * @throws SQLException                Database failure in services this calls
     * @throws AuthorizeException          current user not authorized for this operation.
     */
    public abstract void ingest(Context context, DSpaceObject dso, List<Element> metadata,
                                boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException;

    /**
     * Crosswalk metadata from external XML representation to DSpace
     * internal representations.  This version accepts a single "root"
     * element of the XML metadata.
     * <p>
     * It is otherwise just like the <code>List</code> form of
     * <code>ingest()</code> above.
     * <p>
     *
     * @param context                     DSpace context.
     * @param dso                         DSpace Object (usually an Item) to which new metadata gets attached.
     * @param root                        root Element of metadata document.
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkInternalException  (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk into this kind of
     *                                     DSpace object.
     * @throws MetadataValidationException (<code>CrosswalkException</code>)  metadata format was not acceptable or
     *                                     missing required elements.
     * @throws IOException                 I/O failure in services this calls
     * @throws SQLException                Database failure in services this calls
     * @throws AuthorizeException          current user not authorized for this operation.
     */
    public abstract void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException;
}
