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
import org.jdom2.Namespace;

/**
 * Crosswalk descriptive metadata to and from DIM (DSpace Intermediate
 * Metadata) format, strictly for the purpose of including a precise and
 * complete record of the DMD in an AIP.  Although the DIM format was never
 * intended to be used outside of DSpace,  it is admirably suited to
 * describing the exact state of the descriptive MD stored in the RDBMS.
 * All crosswalks to standard formats such as MODS and even DC are necessarily
 * "lossy" and inexact.  Since the purpose of an AIP is to preserve and restore
 * the state of an object exactly, DIM is the preferred format for
 * recording its descriptive MD.
 * <p>
 * In order to allow external applications to make sense of DSpace AIPs for
 * preservation purposes, we recommend adding a parallel descriptive
 * metadata section in one of the preferred standard formats such as MODS
 * as well as the DIM.
 *
 * @author Larry Stone
 * @version $Revision: 1.2 $
 */
public class AIPDIMCrosswalk
    implements DisseminationCrosswalk, IngestionCrosswalk {
    /**
     * Get XML namespaces of the elements this crosswalk may return.
     * Returns the XML namespaces (as JDOM objects) of the root element.
     *
     * @return array of namespaces, which may be empty.
     */
    @Override
    public Namespace[] getNamespaces() {
        Namespace result[] = new Namespace[1];
        result[0] = XSLTCrosswalk.DIM_NS;
        return result;
    }

    /**
     * Get the XML Schema location(s) of the target metadata format.
     * Returns the string value of the <code>xsi:schemaLocation</code>
     * attribute that should be applied to the generated XML.
     * <p>
     * It may return the empty string if no schema is known, but crosswalk
     * authors are strongly encouraged to implement this call so their output
     * XML can be validated correctly.
     *
     * @return SchemaLocation string, including URI namespace, followed by
     * whitespace and URI of XML schema document, or empty string if unknown.
     */
    @Override
    public String getSchemaLocation() {
        return "";
    }

    /**
     * Predicate: Can this disseminator crosswalk the given object.
     * Needed by OAI-PMH server implementation.
     *
     * @param dso dspace object, e.g. an <code>Item</code>.
     * @return true when disseminator is capable of producing metadata.
     */
    @Override
    public boolean canDisseminate(DSpaceObject dso) {
        return true;
    }

    /**
     * Predicate: Does this disseminator prefer to return a list of Elements,
     * rather than a single root Element?
     * <p>
     * Some metadata formats have an XML schema without a root element,
     * for example, the Dublin Core and Qualified Dublin Core formats.
     * This would be <code>true</code> for a crosswalk into QDC, since
     * it would "prefer" to return a list, since any root element it has
     * to produce would have to be part of a nonstandard schema.  In
     * most cases your implementation will want to return
     * <code>false</code>
     *
     * @return true when disseminator prefers you call disseminateList().
     */
    @Override
    public boolean preferList() {
        return false;
    }

    /**
     * Execute crosswalk, returning List of XML elements.
     * Returns a <code>List</code> of JDOM <code>Element</code> objects representing
     * the XML produced by the crosswalk.  This is typically called when
     * a list of fields is desired, e.g. for embedding in a METS document
     * <code>xmlData</code> field.
     * <p>
     * When there are no results, an
     * empty list is returned, but never <code>null</code>.
     *
     * @param context context
     * @param dso     the  DSpace Object whose metadata to export.
     * @return results of crosswalk as list of XML elements.
     * @throws CrosswalkInternalException  (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace
     *                                     object.
     * @throws IOException                 I/O failure in services this calls
     * @throws SQLException                Database failure in services this calls
     * @throws AuthorizeException          current user not authorized for this operation.
     */
    @Override
    public List<Element> disseminateList(Context context, DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
        AuthorizeException {
        Element dim = disseminateElement(context, dso);
        return dim.getChildren();
    }

    /**
     * Execute crosswalk, returning one XML root element as
     * a JDOM <code>Element</code> object.
     * This is typically the root element of a document.
     * <p>
     *
     * @param context context
     * @param dso     the  DSpace Object whose metadata to export.
     * @return root Element of the target metadata, never <code>null</code>
     * @throws CrosswalkInternalException  (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace
     *                                     object.
     * @throws IOException                 I/O failure in services this calls
     * @throws SQLException                Database failure in services this calls
     * @throws AuthorizeException          current user not authorized for this operation.
     */
    @Override
    public Element disseminateElement(Context context, DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
        AuthorizeException {
        return XSLTDisseminationCrosswalk.createDIM(dso);
    }

    /**
     * Ingest a whole document.  Build Document object around root element,
     * and feed that to the transformation, since it may get handled
     * differently than a List of metadata elements.
     *
     * @param createMissingMetadataFields whether to create missing fields
     * @throws CrosswalkException if crosswalk error
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        ingest(context, dso, root.getChildren(), createMissingMetadataFields);
    }

    /**
     * Fields correspond directly to Item.addMetadata() calls so
     * they are simply executed.
     *
     * @param createMissingMetadataFields whether to create missing fields
     * @param dimList                     List of elements
     * @throws CrosswalkException if crosswalk error
     * @throws IOException        if IO error
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> dimList, boolean createMissingMetadataFields)
        throws CrosswalkException,
        IOException, SQLException, AuthorizeException {
        XSLTIngestionCrosswalk.ingestDIM(context, dso, dimList, createMissingMetadataFields);
    }
}
