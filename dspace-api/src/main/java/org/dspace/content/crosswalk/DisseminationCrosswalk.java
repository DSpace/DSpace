/*
 * DisseminationCrosswalk.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.content.crosswalk;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.content.Item;
import org.dspace.content.DSpaceObject;
import org.dspace.authorize.AuthorizeException;

import org.jdom.Element;
import org.jdom.Namespace;

/**
 * Dissemination Crosswalk plugin -- translate DSpace native
 * metadata into an external XML format.
 * <p>
 * This interface describes a plugin that produces metadata in an XML-based
 * format from the state of a DSpace object.  Note that the object
 * may be an Item, Bitstream, Community, or Collection, although most
 * implementations only work on one type of object.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public interface DisseminationCrosswalk
{
    /** XSI namespace, required for xsi:schemalocation attributes */
    static final Namespace XSI_NS =
        Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    /**
     * Get XML namespaces of the elements this crosswalk may return.
     * Returns the XML namespaces (as JDOM objects) of the root element.
     *
     * @return array of namespaces, which may be empty.
     */
    public Namespace[] getNamespaces();

    /**
     * Get the XML Schema location(s) of the target metadata format.
     * Returns the string value of the <code>xsi:schemaLocation</code>
     * attribute that should be applied to the generated XML.
     *  <p>
     * It may return the empty string if no schema is known, but crosswalk
     * authors are strongly encouraged to implement this call so their output
     * XML can be validated correctly.
     * @return SchemaLocation string, including URI namespace, followed by
     *  whitespace and URI of XML schema document, or empty string if unknown.
     */
    public String getSchemaLocation();

    /**
     * Predicate: Can this disseminator crosswalk the given object.
     * Needed by OAI-PMH server implementation.
     *
     * @param dso  dspace object, e.g. an <code>Item</code>.
     * @return true when disseminator is capable of producing metadata.
     */
    public boolean canDisseminate(DSpaceObject dso);

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
    public boolean preferList();

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
     * @param dso the  DSpace Object whose metadata to export.
     * @return results of crosswalk as list of XML elements.
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public List disseminateList(DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
               AuthorizeException;

    /**
     * Execute crosswalk, returning one XML root element as
     * a JDOM <code>Element</code> object.
     * This is typically the root element of a document.
     * <p>
     *
     * @param dso the  DSpace Object whose metadata to export.
     * @return root Element of the target metadata, never <code>null</code>
     *
     * @throws CrosswalkInternalException (<code>CrosswalkException</code>) failure of the crosswalk itself.
     * @throws CrosswalkObjectNotSupported (<code>CrosswalkException</code>) Cannot crosswalk this kind of DSpace object.
     * @throws IOException  I/O failure in services this calls
     * @throws SQLException  Database failure in services this calls
     * @throws AuthorizeException current user not authorized for this operation.
     */
    public Element disseminateElement(DSpaceObject dso)
        throws CrosswalkException, IOException, SQLException,
               AuthorizeException;
}
