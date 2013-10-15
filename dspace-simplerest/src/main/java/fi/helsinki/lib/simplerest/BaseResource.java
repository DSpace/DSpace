/**
 A RESTful web service on top of DSpace.
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import org.restlet.resource.ServerResource;
import org.restlet.representation.StringRepresentation;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.DOMException;

import java.lang.reflect.Method;

abstract class BaseResource extends ServerResource {

    protected Context getAuthenticatedContext() throws SQLException {
        int adminId = -1; // just some (invalid) value to keep a compiler happy
        try {
            String s = getContext().getParameters().getFirstValue("adminID");
            adminId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            // Let's trust that the configutation contains an integer...
        }

        Context context = new Context();
        EPerson ePerson = EPerson.find(context, adminId);
        context.setCurrentUser(ePerson);
        return context;
    }

    protected void makeInputRow(Document d, Element form,
                                String fieldName,
                                String printableName) {
        makeInputRow(d, form, fieldName, printableName, "text");
    }

    protected void makeInputRow(Document d, Element form,
                                String fieldName,
                                String printableName,
                                String type) {
        Element label = d.createElement("label");
        label.setAttribute("for", fieldName);
        label.appendChild(d.createTextNode(printableName));
        form.appendChild(label);
        Element input = d.createElement("input");
        input.setAttribute("type", type);
        input.setAttribute("name", fieldName);
        input.setAttribute("id", fieldName);
        form.appendChild(input);
        Element br = d.createElement("br");
        form.appendChild(br);
        return;
    }

    protected void setAttribute(Element el, String attribute, String value) {
        try {
            el.setAttribute(attribute, value);
        }
        catch (DOMException e) {
            // We trust that this never happens...
        }
    }

    protected void setClass(Element el, String className) {
        setAttribute(el, "class", className);
    }

    protected void setId(Element el, String id) {
        setAttribute(el, "id", id);
    }

    protected void addDtDd(Document d, Element dl, String key, String value) {
        Element dt = d.createElement("dt");
        dt.appendChild(d.createTextNode(key));
        dl.appendChild(dt);
        
        Element dd = d.createElement("dd");
        dd.appendChild(d.createTextNode(value != null ? value : ""));
        dl.appendChild(dd);
    }

    protected StringRepresentation successOk(String message) {
        setStatus(Status.SUCCESS_OK);
        return new StringRepresentation(message, MediaType.TEXT_PLAIN);
    }

    protected StringRepresentation successCreated(String message,
                                                   String locationUri) {
        setStatus(Status.SUCCESS_CREATED);
        setLocationRef(locationUri);
        return new StringRepresentation(message, MediaType.TEXT_PLAIN);
    }

    protected StringRepresentation errorInternal(Context c, String message) {
        return error(c, message, Status.SERVER_ERROR_INTERNAL);
    }

    protected StringRepresentation errorNotFound(Context c, String message) {
        return error(c, message, Status.CLIENT_ERROR_NOT_FOUND);
    }

    protected StringRepresentation error(Context c, String message,
                                         Status status) {
        if (c != null) {
            c.abort();
        }
        setStatus(status);
        return new StringRepresentation(message, MediaType.TEXT_PLAIN);
    }

    // Ok, maybe this a overkill considering that all we really want to do
    // return is a constant string... which we could have put into a
    // configuration file.
    protected String baseUrl() {
        String relUrl = null;
        try {
            Method method = this.getClass().getDeclaredMethod("relativeUrl",
                                                              Integer.TYPE);
            relUrl = (String) method.invoke(this, 1337);
        }
        catch (Exception e) {
            // As long as we call baseUrl() from a class that has a correctly
            // defined relatiUrl(int) method we should not get any exceptions..
        }

        String url = getRequest().getResourceRef().getIdentifier();
        int n = relUrl.split("/").length;
        while ( n-- > 0) {
            url = url.substring(0, url.lastIndexOf('/'));
        }
        return url + "/";
    }

}
