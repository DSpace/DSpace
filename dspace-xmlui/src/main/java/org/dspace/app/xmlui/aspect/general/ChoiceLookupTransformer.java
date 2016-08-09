/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.general;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.content.DCPersonName;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * Create the "lookup" popup window for Choice Control.  It loads a selector
 * via AJAX request, and transfers values (both text and authority/confidence)
 * back to the indicated form fields in the window that launched it.
 * Some necessary logic is in JavaScript, see choice-control.js.
 *
 * <p>Expected Parameters:
 * <dl>
 *  <dt>field <dd>name of metadata field in "_" notation, eg: dc_contributor_author
 *  <dt>value <dd>maybe-partial value of field
 *  <dt>formID <dd>the @id of {@code <form>} tag in calling window containing the inputs we are to set.
 *  <dt>valueInput <dd>@name of input field in DOM for value.
 *  <dt>authorityInput <dd>@name of input field in DOM for authority value
 *  <dt>isRepeating <dd>true if metadata value can be repeated
 *  <dt>isName <dd>true if this is a name value (i.e. last/first boxes)
 *  <dt>start <dd>starting index, default 0
 *  <dt>limit <dd>maximum values to return, default 0 (none)
 * </dl>
 *
 * <p>Configuration Properties:
 * <ul>
 *  <li>xmlui.lookup.select.size = 12  (default, entries to show in SELECT widget.)
 * </ul>
 *
 * <p>For each FIELD, e.g. dc.contributor.author, these message properties
 * will OVERRIDE the corresponding i18n message catalog entries:
 * <dl>
 *  <dt>xmlui.lookup.field.FIELD.title
 *  <dd>title of lookup page
 *   (e.g. xmlui.lookup.field.dc_contributor_author.title = Author..)
 *
 *  <dt>xmlui.lookup.field.FIELD.nonauthority
 *  <dd>template for "non-authority" label in options
 *
 *  <dt>xmlui.lookup.field.FIELD.help
 *  <dd>help message for single input
 *    (NOTE this is still required even for name inputs)
 *
 *  <dt>xmlui.lookup.field.FIELD.help.last
 *  <dd>help message for last name of Name-oriented input
 *
 *  <dt>xmlui.lookup.field.FIELD.help.first
 *  <dd>help message for first name of Name-oriented input
 * </dl>
 *
 * @author  Larry Stone
 */
public class ChoiceLookupTransformer extends AbstractDSpaceTransformer
{
    private static final String CONFIG_PREFIX = "xmlui.lookup.field.";

    /** Language Strings */
    private static final String MESSAGE_PREFIX = "xmlui.ChoiceLookupTransformer.";
    private static final Message T_title = message(MESSAGE_PREFIX+"title");
    private static final Message T_add =    message(MESSAGE_PREFIX+"add");
    private static final Message T_accept = message(MESSAGE_PREFIX+"accept");
    private static final Message T_more =   message(MESSAGE_PREFIX+"more");
    private static final Message T_cancel = message(MESSAGE_PREFIX+"cancel");
    private static final Message T_results = message(MESSAGE_PREFIX+"results");
    private static final Message T_fail =    message(MESSAGE_PREFIX+"fail");

    protected ChoiceAuthorityService choicheAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        String field = null;
        String value = null;
        String formID = null;
        String confIndicatorID = null;
        boolean isName = false;
        boolean isRepeating = false;
        String valueInput = null;
        String authorityInput = null;
        int start = 0;
        int limit = 0;
        String collection = null;

        // HTTP parameters:
        try
        {
            field = parameters.getParameter("field");
            value = parameters.getParameter("value");
            formID = parameters.getParameter("formID");
            confIndicatorID = parameters.getParameter("confIndicatorID");
            isName = parameters.getParameterAsBoolean("isName", false);
            isRepeating = parameters.getParameterAsBoolean("isRepeating", false);
            valueInput = parameters.getParameter("valueInput");
            authorityInput = parameters.getParameter("authorityInput");
            String sStart = parameters.getParameter("start");
            if (sStart != null)
            {
                start = atoi(sStart);
            }
            String sLimit = parameters.getParameter("limit");
            if (sLimit != null)
            {
                limit = atoi(sLimit);
            }
            collection = parameters.getParameter("collection");
            if (collection == null)
            {
                collection = "-1";
            }
        }
        catch (org.apache.avalon.framework.parameters.ParameterException e)
        {
            throw new UIException("Missing a required parameter",e);
        }

        Division idiv = body.addInteractiveDivision("lookup", "", "get", "popup");
        if (isFieldMessage(field, "title"))
        {
            idiv.setHead(getFieldMessage(field, "title"));
        }
        else
        {
            idiv.setHead(getFieldLabel(field, "title"));
        }
        List fl = idiv.addList("choicesList", "form", "choices-lookup");
        fl.setHead(T_results);

        // the <select> tag, and param values
        Item selectItem = fl.addItem("select", "choices-lookup");
        Select s = selectItem.addSelect("chooser", "choices-lookup");
        s.setSize(DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("xmlui.lookup.select.size", 12));

