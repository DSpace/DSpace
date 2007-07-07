/*
 * Cell.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2006/03/13 17:19:39 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a table cell.
 * 
 * The cell element contained in a row of a table carries content for that
 * table. It is a character container, just like p, item, and hi, and its
 * primary purpose is to display textual data, possibly enhanced with
 * hyperlinks, emphasized blocks of text, images and form fields.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

public class Cell extends RichTextContainer implements StructuralElement
{
    /** The name of the cell element */
    public static final String E_CELL = "cell";

    /** The name of the role attribute */
    public static final String A_ROLE = "role";

    /** The name of the rows attribute */
    public static final String A_ROWS = "rows";

    /** The name of the cols attribute */
    public static final String A_COLS = "cols";

    /** The name of this cell */
    private String name;

    /** The role of this cell, see ROLES below */
    private String role;

    /** How many rows does this table span */
    private int rows;

    /** How many cols does this table span */
    private int cols;

    /** Special rendering instructions */
    private String rend;

    /** The possible cell role types */
    public static final String ROLE_DATA = "data";

    public static final String ROLE_HEADER = "header";

    /** All the possible cell role types collected into one array */
    public static final String[] ROLES = { ROLE_DATA, ROLE_HEADER };

    /**
     * Construct a new cell.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param role
     *            (May be null) determines what kind of information the cell
     *            carries, either header or data. See cell.ROLES
     * @param rows
     *            (May be zero for no defined value) determines how many rows
     *            does this cell span.
     * @param cols
     *            (May be zero for no defined value) determines how many columns
     *            does this cell span.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    protected Cell(WingContext context, String name, String role, int rows,
            int cols, String rend) throws WingException
    {
        super(context);
        restrict(role, ROLES,
                "The 'role' parameter must be one of these values: 'data' or 'header'.");
        greater(rows, -1,
                "The 'rows' parameter must be greater than or equal to zero.");
        greater(cols, -1,
                "The 'cols' parameter must be greater than or equal to zero.");

        this.name = name;
        this.role = role;
        this.rows = rows;
        this.cols = cols;
        this.rend = rend;
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical 
     *            events (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     */
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler,
            NamespaceSupport namespaces) throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        if (name != null)
        {
            attributes.put(A_NAME, name);
            attributes.put(A_ID, context.generateID(E_CELL, name));
        }

        if (role != null)
            attributes.put(A_ROLE, role);
        // else
        // attributes.put(A_ROLE, ROLE_DATA);
        if (rows > 0)
            attributes.put(A_ROWS, rows);
        if (cols > 0)
            attributes.put(A_COLS, cols);
        if (rend != null)
            attributes.put(A_RENDER, rend);

        startElement(contentHandler, namespaces, E_CELL, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_CELL);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        for (AbstractWingElement content : contents)
            content.dispose();
        if (contents != null)
            contents.clear();
        contents = null;
        super.dispose();
    }

}
