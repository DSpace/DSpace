/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class represented parameters to fields. The parameter element is basically a
 * grab bag of attributes associated with various fields.
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
import java.util.Arrays;

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

    /** The name of the behavior attribute */
    public static final String A_EVTBEHAVIOR = "evtbehavior";

    /** The name of the max length attribute */
    public static final String A_MAX_LENGTH = "maxlength";

    /** The name of the multiple attribute */
    public static final String A_MULTIPLE = "multiple";

    /** The name of the rows attribute */
    public static final String A_ROWS = "rows";

    /** The name of the cols attribute */
    public static final String A_COLS = "cols";

    /** True if this field is authority-controlled */
    public static final String A_AUTHORITY_CONTROLLED = "authorityControlled";

    /** True if an authority value is required */
    public static final String A_AUTHORITY_REQUIRED = "authorityRequired";
    
    /** The name of the HTML5 autofocus field */
    public static final String A_AUTOFOCUS = "autofocus";

    /** The name of the field to use for a list of choices */
    public static final String A_CHOICES = "choices";

    /** Type of presentation recommended for showing choices to user */
    /** See PRESENTATION_*   */
    public static final String A_CHOICES_PRESENTATION = "choicesPresentation";

    /** The name of the field to use for a list of choices */
    public static final String A_CHOICES_CLOSED = "choicesClosed";

    /** Possible operations */
    public static final String OPERATION_ADD = "add";

    public static final String OPERATION_DELETE = "delete";

    public static final String[] OPERATIONS = { OPERATION_ADD, OPERATION_DELETE };
    
    /** Possible UI presentation values */
    public static final String PRESENTATION_SELECT = "select";
    public static final String PRESENTATION_SUGGEST = "suggest";
    public static final String PRESENTATION_LOOKUP = "lookup";
    public static final String PRESENTATION_AUTHORLOOKUP = "authorLookup";
    public static final String PRESENTATION_NONE = "none";
    public static final String[] PRESENTATIONS = { PRESENTATION_SELECT, PRESENTATION_SUGGEST, PRESENTATION_LOOKUP, PRESENTATION_NONE, PRESENTATION_AUTHORLOOKUP };


    /** *********** Parameter Attributes *************** */

    /** The supported operations for this field */
    protected boolean addOperation;
    protected boolean deleteOperation;
    
    /** The return value for the field, checkboxes and radio buttons. */
    protected String returnValue;

    /** The field size */
    protected int size = -1;


    /** The event behavior attribute such as onchange, onfocus, on... */
    protected String evtBehavior = "";

    /** The maximum length of the field */
    protected int maxlength = -1;

    /** Whether multiple values for this field are allowed */
    protected boolean multiple = false;

    /** The number of rows the field should span */
    protected int rows = -1;

    /** The number of cols the field should span */
    protected int cols = -1;

    /** Value of the AuthorityControlled attribute */
    protected boolean authority = false;

    /** Value of the Authority_Required attribute */
    protected boolean authority_required = false;

    /** Value of the HTML5 autofocus field */
    protected String autofocus = null;

    /** Value of the Choices attribute */
    protected String choices = null;

    /** Value of the Choices Presentation attribute */
    protected String presentation = null;

    /** Value of choicesClosed option */
    protected boolean choicesClosed = false;

    /**
     * Construct a new parameter's element
     *
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * @throws org.dspace.app.xmlui.wing.WingException never.
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
     * @throws org.dspace.app.xmlui.wing.WingException never.
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
     * Set the event behavior (e.g. a javascript event)
     *  of the field.
     *
     * This applies to select fields as of this writing.
     *
     * @param behavior
     *            The action on onchange of the field.
     */
    public void setEvtBehavior(String behavior)
    {
        this.evtBehavior = behavior;
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
     * Set this field to be authority-controlled.
     *
     */
    public void setAuthorityControlled()
    {
        this.authority = true;
    }

    /**
     * Set this field to be authority-controlled.
     *
     * @param value true if authority-controlled.
     */
    public void setAuthorityControlled(boolean value)
    {
        this.authority = value;
    }

    /**
     * Set this field as authority_required.
     */
    public void setAuthorityRequired()
    {
        this.authority_required = true;
    }

    /**
     * Set this field to either be required or not required as determined by the
     * required parameter.
     *
     * @param value
     *          Determine if the authority control is required or not on this field.
     */
    public void setAuthorityRequired(boolean value)
    {
        this.authority_required = value;
    }

    /**
     * Set the field's autofocus attribute, an HTML5 feature.
     * Valid input values to enable autofocus are: autofocus, and empty string.
     * @param value "autofocus" or empty.
     */
    public void setAutofocus(String value)
    {
        this.autofocus = value;
    }

    /**
     *
     * @param fieldKey pre-determined metadata field key
     */
    public void setChoices(String fieldKey)
    {
        this.choices = fieldKey;
    }

    /**
     * Set the kind of UI presentation requested for this choice, e.g.
     * select vs. suggest.  Value must match one of the PRESENTATIONS.
     *
     * @param value pre-determined metadata field key
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setChoicesPresentation(String value)
        throws WingException
    {
        restrict(value, PRESENTATIONS,
                "The 'presentation' parameter must be one of these values: "+Arrays.deepToString(PRESENTATIONS));
        this.presentation = value;
    }

    /**
     * Sets whether choices are "closed" to the set returned by plugin.
     *
     * @param value pre-determined metadata field key
     */
    public void setChoicesClosed(boolean value)
    {
        this.choicesClosed = value;
    }

    /**
     * Sets whether choices are "closed" to the set returned by plugin.
     */
    public void setChoicesClosed()
    {
        this.choicesClosed = true;
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
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
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
            {
                operations = OPERATION_ADD;
            }
            else
            {
                operations += " " + OPERATION_ADD;
            }
        }
        if (addOperation)
        {
            if (operations == null)
            {
                operations = OPERATION_DELETE;
            }
            else
            {
                operations += " " + OPERATION_DELETE;
            }
        }
        if (operations != null)
        {
            attributes.put(A_OPERATIONS, operations);
        }

        if (this.returnValue != null)
        {
            attributes.put(A_RETURN_VALUE, this.returnValue);
        }

        if (this.size > -1)
        {
            attributes.put(A_SIZE, this.size);
        }

        if (!this.evtBehavior.equals(""))
        {
        	attributes.put(A_EVTBEHAVIOR, this.evtBehavior);
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

        if (this.authority)
        {
            attributes.put(A_AUTHORITY_CONTROLLED, this.authority);
        }
        if (this.authority_required)
        {
            attributes.put(A_AUTHORITY_REQUIRED, this.authority_required);
        }
        if (this.choices != null)
        {
            attributes.put(A_CHOICES, this.choices);
        }
        if (this.presentation != null)
        {
            attributes.put(A_CHOICES_PRESENTATION, this.presentation);
        }
        if (this.choicesClosed)
        {
            attributes.put(A_CHOICES_CLOSED, true);
        }

        if (this.autofocus != null)
        {
            attributes.put(A_AUTOFOCUS, this.autofocus);
        }

        startElement(contentHandler, namespaces, E_PARAMS, attributes);
        endElement(contentHandler, namespaces, E_PARAMS);
    }
}
