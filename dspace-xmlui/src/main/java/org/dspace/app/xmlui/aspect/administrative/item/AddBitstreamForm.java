/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.commons.lang.ArrayUtils;

import org.dspace.app.xmlui.aspect.submission.submit.AccessStepUtil;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.File;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

/**
 * 
 * Show a form that allows the user to upload a new bitstream. The 
 * user can select the new bitstream's bundle (which is unchangeable
 * after upload) and a description for the file.
 * 
 * @author Scott Phillips
 */
public class AddBitstreamForm extends AbstractDSpaceTransformer
{
	
	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");

	private static final Message T_title = message("xmlui.administrative.item.AddBitstreamForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.AddBitstreamForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.AddBitstreamForm.head1");
	private static final Message T_bundle_label = message("xmlui.administrative.item.AddBitstreamForm.bundle_label");
	private static final Message T_file_label = message("xmlui.administrative.item.AddBitstreamForm.file_label");
	private static final Message T_file_help = message("xmlui.administrative.item.AddBitstreamForm.file_help");
	private static final Message T_description_label = message("xmlui.administrative.item.AddBitstreamForm.description_label");
	private static final Message T_description_help = message("xmlui.administrative.item.AddBitstreamForm.description_help");
	private static final Message T_submit_upload = message("xmlui.administrative.item.AddBitstreamForm.submit_upload");

	private static final Message T_no_bundles = message("xmlui.administrative.item.AddBitstreamForm.no_bundles");

	
	private static final String[] DEFAULT_BUNDLE_LIST = new String[]{"ORIGINAL", "METADATA", "THUMBNAIL", "LICENSE", "CC-LICENSE"};

    private boolean isAdvancedFormEnabled=true;

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/item", T_item_trail);
        pageMeta.addTrail().addContent(T_trail);
        pageMeta.addMetadata("javascript", "static").addContent("static/js/editItemUtil.js");
    }

    @Override
	public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException
	{
            isAdvancedFormEnabled=DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

            UUID itemID = UUID.fromString(parameters.getParameter("itemID", null));
            org.dspace.content.Item item = itemService.find(context, itemID);

            // DIVISION: main div
            Division div = body.addInteractiveDivision("add-bitstream", contextPath + "/admin/item", Division.METHOD_MULTIPART, "primary administrative item");

            // LIST: upload form
            List upload = div.addList("submit-upload-new", List.TYPE_FORM);
            upload.setHead(T_head1);

            int bundleCount = 0; // record how many bundles we are able to upload too.
            Select select = upload.addItem().addSelect("bundle");
            select.setLabel(T_bundle_label);

            // Get the list of bundles to allow the user to upload too. Either use the default
            // or one supplied from the dspace.cfg.
            String[] bundles = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("xmlui.bundle.upload");
            if (ArrayUtils.isEmpty(bundles))
            {
                bundles = DEFAULT_BUNDLE_LIST;
            }
            for (String part : bundles)
            {
                if (addBundleOption(item, select, part.trim()))
                {
                    bundleCount++;
                }
            }
            select.setOptionSelected("ORIGINAL");

            if (bundleCount == 0) {
                select.setDisabled();
            }


            File file = upload.addItem().addFile("file");
            file.setLabel(T_file_label);
            file.setHelp(T_file_help);
            file.setRequired();

            if (bundleCount == 0) {
                file.setDisabled();
            }

            Text description = upload.addItem().addText("description");
            description.setLabel(T_description_label);
            description.setHelp(T_description_help);

            if (bundleCount == 0) {
                description.setDisabled();
            }

            if (bundleCount == 0) {
                upload.addItem().addContent(T_no_bundles);
            }

            // EMBARGO FIELD
            // if AdvancedAccessPolicy=false: add Embargo Fields.
            if(!isAdvancedFormEnabled){
                AccessStepUtil asu = new AccessStepUtil(context);
                // if the item is embargoed default value will be displayed.
                asu.addEmbargoDateSimpleForm(item, upload, -1);
                asu.addReason(null, upload, -1);
            }



            // ITEM: actions
            Item actions = upload.addItem();
            Button button = actions.addButton("submit_upload");
            button.setValue(T_submit_upload);
            if (bundleCount == 0) {
                button.setDisabled();
            }

            actions.addButton("submit_cancel").setValue(T_submit_cancel);

            div.addHidden("administrative-continue").setValue(knot.getId());
        }
	
	/**
         * Add the bundleName to the list of bundles available to submit to. 
         * Performs an authorization check that the current user has privileges 
         * @param item DSO item being evaluated
         * @param select DRI wing select box that is being added to
         * @param bundleName the new bundle name.
         * @return boolean indicating whether user can upload to bundle
         * @throws SQLException passed through.
         * @throws WingException passed through.
         */
        public boolean addBundleOption(org.dspace.content.Item item, Select select, String bundleName) throws SQLException, WingException
	{
            java.util.List<Bundle> bundles = itemService.getBundles(item, bundleName);
            if (bundles == null || bundles.size() == 0)
            {
                // No bundle, so the user has to be authorized to add to item.
                if(!authorizeService.authorizeActionBoolean(context, item, Constants.ADD))
                {
                    return false;
                }
            } else
            {
                // At least one bundle exists, does the user have privileges to upload to it?
                Bundle bundle = bundles.get(0);
                if (!authorizeService.authorizeActionBoolean(context, bundle, Constants.ADD))
                {
                    return false; // you can't upload to this bundle.
                }

                // You also need the write privlege on the bundle.
                if (!authorizeService.authorizeActionBoolean(context, bundle, Constants.WRITE))
                {
                    return false;  // you can't upload
                }
            }

            // It's okay to upload.
            select.addOption(bundleName, message("xmlui.administrative.item.AddBitstreamForm.bundle." + bundleName));
            return true;
        }
	
}
