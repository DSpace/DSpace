/*
 * Row.java
 *
 * Version: $Revision: 1.7 $
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
 * A class that represents a table row.
 * 
 * The row element is contained inside a table and serves as a container of cell
 * elements. A required 'role' attribute determines how the row and its cells
 * are used.
 * 
 * @author Scott Phillips
 */

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

public class Row extends AbstractWingElement implements StructuralElement
{
    /** The name of the row element */
    public static final String E_ROW = "row";

    /** The name of the role attribute */
    public static final String A_ROLE = "role";

    /** The row's name */
    private String name;

    /** The row's role, see ROLES below */
    private String role;

    /** Special rendering instructions */
    private String rend;

    /** The row (and cell) role types: */
    public static final String ROLE_DATA = "data";

    public static final String ROLE_HEADER = "header";

    /** All the roles collected into one array */
    public static final String[] ROLES = { ROLE_DATA, ROLE_HEADER };

    /** The contents of this row */
    List<AbstractWingElement> contents = new ArrayList<AbstractWingElement>();

    /**
     * Construct a new table row.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param role
     *            (May be null) determines what kind of information the row
     *            carries, either header or data. See row.ROLES
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    protected Row(WingContext context, String name, String role, String rend)
            throws WingException
    {
        super(context);
        restrict(role, ROLES,
                "The 'role' parameter must be one of these values: 'data' or 'header'.");

        this.name = name;
        this.role = role;
        this.rend = rend;
    }

    /**
     * Add a new cell to the table. The cell element contained in a row of a
     * table carries content for that table. It is a character container, just
     * like p, item, and hi, and its primary purpose is to display textual data,
     * possibly enhanced with hyperlinks, emphasized blocks of text, images and
     * form fields.
     * 
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
     * @return a new table cell.
     */
    public Cell addCell(String name, String role, int rows, int cols,
            String rend) throws WingException
    {
        Cell cell = new Cell(context, name, role, rows, cols, rend);
        contents.add(cell);
        return cell;
    }

    /**
     * Add a new cell to the table. The cell element contained in a row of a
     * table carries content for that table. It is a character container, just
     * like p, item, and hi, and its primary purpose is to display textual data,
     * possibly enhanced with hyperlinks, emphasized blocks of text, images and
     * form fields.
     * 
     * @param rows
     *            (May be zero for no defined value) determines how many rows
     *            does this cell span.
     * @param cols
     *            (May be zero for no defined value) determines how many columns
     *            does this cell span.
     * @return a new table cell.
     */
    public Cell addCell(int rows, int cols) throws WingException
    {
    	return addCell(null, null, rows, cols, null);
    }

    
    
    /**
     * Add a new cell to the table. The cell element contained in a row of a
     * table carries content for that table. It is a character container, just
     * like p, item, and hi, and its primary purpose is to display textual data,
     * possibly enhanced with hyperlinks, emphasized blocks of text, images and
     * form fields.
     * 
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param role
     *            (May be null) determines what kind of information the cell
     *            carries, either header or data. See cell.ROLES
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new table cell.
     */
    public Cell addCell(String name, String role, String rend)
            throws WingException
    {
        return addCell(name, role, 0, 0, rend);
    }

    /**
     * Add a new cell to the table. The cell element contained in a row of a
     * table carries content for that table. It is a character container, just
     * like p, item, and hi, and its primary purpose is to display textual data,
     * possibly enhanced with hyperlinks, emphasized blocks of text, images and
     * form fields.
     * 
     * @param role
     *            (May be null) determines what kind of information the cell
     *            carries, either header or data. See cell.ROLES
     * @return a new table cell.
     */
    public Cell addCell(String role) throws WingException
    {
        return addCell(null, role, 0, 0, null);
    }

    /**
     * Add a new cell to the table. The cell element contained in a row of a
     * table carries content for that table. It is a character container, just
     * like p, item, and hi, and its primary purpose is to display textual data,
     * possibly enhanced with hyperlinks, emphasized blocks of text, images and
     * form fields.
     * 
     * @return a new table cell.
     */
    public Cell addCell() throws WingException
    {
        return addCell(null, null, 0, 0, null);
    }

    /**
     * Add a new cell to the table. The cell element contained in a row of a
     * table carries content for that table. It is a character container, just
     * like p, item, and hi, and its primary purpose is to display textual data,
     * possibly enhanced with hyperlinks, emphasized blocks of text, images and
     * form fields.
     * 
     * Once the cell has been created set the cell's contents to the provided
     * content.
     * 
     * @param characters
     *            (Required) Untranslated character data to be included.
     */
    public void addCellContent(String characters) throws WingException
    {
        Cell cell = this.addCell();
        cell.addContent(characters);
    }

    /**
     * Add a new cell to the table. The cell element contained in a row of a
     * table carries content for that table. It is a character container, just
     * like p, item, and hi, and its primary purpose is to display textual data,
     * possibly enhanced with hyperlinks, emphasized blocks of text, images and
     * form fields.
     * 
     * Once the cell has been created set the cell's contents to the provided
     * content.
     * 
     * @param message
     *            (Required) Key to the i18n catalogue to translate the content
     *            into the language preferred by the user.
     */
    public void addCellContent(Message message) throws WingException
    {
        Cell cell = this.addCell();
        cell.addContent(message);
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
            attributes.put(A_ID, context.generateID(E_ROW, name));
        }

        if (role != null)
            attributes.put(A_ROLE, role);

        if (rend != null)
            attributes.put(A_RENDER, rend);

        startElement(contentHandler, namespaces, E_ROW, attributes);
        for (AbstractWingElement content : contents)
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_ROW);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        for (AbstractWingElement content : contents)
            content.dispose();
        contents.clear();
        contents = null;
        super.dispose();
    }

}