        // parameters for javascript
        Hidden h = selectItem.addHidden("paramField");
        h.setValue(field);
        h = selectItem.addHidden("paramValue");
        h.setValue(value);
        h = selectItem.addHidden("paramIsName");
        h.setValue(String.valueOf(isName));
        h = selectItem.addHidden("paramIsRepeating");
        h.setValue(String.valueOf(isRepeating));
        h = selectItem.addHidden("paramValueInput");
        h.setValue(valueInput);
        h = selectItem.addHidden("paramAuthorityInput");
        h.setValue(authorityInput);
        h = selectItem.addHidden("paramStart");
        h.setValue(String.valueOf(start));
        h = selectItem.addHidden("paramLimit");
        h.setValue(String.valueOf(limit));
        h = selectItem.addHidden("paramFormID");
        h.setValue(formID);
        h = selectItem.addHidden("paramConfIndicatorID");
        h.setValue(confIndicatorID);
        h = selectItem.addHidden("paramFail");
        h.setValue(T_fail);
        boolean isClosed = choicheAuthorityService.isClosed(field);
        h = selectItem.addHidden("paramIsClosed");
        h.setValue(String.valueOf(isClosed));
        h = selectItem.addHidden("paramCollection");
        h.setValue(String.valueOf(collection));
        if (!isClosed)
        {
            h = selectItem.addHidden("paramNonAuthority");
            if (isFieldMessage(field, "nonauthority"))
            {
                h.setValue(getFieldMessage(field, "nonauthority"));
            }
            else
            {
                h.setValue(getFieldLabel(field, "nonauthority"));
            }
        }
        h = selectItem.addHidden("contextPath");
        h.setValue(contextPath);

        // NOTE: the "spinner" indicator image gets added in the XSLT.

        // the text input(s)
        Item ti = fl.addItem("textFields", "choices-lookup");
        Composite textItem = ti.addComposite("textFieldsComp", "choices-lookup");
        Text t1 = textItem.addText("text1", "choices-lookup");
        if (isName)
        {
            Text t2 = textItem.addText("text2", "choices-lookup");
            DCPersonName dp = new DCPersonName(value);
            t1.setValue(dp.getLastName());
            t2.setValue(dp.getFirstNames());
            if (isFieldMessage(field, "help.last"))
            {
                Message m = getFieldMessage(field, "help.last");
                t1.setLabel(m);
                t1.setHelp(m);
            }
            else
            {
                String m = getFieldLabel(field, "help.last");
                t1.setLabel(m);
                t1.setHelp(m);
            }
            if (isFieldMessage(field, "help.first"))
            {
                Message m = getFieldMessage(field, "help.first");
                t2.setLabel(m);
                t2.setHelp(m);
            }
            else
            {
                String m = getFieldLabel(field, "help.first");
                t2.setLabel(m);
                t2.setHelp(m);
            }
        }
        else
        {
            t1.setValue(value);
            if (isFieldMessage(field, "help"))
            {
                Message m = getFieldMessage(field, "help");
                t1.setLabel(m);
                t1.setHelp(m);
            }
            else
            {
                String m = getFieldLabel(field, "help");
                t1.setLabel(m);
                t1.setHelp(m);
            }
        }

        // confirmation buttons
        Item buttItem = fl.addItem("confirmation", "choices-lookup");
        Button accept = buttItem.addButton("accept", "choices-lookup");
        accept.setValue(isRepeating ? T_add : T_accept);
        Button more = buttItem.addButton("more", "choices-lookup");
        more.setValue(T_more);
        Button cancel = buttItem.addButton("cancel", "choices-lookup");
        cancel.setValue(T_cancel);
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        // This invokes magic popup transformation in XSL - "framing.popup"
        pageMeta.addMetadata("framing","popup").addContent("true");
    }

    /**
     *  Protocol to get custom and/or i18n strings:
     *   For label NAME,
     *    .. if config key xmlui.choices.FIELD.NAME is defined, and starts
     *       with "xmlui.", then it's a message key.
     *    .. if NO config key is defined, look for message
     *      xmlui.ChoiceLookupTransformer.field.FIELD.NAME
     *    .. otherwise take literal value from configuration
     */

    // return true if configured (or lack thereof) value points to Message
    private boolean isFieldMessage(String field, String name)
    {
        String cv = getFieldLabel(field, name);
        return (cv == null || cv.startsWith("xmlui."));
    }

    // get field-specific label value
    private String getFieldLabel(String field, String name)
    {
        return DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(CONFIG_PREFIX+field+"."+name);
    }

    // get field-specific label value
    private Message getFieldMessage(String field, String name)
    {
        String cv = getFieldLabel(field, name);
        if (cv == null)
        {
            return message(MESSAGE_PREFIX + "field." + field + "." + name);
        }
        else
        {
            return message(cv);
        }
    }

    private int atoi(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch (Exception e) {}
        return 0;
   }
}
