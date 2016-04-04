package org.dspace.app.xmlui.aspect.authority;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.AuthorityObject;
import org.dspace.content.authority.Choices;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Event;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * User: lantian @ atmire . com
 * Date: 5/12/14
 * Time: 3:25 PM
 */
public class FlowAuthorityMetadataValueUtils {

    /** log4j category */
    private static final Logger log = Logger.getLogger(FlowAuthorityMetadataValueUtils.class);


    public static FlowResult processEditMetadata(Context context, int type, String authorityId,Request request){
        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure
        try{
            AuthorityObject authorityObject = AuthorityObject.find(context,type,Integer.parseInt(authorityId));
            String editFieldIds = request.getParameter("field");
            // STEP 1:
            // Clear all metadata within the scope
            // Only metadata values within this scope will be considered. This
            // is so ajax request can operate on only a subset of the values.
            String scope = request.getParameter("scope");
            if ("*".equals(scope))
            {
                authorityObject.clearMetadata(context, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            }
            else
            {
                String[] parts = parseName(scope);
                authorityObject.clearMetadata(context, parts[0],parts[1],parts[2],Item.ANY);
            }

            // STEP 2:
            // First determine all the metadata fields that are within
            // the scope parameter
            ArrayList<Integer> indexes = new ArrayList<Integer>();
            Enumeration parameters = request.getParameterNames();
            while(parameters.hasMoreElements())
            {

                // Only consider the name_ fields
                String parameterName = (String) parameters.nextElement();
                if (parameterName.startsWith("name_"))
                {
                    // Check if the name is within the scope
                    String parameterValue = request.getParameter(parameterName);
                    if ("*".equals(scope) || scope.equals(parameterValue))
                    {
                        // Extract the index from the name.
                        String indexString = parameterName.substring("name_".length());
                        Integer index = Integer.valueOf(indexString);
                        indexes.add(index);
                    }
                }
            }

            // STEP 3:
            // Iterate over all the indexes within the scope and add them back in.
            for (Integer index=1; index <= indexes.size(); ++index)
            {
                String name = request.getParameter("name_"+index);
                String value = request.getParameter("value_"+index);
                String authority = request.getParameter("value_"+index+"_authority");
                String confidence = request.getParameter("value_"+index+"_confidence");
                String lang = request.getParameter("language_"+index);
                String remove = request.getParameter("remove_"+index);

                // the user selected the remove checkbox.
                if (remove != null)
                {
                    continue;
                }

                // get the field's name broken up
                String[] parts = parseName(name);

                // probe for a confidence value
                int iconf = Choices.CF_UNSET;
                if (confidence != null && confidence.length() > 0)
                {
                    iconf = Choices.getConfidenceValue(confidence);
                }
                // upgrade to a minimum of NOVALUE if there IS an authority key
                if (authority != null && authority.length() > 0 && iconf == Choices.CF_UNSET)
                {
                    iconf = Choices.CF_NOVALUE;
                }
                authorityObject.addMetadata(context, parts[0], parts[1], parts[2], lang,
                        value, authority, iconf);
            }

            context.commit();
        }catch (Exception e)
        {
            log.error(e.getMessage());
        }

        return result;
    }

    public static FlowResult doDeleteMetadata(Context context, int type,String id,Request request)throws SQLException, AuthorizeException, UIException, IOException {
        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure
        String fieldIDs = request.getParameter("field");
        AuthorityObject authorityObject = AuthorityObject.find(context,type,Integer.parseInt(id));

        context.commit();
        return result;
    }

    public static FlowResult doAddMetadata(Context context,int type, int id, Request request) throws SQLException, AuthorizeException, UIException, IOException
    {
        FlowResult result = new FlowResult();
        String tableName = "";
        result.setContinue(false);
        switch (type)
        {
            case Constants.SCHEME: tableName = "SchemeMetadataValue";break;
            case Constants.TERM: tableName = "TermMetadataValue"; break;
            case Constants.CONCEPT: tableName = "ConceptMetadataValue"; break;
        }
        String fieldID = request.getParameter("field");
        String value = request.getParameter("value");
        String language = request.getParameter("language");

        //add email to concept metadata
        AuthorityMetadataValue authorityMetadataValue = new AuthorityMetadataValue(tableName);

        authorityMetadataValue.setValue(value);
        authorityMetadataValue.setFieldId(Integer.parseInt(fieldID));
        authorityMetadataValue.setParentId(id);
        authorityMetadataValue.create(context);
        context.addEvent(new Event(Event.MODIFY_METADATA, type, id, null));
        context.commit();

        result.setContinue(true);

        result.setOutcome(true);
        result.setMessage(T_metadata_added);

        return result;
    }
    private static final Message T_metadata_added = new Message("default","New metadata was added.");


    /**
     * Parse the given name into three parts, divided by an _. Each part should represent the
     * schema, element, and qualifier. You are guaranteed that if no qualifier was supplied the
     * third entry is null.
     *
     * @param name The name to be parsed.
     * @return An array of name parts.
     */
    private static String[] parseName(String name) throws UIException
    {
        String[] parts = new String[3];

        String[] split = name.split("_");
        if (split.length == 2) {
            parts[0] = split[0];
            parts[1] = split[1];
            parts[2] = null;
        } else if (split.length == 3) {
            parts[0] = split[0];
            parts[1] = split[1];
            parts[2] = split[2];
        } else {
            throw new UIException("Unable to parse metedata field name: "+name);
        }
        return parts;
    }

}
