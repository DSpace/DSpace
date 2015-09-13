/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.administrative;

import java.sql.SQLException;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.aspect.administrative.item.ViewItem;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;


/**
 * Display information about the item and allow the user to change 
 * 
 * @author Amir Kamran
 */


public class EditItemServicesForm extends AbstractDSpaceTransformer {
	
	static Logger log = Logger.getLogger(EditItemServicesForm.class);

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");	
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
			
	private static final Message T_title = message("xmlui.administrative.item.EditItemServicesForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.EditItemServicesForm.trail");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		
		Item item = Item.find(context, itemID);
		String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();
						
		
		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-services", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative edit-item-services");
		main.setHead(T_option_head);

		String tabLink = baseURL + "&services";
		
		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		ViewItem.add_options(context, eperson, options, baseURL, ViewItem.T_option_services, tabLink);
		
		String featuredServices = ConfigurationManager.getProperty("lr", "featured.services");
		
		if(featuredServices!=null && !featuredServices.isEmpty()) {
			
			Division fsDiv = main.addDivision("featuredService", "");

			for(String featuredService : featuredServices.split(",")) {
								
				String name = ConfigurationManager.getProperty("lr", "featured.service." + featuredService + ".fullname");
				String url = ConfigurationManager.getProperty("lr", "featured.service." + featuredService + ".url");
				String description = ConfigurationManager.getProperty("lr", "featured.service." + featuredService + ".description");					

				Division fsInnerDiv = fsDiv.addDivision(featuredService, "well well-white").addDivision("caption", "caption");
				fsInnerDiv.addPara(null, "h3").addXref(url, name, "target_blank");
				fsInnerDiv.addPara(description);
				List service_urls = fsInnerDiv.addList("service_urls", List.TYPE_GLOSS);
				service_urls.addLabel("base_url_label", "").addContent("Service Base URL");
				service_urls.addItem("base_url_value", "").addContent(url);
				service_urls.addLabel("add_links_label", "").addContent("Add Item integration links");
				Metadatum[] mds = item.getMetadataByMetadataString("local.featuredService." + featuredService);
				if(mds!=null && mds.length!=0) {
					int c = 0;
					for(Metadatum md : mds) {
						c++;
						String []key_value = md.value.split("\\|");
						org.dspace.app.xmlui.wing.element.Item inputs = service_urls.addItem("text_fields_" + c, "");
						inputs.addText(featuredService + "_url_key_" + c, "url_key").setValue(key_value[0]);
						inputs.addText(featuredService + "_url_value_" + c, "url_value").setValue(key_value[1]);						
					}
					service_urls.addItem("url_count", "hidden").addHidden(featuredService + "_url_count").setValue(c);
					org.dspace.app.xmlui.wing.element.Item btns = service_urls.addItem("buttons", "");
					Button update = btns.addButton("update", "btn btn-sm btn-info");
					update.setLabel("Update");
					update.setValue(featuredService);
					Button deactivate = btns.addButton("deactivate", "btn btn-sm btn-danger");
					deactivate.setLabel("Deactivate");
					deactivate.setValue(featuredService);
				} else {
					org.dspace.app.xmlui.wing.element.Item inputs = service_urls.addItem("text_fields_1", "");
					inputs.addText(featuredService + "_url_key_1", "url_key");
					inputs.addText(featuredService + "_url_value_1", "url_value");
					service_urls.addItem("url_count", "hidden").addHidden(featuredService + "_url_count", "url_count").setValue(1);
					org.dspace.app.xmlui.wing.element.Item btns = service_urls.addItem("buttons", "");
					Button activate = btns.addButton("activate", "btn btn-sm btn-info");
					activate.setLabel("Activate");
					activate.setValue(featuredService);					
				}
			}
		
		} else {
			
		}

		main.addPara().addHidden("administrative-continue").setValue(knot.getId());

	}
	
	public static FlowResult activate(Context context, int itemID, String serviceName, Request request) {
		FlowResult result = new FlowResult();
		if(serviceName == null || serviceName.isEmpty()) {
			result.setOutcome(false);
			result.setContinue(false);
			result.addError("Invalid Service.");
			return result;
		}
		
		try {
			Item item = Item.find(context, itemID);
			int count = Integer.parseInt(request.getParameter(serviceName + "_url_count"));
			
			item.clearMetadata("local", "featuredService", serviceName, Item.ANY);
									
			for(int i=1;i<=count;i++) {
			
				String key = request.getParameter(serviceName + "_url_key_" + i);
				String value = request.getParameter(serviceName + "_url_value_" + i);
				
				if(key==null || key.isEmpty() || value==null || value.isEmpty()) continue;
							
				item.addMetadata("local", "featuredService", serviceName, Item.ANY, key + "|" + value);
				
			}
						
			item.update();
			
			result.setContinue(true);
			result.setOutcome(true);
			if(request.get("update")!=null) {
				result.setMessage(new Message("default", "Service Updated Successfully"));
			} else {
				result.setMessage(new Message("default", "Service Activated Successfully"));
			}
		} catch (Exception e) {
			log.error(e);
			result.setOutcome(false);
			result.setContinue(false);
			result.addError(e.getMessage());
		}
		
		return result;
	}

	public static FlowResult deactivate(Context context, int itemID, String serviceName) {
		FlowResult result = new FlowResult();
		if(serviceName == null || serviceName.isEmpty()) {
			result.setOutcome(false);
			result.setContinue(false);
			return result;
		}
		
		try {
			Item item = Item.find(context, itemID);		
			item.clearMetadata("local", "featuredService", serviceName, Item.ANY);
			item.update();
			result.setContinue(true);
			result.setOutcome(true);
			result.setMessage(new Message("default", "Service Deactivated Successfully"));			
		} catch (Exception e) {
			log.error(e);
			result.setOutcome(false);
			result.setContinue(false);			
		}
		
		return result;
	}

	public static FlowResult update(Context context, int itemID, String serviceName, Request request) {
		return activate(context, itemID, serviceName, request);
	}

}
