/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * UFAL: jmisutka
 * 2011/04
 *
 * Extra generic metadata support v0.1
 */
package cz.cuni.mff.ufal.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.Context;

/**
 *
 * @author jm
 * modified for LINDAT/CLARIN
 */
public class UFALExtraMetadataStep extends org.dspace.submit.step.DescribeStep
{
    private static Logger log = Logger.getLogger(UFALExtraMetadataStep.class);

    /** Constructor */
    public UFALExtraMetadataStep() throws ServletException
    {
    }

    /**
     * @see DescribeStep.doProcessing
     */
    @Override
    public int doProcessing(Context context, HttpServletRequest request,
            HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // check what submit button was pressed in User Interface
        String buttonPressed = Util.getSubmitButton(request, NEXT_BUTTON);

        // get the item and current page
        Item item = subInfo.getSubmissionItem().getItem();
        int currentPage = getCurrentPage(request);

        // lookup applicable inputs
        Collection c = subInfo.getSubmissionItem().getCollection();
        DCInput[] inputs = null;
        try {
            inputs = inputsReader.getInputsExtra(c.getHandle())
                        .getPageRows( currentPage - 1, false, false );
        } catch (DCInputsReaderException e) {
            throw new ServletException(e);
        }
        
        // Fetch the document type (dc.type)
        String documentType = "";
        if( (item.getMetadataByMetadataString("dc.type") != null) && (item.getMetadataByMetadataString("dc.type").length >0) )
        {
            documentType = item.getMetadataByMetadataString("dc.type")[0].value;
        }
                
 

        // Step 1:
        // clear out all item metadata defined on this page
        for (int i = 0; i < inputs.length; i++)
        {
            if (!inputs[i]
                    .isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                            : DCInput.SUBMISSION_SCOPE))
            {
                continue;
            }
            String qualifier = inputs[i].getQualifier();
            if (qualifier == null
                    && inputs[i].getInputType().equals("qualdrop_value"))
            {
                qualifier = Item.ANY;
            }
            item.clearMetadata(inputs[i].getSchema(), inputs[i].getElement(),
                    qualifier, Item.ANY);
        }

        // Clear required-field errors first since missing authority
        // values can add them too.
        clearErrorFields(request);

        // Step 2:
        // now update the item metadata.
        String fieldName;
        boolean moreInput = false;
        for (int j = 0; j < inputs.length; j++)
        {
        	// Omit fields not allowed for this document type
            if(!inputs[j].isAllowedFor(documentType))
            {
            	continue;
            }

            if (!inputs[j]
                        .isVisible(subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE
                                : DCInput.SUBMISSION_SCOPE))
            {
                continue;
            }
            String element = inputs[j].getElement();
            String qualifier = inputs[j].getQualifier();
            String schema = inputs[j].getSchema();
            boolean repeatable = inputs[j].getRepeatable();

            if (qualifier != null && !qualifier.equals(Item.ANY))
            {
                fieldName = schema + "_" + element + '_' + qualifier;
            }
            else
            {
                fieldName = schema + "_" + element;
            }
            

            // UFAL/jmisutka hierarchical repeatable components
            // - we pressed the add new component
            // -- set the jump to field
            String repeatable_component = inputs[j].getExtraRepeatableComponent();
            if ( buttonPressed.equals("submit_component_" + repeatable_component) ) {
                if ( subInfo.getJumpToField() == null ){
                  subInfo.setJumpToField( fieldName );
                }
            }

            String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
            ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
            String inputType = inputs[j].getInputType();
            if (inputType.equals("name"))
            {
                readNames(request, item, schema, element, qualifier, repeatable);
            }
            else if (inputType.equals("date"))
            {
                readDate(request, item, schema, element, qualifier);
            }
            // choice-controlled input with "select" presentation type is
            // always rendered as a dropdown menu
            else if (inputType.equals("dropdown") || inputType.equals("list") ||
                     (cmgr.isChoicesConfigured(fieldKey) &&
                      "select".equals(cmgr.getPresentation(fieldKey))))
            {
                String[] vals = request.getParameterValues(fieldName);
                if (vals != null)
                {
                    for (int z = 0; z < vals.length; z++)
                    {
                        if (!vals[z].equals(""))
                        {
                            item.addMetadata(schema, element, qualifier, LANGUAGE_QUALIFIER,
                                    vals[z]);
                        }
                    }
                }
            }
            else if (inputType.equals("series"))
            {
                readSeriesNumbers(request, item, schema, element, qualifier, repeatable);
            }
            else if (inputType.equals("qualdrop_value"))
            {
                List<String> quals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_qualifier");
                List<String> vals = getRepeatedParameter(request, schema + "_"
                        + element, schema + "_" + element + "_value");
                for (int z = 0; z < vals.size(); z++)
                {
                    String thisQual = quals.get(z);
                    if ("".equals(thisQual))
                    {
                        thisQual = null;
                    }
                    String thisVal = vals.get(z);
                    if (!buttonPressed.equals("submit_" + schema + "_"
                            + element + "_remove_" + z)
                            && !thisVal.equals(""))
                    {
                        item.addMetadata(schema, element, thisQual, null,
                                thisVal);
                    }
                }
            }
            else if ((inputType.equals("onebox"))
                    || (inputType.equals("twobox"))
                    || (inputType.equals("textarea")))
            {
                readText( request, item, schema, element, qualifier, 
                          repeatable, LANGUAGE_QUALIFIER, repeatable_component, inputs[j].getRepeatableParse());

            }
            else
            {
                throw new ServletException("Field " + fieldName
                        + " has an unknown input type: " + inputType);
            }

            // determine if more input fields were requested
            if (!moreInput
                    && buttonPressed.equals("submit_" + fieldName + "_add"))
            {
                subInfo.setMoreBoxesFor(fieldName);
                subInfo.setJumpToField(fieldName);
                moreInput = true;
            }
            // was XMLUI's "remove" button pushed?
            else if (buttonPressed.equals("submit_" + fieldName + "_delete"))
            {
                subInfo.setJumpToField(fieldName);
            }
        }

