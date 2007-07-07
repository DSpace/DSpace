/*
 * Table.java
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
 * A class representing a table.
 * 
 * The table element is a container for information presented in tabular format.
 * It consists of a set of row elements and an optional header.
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

public class Table extends AbstractWingElement implements StructuralElement
{
    /** The name of the table element */
    public static final String E_TABLE = "table";

    /** The name of the rows attribute */
    public static final String A_ROWS = "rows";

    /** The name of the cols attribute */
    public static final String A_COLS = "cols";

    /** The name assigned to this table */
    private String name;

    /** Special rendering instructions for this table */
    private String rend;

    /** The number of rows in the table */
    private int rows;

    /** The number of cols in the table */
    private int cols;

    /** The table's head */
    private Head head;

    /** the rows contained in the table */
    private List<AbstractWingElement> contents = new ArrayList<AbstractWingElement>();

    /**
     * Construct a new row.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * 
     * @param rows
     *            (Required) the number of rows in the table.
     * @param cols
     *            (Required) the number of columns in the table.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     */
    protected Table(WingContext context, String name, int rows, int cols,
            String rend) throws WingException
    {
        super(context);
        require(name, "The 'name' parameter is required for all tables.");
        greater(rows, 0, "The 'rows' parameter must be grater than zero.");
        greater(cols, 0, "The 'cols' parameter must be grater than zero.");

        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.rend = rend;
    }

    /**
     * Set the head element which is the label associated with this table.
     * 
     * @param characters
     *            (May be null) Unprocessed characters to be included
     */
    public Head setHead() throws WingException
    {
        this.head = new Head(context, null);
        return head;

    }

    /**
     * Set the head element which is the label associated with this table.
     * 
     * @param characters
     *            (May be null) Unprocessed characters to be included
     */
    public void setHead(String characters) throws WingException
    {
        Head head = this.setHead();
        head.addContent(characters);

    }

    /**
     * Set the head element which is the label associated with this table.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     */
    public void setHead(Message message) throws WingException
    {
        Head head = this.setHead();
        head.addContent(message);
    }

    /**
     * Add a new row to the table. The row element is contained inside a table
     * and serves as a container of cell elements. A required 'role' attribute
     * determines how the row and its cells are used.
     * 
     * 
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param role
     *            (May be null) determine what kind of information the row
     *            carries, either header or data. See row.ROLES
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * 
     * @return a new table row
     */
    public Row addRow(String name, String role, String rend)
            throws WingException
    {
        Row row = new Row(context, name, role, rend);
        contents.add(row);
        return row;
    }

    /**
     * Add a new row to the table. The row element is contained inside a table
     * and serves as a container of cell elements. A required 'role' attribute
     * determines how the row and its cells are used.
     * 
     * @param role
     *            (May be null) determines what kind of information the row
     *            carries, either header or data. See row.ROLES
     * 
     * @return a new table row
     */
    public Row addRow(String role) throws WingException
    {
        return this.addRow(null, role, null);
    }

    /**
     * Add a new row to the table. The row element is contained inside a table
     * and serves as a container of cell elements.
     * 
     * @return a new table row
     */
    public Row addRow() throws WingException
    {
        return this.addRow(null, null, null);
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
        attributes.put(A_NAME, name);
        attributes.put(A_ID, context.generateID(E_TABLE, name));
        attributes.put(A_ROWS, rows);
        attributes.put(A_COLS, cols);
        if (rend != null)
            attributes.put(A_RENDER, rend);

        startElement(contentHandler, namespaces, E_TABLE, attributes);
        if (head != null)
            head.toSAX(contentHandler, lexicalHandler, namespaces);
        for (AbstractWingElement content : contents)
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_TABLE);
    }

    /**
     * dispose
     */
    public void dispose()
    {
        if (head != null)
            head.dispose();
        for (AbstractWingElement content : contents)
            content.dispose();

        head = null;
        contents.clear();
        contents = null;
        super.dispose();
    }

}
