/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.registries;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.RequestUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;

/**
 * Enable the user to edit a bitstream format's metadata. 
 * 
 * @author Scott Phillips
 */
public class EditBitstreamFormat extends AbstractDSpaceTransformer   
{	
	
	/** Language Strings */
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	private static final Message T_title =
		message("xmlui.administrative.registries.EditBitstreamFormat.title");
	private static final Message T_format_registry_trail =
		message("xmlui.administrative.registries.general.format_registry_trail");
	private static final Message T_trail =
		message("xmlui.administrative.registries.EditBitstreamFormat.trail");
	private static final Message T_head1 =
		message("xmlui.administrative.registries.EditBitstreamFormat.head1");
	private static final Message T_head2 =
		message("xmlui.administrative.registries.EditBitstreamFormat.head2");
	private static final Message T_para1 =
		message("xmlui.administrative.registries.EditBitstreamFormat.para1");
	private static final Message T_name =
		message("xmlui.administrative.registries.EditBitstreamFormat.name");
	private static final Message T_name_help =
		message("xmlui.administrative.registries.EditBitstreamFormat.name_help");
	private static final Message T_name_error =
		message("xmlui.administrative.registries.EditBitstreamFormat.name_error");
	private static final Message T_mimetype =
		message("xmlui.administrative.registries.EditBitstreamFormat.mimetype");
	private static final Message T_mimetype_help =
		message("xmlui.administrative.registries.EditBitstreamFormat.mimetype_help");
	private static final Message T_description =
		message("xmlui.administrative.registries.EditBitstreamFormat.description");
	private static final Message T_support =
		message("xmlui.administrative.registries.EditBitstreamFormat.support");
	private static final Message T_support_help =
		message("xmlui.administrative.registries.EditBitstreamFormat.support_help");
	private static final Message T_support_0 =
		message("xmlui.administrative.registries.EditBitstreamFormat.support_0");
	private static final Message T_support_1 =
		message("xmlui.administrative.registries.EditBitstreamFormat.support_1");
	private static final Message T_support_2 =
		message("xmlui.administrative.registries.EditBitstreamFormat.support_2");
	private static final Message T_internal =
		message("xmlui.administrative.registries.EditBitstreamFormat.internal");
	private static final Message T_internal_help =
		message("xmlui.administrative.registries.EditBitstreamFormat.internal_help");
	private static final Message T_extensions =
		message("xmlui.administrative.registries.EditBitstreamFormat.extensions");
	private static final Message T_extensions_help =
		message("xmlui.administrative.registries.EditBitstreamFormat.extensions_help");
	private static final Message T_submit_save =
		message("xmlui.general.save");
	private static final Message T_submit_cancel =
		message("xmlui.general.cancel");

	protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();

	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/format-registry",T_format_registry_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException 
	{
		// Get our parameters & state
		int formatID = parameters.getParameterAsInteger("formatID",-1);
		BitstreamFormat format = null;
		
		if (formatID >= 0)
        {
            format = bitstreamFormatService.find(context, formatID);
        }
	
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }
		
		Request request = ObjectModelHelper.getRequest(objectModel);
        String mimetypeValue = request.getParameter("mimetype");
        String nameValue = request.getParameter("short_description");
        String descriptionValue = request.getParameter("description");
        String supportLevelValue = request.getParameter("support_level");
        String internalValue = request.getParameter("internal");
        java.util.List<String> extensionValues = RequestUtils.getFieldValues(request, "extensions");

        // Remove leading periods from file extensions.
        for (int i = 0; i < extensionValues.size(); i++)
        {
        	if (extensionValues.get(i).startsWith("."))
            {
                extensionValues.set(i, extensionValues.get(i).substring(1));
            }
        }
        
        if (format != null)
        {
        	if (mimetypeValue == null)
            {
                mimetypeValue = format.getMIMEType();
            }
        	if (nameValue == null)
            {
                nameValue = format.getShortDescription();
            }
        	if (descriptionValue == null)
            {
                descriptionValue = format.getDescription();
            }
        	if (supportLevelValue == null)
            {
                supportLevelValue = String.valueOf(format.getSupportLevel());
            }
        	if (request.getParameter("mimetype") == null)
            {
                internalValue = format.isInternal() ? "true" : null;
            }
        	if (request.getParameter("extensions") == null)
            {
                extensionValues = format.getExtensions();
            }
        }
		
        
	
        
        // DIVISION: edit-bitstream-format
		Division main = body.addInteractiveDivision("edit-bitstream-format",contextPath+"/admin/format-registry",Division.METHOD_POST,"primary administrative format-registry");
		if (formatID == -1)
        {
            main.setHead(T_head1);
        }
		else
        {
            main.setHead(T_head2.parameterize(nameValue));
        }
		main.addPara(T_para1);
	
		List form = main.addList("edit-bitstream-format",List.TYPE_FORM);
		
		Text name = form.addItem().addText("short_description");
		name.setRequired();
		name.setLabel(T_name);
		name.setHelp(T_name_help);
		name.setValue(nameValue);
		name.setSize(35);
		if (errors.contains("short_description"))
        {
            name.addError(T_name_error);
        }
		
		Text mimeType = form.addItem().addText("mimetype");
		mimeType.setLabel(T_mimetype);
		mimeType.setHelp(T_mimetype_help);
		mimeType.setValue(mimetypeValue);
		mimeType.setSize(35);
		
		// Do not allow anyone to change the name of the unknown format.
		if (format != null && format.getID() == 1)
        {
            name.setDisabled();
        }

		TextArea description = form.addItem().addTextArea("description");
		description.setLabel(T_description);
		description.setValue(descriptionValue);
		description.setSize(3, 35);
		
		Select supportLevel = form.addItem().addSelect("support_level");
		supportLevel.setLabel(T_support);
		supportLevel.setHelp(T_support_help);
		supportLevel.addOption(0,T_support_0);
		supportLevel.addOption(1,T_support_1);
		supportLevel.addOption(2,T_support_2);
		supportLevel.setOptionSelected(supportLevelValue);
		
		CheckBox internal = form.addItem().addCheckBox("internal");
		internal.setLabel(T_internal);
		internal.setHelp(T_internal_help);
		internal.addOption((internalValue != null),"true");
		
		Text extensions = form.addItem().addText("extensions");
		extensions.setLabel(T_extensions);
		extensions.setHelp(T_extensions_help);
		extensions.enableAddOperation();
		extensions.enableDeleteOperation();
		for (String extensionValue : extensionValues)
		{
			extensions.addInstance().setValue(extensionValue);
		}
		
		Item actions = form.addItem();
		actions.addButton("submit_save").setValue(T_submit_save);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);
		
		
		main.addHidden("administrative-continue").setValue(knot.getId());
        
   }
	
}