        // Step 3:
        // Check to see if any fields are missing
        // Only check for required fields if user clicked the "next", the "previous" or the "progress bar" button
        if (buttonPressed.equals(NEXT_BUTTON)
                || buttonPressed.startsWith(PROGRESS_BAR_PREFIX)
                || buttonPressed.equals(PREVIOUS_BUTTON)
                || buttonPressed.equals(CANCEL_BUTTON))
        {
            for (int i = 0; i < inputs.length; i++)
            {
               	// Do not check the required attribute if it is not visible or not allowed for the document type
            	String scope = subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
                if ( !( inputs[i].isVisible(scope) && inputs[i].isAllowedFor(documentType) ) )
                {
                	continue;
                }
                
                Metadatum[] values = item.getMetadata(inputs[i].getSchema(),
                        inputs[i].getElement(), inputs[i].getQualifier(), Item.ANY);

                if (inputs[i].isRequired() && values.length == 0)
                {
                    // since this field is missing add to list of error fields
                    addErrorField(request, getFieldName(inputs[i]));
                }
                
                // Check whether the value matches given regexp 
                for(Metadatum dcval:values){
                    if(!inputs[i].isAllowedValue(dcval.value)){
                        addErrorField(request, getFieldName(inputs[i]));
                    }
                }                
            }
        }

        // Step 4:
        // Save changes to database
        subInfo.getSubmissionItem().update();

        // commit changes
        context.commit();

        // check for request for more input fields, first
        if (moreInput)
        {
            return STATUS_MORE_INPUT_REQUESTED;
        }
        // if one or more fields errored out, return
        else if (getErrorFields(request) != null && getErrorFields(request).size() > 0)
        {
            return STATUS_MISSING_REQUIRED_FIELDS;
        }

        // completed without errors
        return STATUS_COMPLETE;
    }


    /**
     * UFAL/jmisutka
     * Get number of pages from extra.
     */
    @Override
    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // by default, use the "default" collection handle
        String collectionHandle = DCInputsReader.DEFAULT_COLLECTION;

        if (subInfo.getSubmissionItem() != null)
        {
            collectionHandle = subInfo.getSubmissionItem().getCollection()
                    .getHandle();
        }

        // get number of input pages (i.e. "Describe" pages)
        try
        {
            return getInputsReader().getInputsExtra( collectionHandle ).getNumberPages();
        }
        catch (DCInputsReaderException e)
        {
            throw new ServletException(e);
        }
    }

}

