/*
 * Params.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/07/05 21:40:01 $
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
 * A class represented parameters to fields. The parameter element is basicaly a
 * grab bag of attributes associated with varios fields.
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

public class Params extends AbstractWingElement implements StructuralElement
{
    /** The name of the params element */
    public static final String E_PARAMS = "params";

    /** The name of the operations attribute */
    public static final String A_OPERATIONS = "operations";
    
    /** The name of the return value attribute */
    public static final String A_RETURN_VALUE = "returnValue";

    /** The name of the size attribute */
    public static final String A_SIZE = "size";

    /** The name of the max length attribute */
    public static final String A_MAX_LENGTH = "maxlength";

    /** The name of the multiple attribute */
    public static final String A_MULTIPLE = "multiple";

    /** The name of the rows attribute */
    public static final String A_ROWS = "rows";

    /** The name of the cols attribute */
    public static final String A_COLS = "cols";

    /** Possible operations */
    public static final String OPERATION_ADD = "add";

    public static final String OPERATION_DELETE = "delete";

    public static final String[] OPERATIONS = { OPERATION_ADD, OPERATION_DELETE };
    
    /** *********** Parameter Attributes *************** */

    /** The supported operations for this field */
    protected boolean addOperation;
    protected boolean deleteOperation;
    
    /** The return value for the field, checkboxes and radio buttons. */
    protected String returnValue;

    /** The field size */
    protected int size = -1;

    /** The maximum length of the field */
    protected int maxlength = -1;

    /** Weather multiple values for this field are allowed */
    protected boolean multiple = false;

    /** The number of rows the field should span */
    protected int rows = -1;

    /** The number of cols the field should span */
    protected int cols = -1;

    /**
     * Construct a new parameter's element
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
     */
    protected Params(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Enable the add operation for this field set. When this is enabled the
     * front end will add a button to add more items to the field.
     * 
     */
    public void enableAddOperation() throws WingException
    {
        this.addOperation = true;
    }

    /**
     * Enable the delete operation for this field set. When this is enabled then
     * the front end will provide a way for the user to select fields (probably
     * checkboxes) along with a submit button to delete the selected fields.
     * 
     */
    public void enableDeleteOperation()throws WingException
    {
        this.deleteOperation = true;
    }

    /**
     * Set the size of the field.
     * 
     * This applies to text, password, and select fields.
     * 
     * @param size
     *            (Required) The size of the field.
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * Set the maximum length of the field.
     * 
     * This applies to text, password, and textarea fields.
     * 
     * @param maxlength
     *            (Required) The maximum length of the field.
     */
    public void setMaxLength(int maxlength)
    {
        this.maxlength = maxlength;
    }

    /**
     * Set the number of rows of this field.
     * 
     * The applies only to textarea fields.
     * 
     * @param rows
     *            (Required) The number of rows.
     */
    public void setRows(int rows)
    {
        this.rows = rows;
    }

    /**
     * Set the number of columns of this field.
     * 
     * The applies only to textarea fields.
     * 
     * @param cols
     *            (Required) The number of columns.
     */
    public void setCols(int cols)
    {
        this.cols = cols;
    }

    /**
     * The returned value for this field if it is checked (or selected).
     * 
     * The applies to radio and checkbox fields.
     * 
     * @param returnValue
     *            (Required) The value to be returned if this field is checked.
     */
    public void setReturnValue(String returnValue)
    {
        this.returnValue = returnValue;
    }

    /**
     * Determine if this field can accept multiple values.
     * 
     * The applies only to select fields.
     * 
     * @param multiple
     *            (Required) whether the field can accept multiple values.
     */
    public void setMultiple(boolean multiple)
    {
        this.multiple = multiple;
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical events
     *            (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     */
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        AttributeMap attributes = new AttributeMap();

        // Determine if there are any operations
        String operations = null;
        if (addOperation )
        {
            if (operations == null)
                operations = OPERATION_ADD;
            else
                operations += " " + OPERATION_ADD;
        }
        if (addOperation)
        {
            if (operations == null)
                operations = OPERATION_DELETE;
            else
                operations += " " + OPERATION_DELETE;
        }
        if (operations != null)
            attributes.put(A_OPERATIONS, operations);

        
        
        if (this.returnValue != null)
        {
            attributes.put(A_RETURN_VALUE, this.returnValue);
        }

        if (this.size > -1)
        {
            attributes.put(A_SIZE, this.size);
        }

        if (this.maxlength > -1)
        {
            attributes.put(A_MAX_LENGTH, this.maxlength);
        }

        if (this.multiple == true)
        {
            attributes.put(A_MULTIPLE, this.multiple);
        }

        if (this.rows > -1)
        {
            attributes.put(A_ROWS, this.rows);
        }

        if (this.cols > -1)
        {
            attributes.put(A_COLS, this.cols);
        }

        startElement(contentHandler, namespaces, E_PARAMS, attributes);
        endElement(contentHandler, namespaces, E_PARAMS);
    }

    /**
     * Dispose
     */
    public void dispose()
    {
        super.dispose();
    }
}
