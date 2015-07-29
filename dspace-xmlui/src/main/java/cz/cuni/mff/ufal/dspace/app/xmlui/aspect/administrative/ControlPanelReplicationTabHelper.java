package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cocoon.environment.Request;
import org.apache.commons.io.FileUtils;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import cz.cuni.mff.ufal.dspace.b2safe.ReplicationManager;

public class ControlPanelReplicationTabHelper {
	
	private final static String delete_prefix = "checkbox-delete";
	private final static String url_hdl_prefix = ConfigurationManager.getProperty("handle.canonical.prefix");
		
	public static void showTabs(Division div, Request request, Context context) throws WingException {
		String action = request.getParameter("action");
		if(action==null || action.equals("") || action.equals("toggle_on_off")) action = "show_info";
		if(action.equals("replicate_specific") || action.equals("replicate_all_off") || action.equals("replicate_all_on")) action="repl_tobe";
		if(action.equals("Delete")) action = "list_replicas";
		List tabs = div.addList("replication_tabs");
		
		executeCommand(div, request, context);
		
		tabs.addItem(action.equals("show_info")?"active":"", "").addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=show_info", "Info");
		if (ReplicationManager.isReplicationOn()) {			
			tabs.addItem(action.equals("list_replicas")?"active":"", "").addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=list_replicas", "Replicas");
			tabs.addItem(action.equals("repl_tobe")?"active":"", "").addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=repl_tobe", "Missing");
			tabs.addItem(action.equals("repl_not_tobe")?"active":"", "").addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=repl_not_tobe", "Cannot Replicate");
		}
		
	}

	public static void showConfiguration(Division mainDiv, Request request, Context context) throws WingException {
		
		boolean isReplicationOn = ReplicationManager.isReplicationOn();
		
		List statusList = mainDiv.addList("status");

		statusList.addLabel("Replication Service Status");
		String css = isReplicationOn ? "label label-success" : "label label-important";
		String status = isReplicationOn ? "ON" : "OFF";
		String onoff = isReplicationOn ? " TURN OFF" : " TURN ON";		
		statusList.addItem().addHighlight(css).addContent(status);
		statusList.addLabel();
		css = isReplicationOn ? "btn btn-sm btn-danger " : "btn btn-sm btn-primary ";
		statusList.addItem().addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=toggle_on_off", onoff, css + "fa fa-power-off");

		if(isReplicationOn) {
			
			// if not initialized try initializing it
			if (!ReplicationManager.isInitialized()) {
				try {
					ReplicationManager.initialize();
				} catch (Exception e) {
					List info = mainDiv.addList("replication-config");
					info.addItem().addContent(e.toString());
					return;
				}
			}					
			
			Map<String, String> serverInfo = null;
			try {
				serverInfo = ReplicationManager.getServerInformation();
			} catch(Exception e) {
				serverInfo = new HashMap<String, String>();
			}
			
			statusList.addLabel("IRODS Server API Version");
			statusList.addItem(serverInfo.get("API_VERSION"));
			
			statusList.addLabel("IRODS Server Boot Time");
			statusList.addItem(serverInfo.get("SERVER_BOOT_TIME"));
			
			statusList.addLabel("IRODS Server Rel Version");
			statusList.addItem(serverInfo.get("REL_VERSION"));
			
			statusList.addLabel("IRODS Zone");
			statusList.addItem(serverInfo.get("RODS_ZONE"));
			
			statusList.addLabel("IRODS Server Initialize Date");
			statusList.addItem(serverInfo.get("INITIALIZE_DATE"));
	
			Properties config = ReplicationManager.getConfiguration();
	
            Enumeration e = config.propertyNames();

            while (e.hasMoreElements()) {
                  String key = (String) e.nextElement();
                  if ( key.toLowerCase().contains("password") ) {
					continue;
                  }
				String value = config.getProperty(key);
				if (value == null || value.isEmpty()) {
					value = "N/A";
				}
				statusList.addLabel(key);
				statusList.addItem(value);
			}
		}
	}
	
	public static void addForm(Division div, Request request) throws WingException {
		
		List form = div.addList("standalone-programs", List.TYPE_FORM, "cp-programs");
		org.dspace.app.xmlui.wing.element.Item item_row = form.addItem();

		item_row.addButton("submit_repl_list_home").setValue("List HomeDir");
		item_row.addButton("submit_repl_list_replicas").setValue("List Replicas");
		item_row.addButton("submit_repl_tobe").setValue("Tobe Replicated");
		item_row.addButton("submit_repl_not_tobe").setValue("Cannot Replicate");

		form = div.addList("standalone-programs-input", List.TYPE_FORM, "cp-programs");
		
		org.dspace.app.xmlui.wing.element.Item form_item = null;

		form_item = form.addItem(null, "prog-param");
		form_item.addText("submit_repl_missing_count").setValue("3");
		Button btasync = form_item.addButton("submit_repl_missing");
		btasync.setValue("Replicate missing (async.)");
		btasync.setHelp("Number of items to replicate, use with caution as it may be resource intensive");

		form_item = form.addItem(null, "prog-param");
		form_item.addText("submit_repl_replicate_handle").setValue("");
		Button btrepl = form_item.addButton("submit_repl_replicate");
		btrepl.setValue("Replicate specific handle");
		btrepl.setHelp("Enter handle e.g. 11858/00-097C-0000-000D-F696-9");

		form_item = form.addItem(null, "prog-param");
		form_item.addText("submit_repl_delete_filepath").setValue("");
		Button btdel = form_item.addButton("submit_repl_delete");
		btdel.setValue("Delete replica");
		btdel.setHelp("Enter Absolute Remote Path e.g. /CINESZone/home/cuni/dspace_1.8.2_ufal_point_dev/11858_1017.zip");

		form_item = form.addItem(null, "prog-param");
		Text repPath = form_item.addText("submit_repl_download_filepath");
		repPath.setLabel("Remote Path");
		repPath.setHelp("Enter Absolute Remote Path e.g. /CINESZone/home/cuni/dspace_1.8.2_ufal_point_dev/11858_1017.zip");		
		Text localPath = form_item.addText("submit_local_download_filepath");
		localPath.setLabel("Local Path");
		localPath.setHelp("Enter local path where the file should be downloaded.");
		Button btdown = form_item.addButton("submit_repl_download");
		btdown.setValue("Download replica");
		
	}

	public static void executeCommand(Division div, Request request, Context context) throws WingException {

		String action = request.getParameter("action");
		if(action==null || action.equals("")) action = "show_info";

		if(action.equals("toggle_on_off")) {
			boolean on = ReplicationManager.isReplicationOn();
			ReplicationManager.setReplicationOn(!on);
			showConfiguration(div, request, context);
			return;
		}
		
		if(action.equals("Delete")) {
			Map<String, String> params = request.getParameters();
			
			ArrayList<String> todel = new ArrayList<String>();
	
			for (String key : params.keySet()) {
				if (key.startsWith(delete_prefix)) {
					String path = params.get(key);
					todel.add(path);
				}
			}
			
			if(!todel.isEmpty()) {
				Division m = div.addDivision("message", "alert alert-success bold");
				for(String path : todel) {
					try {
						if(ReplicationManager.delete(path)) {
							m.addPara().addContent("Deleted Successfully " + path);
						} else {
							m.addPara(null, "text-error").addContent("Unable to delete " + path);
						}
					} catch (Exception e) {
						m.addPara(null, "text-error").addContent("Unable to delete " + path + " " + e.getLocalizedMessage());
					}
				}
			} else {
				Division m = div.addDivision("message", "alert alert-error");
				m.addPara("Please select an item to delete.");
			}
			action = "list_replicas";
		}
		
		if(action.equals("replicate_all_on")) {
			ReplicationManager.setReplicateAll(true);
			action = "repl_tobe";
		} else
		if(action.equals("replicate_all_off")) {
			ReplicationManager.setReplicateAll(false);
			action = "repl_tobe";
		}
				
		//if (!ReplicationManager.isReplicationOn()) return;
				
		if(action.equals("show_info")) {
			showConfiguration(div, request, context);
		} else
		if(action.equals("list_replicas")) {
			listReplicas(div, request, context);
		} else
		if(action.equals("repl_tobe")){
			showTobeReplicated(div, request, context);
		} else
		if(action.equals("replicate_specific")) {
			replicate(div, request, context);
			showTobeReplicated(div, request, context);
		} else
		if(action.equals("repl_not_tobe")) {
			showCannotReplicate(div, request, context);
		}
		
		// Download path
		//
		else if (request.getParameter("submit_repl_download") != null) {
			try {
				String remPath = request.getParameter("submit_repl_download_filepath");
				String locPath = request.getParameter("submit_local_download_filepath");
				File file = new File(locPath);
				if(file.exists()) {
					file.delete();
				}
				ReplicationManager.retriveFile(remPath, file.getAbsolutePath());
				//message = "file retrived and stored to " + file.getAbsolutePath();
			} catch (Exception e) {
				//message += "Could not download path: " + e.toString();
			}

		}

	}

	public static void showTobeReplicated(Division div, Request request, Context context) throws WingException {		
		int size = 0;
		ItemIterator it;
		try {
			it = Item.findAll(context);
			while (null != it.next()) {
				++size;
			}
			Division m = div.addDivision("message", "alert alert-info");

			if(ReplicationManager.isReplicateAllOn()) {
				m.addPara().addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=replicate_all_off", "REPLICATE ALL DAEMON: ON", "label label-success btn btn-sm pull-right active");
			} else {
				m.addPara().addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=replicate_all_on", "REPLICATE ALL DAEMON: OFF", "label label-warning btn btn-sm pull-right");
			}
			
			m.addPara().addContent(String.format("All items (%d), Public (%d)\n", size, ReplicationManager.getPublicItemHandles().size()));			
			java.util.List<String> tobe = ReplicationManager.listMissingReplicas();
			m.addPara().addContent(String.format("Tobe replicated (%d)", tobe.size()));
			
			Table tobeTable = div.addTable("tobe_replicated", 1, 3);
			
			Row head = tobeTable.addRow(Row.ROLE_HEADER);
			head.addCellContent("#");
			head.addCellContent("ITEM");
			head.addCellContent("REPLICATE");
	
			int i = 1;
			for (String handle : tobe) {
				Row row = tobeTable.addRow(Row.ROLE_DATA);
				row.addCell().addContent(i++);
				row.addCell().addXref(request.getContextPath() + "/handle/" + handle, handle);
				if(ReplicationManager.inProgress.contains(handle)) {
					row.addCell().addHighlight("fa fa-spinner fa-spin");
				} else {
					Cell c = row.addCell();
					if(ReplicationManager.failed.containsKey(handle)) {
						c.addHighlight("label label-important").addContent("ERROR");
						c.addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=replicate_specific" +
								"&handle=" + handle , "", "fa fa-repeat label label-important");						
					} else
					if(ReplicationManager.replicationQueue.contains(handle)){
						c.addHighlight("label label-warning").addContent("QUEUED");
						c.addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=replicate_specific" +
								"&handle=" + handle , "", "fa fa-plus label label-primary");						
					} else {
						c.addXref(request.getContextPath() + "/admin/panel?tab=IRODs Replication&action=replicate_specific" +
								"&handle=" + handle , "", "fa fa-plus label label-primary");
					}
				}
			}
				
		} catch (Exception e) {
			div.addPara("", "alert alert-error").addContent("Could not get list of all items: " + e.toString());
		}
	}

	public static void showCannotReplicate(Division div, Request request, Context context) throws WingException {
		int size = 0;
		ItemIterator it;		
		try {
			it = Item.findAll(context);
			while (null != it.next()) {
				++size;
			}
			
			java.util.List<String> pubItems = ReplicationManager.getPublicItemHandles();
			java.util.List<String> nonPubItems = ReplicationManager.getNonPublicItemHandles();
			
			Division m = div.addDivision("message", "alert alert-info");
			m.addPara().addContent(String.format("All items (%d), Public (%d)", size, pubItems.size()));
			m.addPara().addContent(String.format("NOT going to be replicated (%d)", nonPubItems.size()));			

			Table nottobeTable = div.addTable("tobe_replicated", 1, 2);

			Row head = nottobeTable.addRow(Row.ROLE_HEADER);
			head.addCellContent("#");
			head.addCellContent("ITEM");
			
			int i = 1;
			for (String handle : nonPubItems) {
				Row row = nottobeTable.addRow(Row.ROLE_DATA);
				row.addCell().addContent(i++);
				row.addCell().addXref(request.getContextPath() + "/handle/" + handle, handle);
			}
		} catch (SQLException e) {
			div.addPara("", "alert alert-error").addContent("Could not get list of all items: " + e.toString());
		}
	}

	public static void replicate(Division div, Request request, Context context) throws WingException {
		try {
			String handle = null;
			if (null != request.getParameter("handle")) {
				handle = request.getParameter("handle");
				if (handle.length() == 0) {
					handle = null;
					Division msg = div.addDivision("message", "alert alert-error");
					msg.addPara().addContent("Handle is not provided.");
					return;
				}
			}
			if (handle != null) {
				Item item = (Item) HandleManager.resolveToObject(context, handle);
				if (item != null) {
					try {
						Division msg = div.addDivision("message", "alert alert-success");
						ReplicationManager.replicate(context, handle, item, true);
						msg.addPara().addContent("Replication started successfully for item " + handle);
					} catch (Exception e) {
						Division msg = div.addDivision("message", "alert alert-error");
						msg.addPara().addContent("Replication Failed for handle " + handle);
						msg.addPara().addContent(e.toString());
					}

				} else {
					Division msg = div.addDivision("message", "alert alert-error");
					msg.addPara().addContent(String.format("Invalid handle [%s] supplied - cannot find the handle!", handle));
				}
			}
		} catch (Exception e) {
			Division msg = div.addDivision("message", "alert alert-error");
			msg.addPara().addContent("Replication Failed");
			msg.addPara().addContent(e.toString());
		}
	}

	public static void listReplicas(Division div, Request request, Context context) throws WingException {
		java.util.List<String> list = new ArrayList<>();
		try {
			list = ReplicationManager.listFilenames(true);
		} catch (Exception e) {
			Division msg = div.addDivision("message", "alert alert-error");
			msg.addPara().addContent("Replication Failed");
			msg.addPara().addContent(e.getLocalizedMessage());			
		}
		
		Division msg = div.addDivision("message", "alert alert-info");
		
		msg.addPara().addHighlight("pull-right").addButton("action", "label label-important btn btn-sm btn-danger").setValue("Delete");
		
		// display it
		Table table = div.addTable("replica_items", 1, 6);
		Row head = table.addRow(Row.ROLE_HEADER);
		head.addCellContent("#");
		head.addCellContent("STATUS");
		head.addCellContent("ITEM");
		head.addCellContent("SIZE REP/ORIG");
		head.addCellContent("INFO");
		head.addCell("DEL_COL", null, null).addContent("DEL");

		int pos = 0;
		long all_file_size = 0;

		for (String name : list) {

			Row row = table.addRow(Row.ROLE_DATA);
			row.addCellContent(String.valueOf(++pos));
							
			Map<String, String> metadata;
			try {
				metadata = ReplicationManager.getMetadataOfDataObject(name);
			} catch (Exception e) {
				throw new WingException(e);
			}
							
			String adminStatus = metadata.get("ADMIN_Status");
			if(adminStatus==null) adminStatus = "NA";
			
			String rend_status = "label ";
			if (adminStatus!=null && adminStatus.equals("Archive_ok")) {
				rend_status += "label-success fa fa-check bold";
			} else if (adminStatus!=null && adminStatus.startsWith("Error")) {
				rend_status += "label-important";
			} else {
				row.addCell().addHighlight("label label-warning").addHighlight("fa fa-spinner fa-spin");
				row.addCell(1, 4).addContent(name);
				continue;
			}
						
			row.addCell().addHighlight(rend_status).addContent(adminStatus);
			
			String eudatPID = metadata.get("EUDAT_PID");
			if (eudatPID!=null) {
				eudatPID = "http://hdl.handle.net" + eudatPID;
			}

			row.addCell().addXref(eudatPID, metadata.get("EUDAT_ROR"));
			
			// check md5 too
			String md5 = metadata.get("INFO_Checksum");
			String original_md5 = metadata.get("OTHER_original_checksum");
			long orig_file_size = -1;
			try {
				orig_file_size = Long.valueOf(metadata.get("OTHER_original_filesize"));
			} catch(NumberFormatException e) {
				
			}
			
			String itemHandle = metadata.get("EUDAT_ROR");
							
			String sizes = orig_file_size < 0 ? "NA" : FileUtils.byteCountToDisplaySize(orig_file_size);
			sizes += " / ";
			all_file_size += orig_file_size;

			if(itemHandle != null) {
				itemHandle = itemHandle.substring(url_hdl_prefix.length());
				try {
					Item item = (Item) HandleManager.resolveToObject(context, itemHandle);
					sizes += FileUtils.byteCountToDisplaySize(item.getTotalSize());
				} catch (Exception e) {
				}					
			}
			
			row.addCell().addHighlight("label label-info").addContent(sizes);
			
			// are md5 ok?
			if (!adminStatus.equals("Archive_ok")) {
				rend_status = "hidden";
			} else if (original_md5 != null && md5 != null && !original_md5.equals(md5)) {
				rend_status = "label label-important";
			} else {
				rend_status = "label label-success fa fa-check bold";
			}
			
			Cell c = row.addCell();
			
			c.addHighlight(rend_status).addContent(" " + md5);
			c.addHighlight("label label-default fa fa-clock-o bold").addContent(" " + metadata.get("INFO_TimeOfTransfer"));
			
			CheckBox r = row.addCell("todelete", Row.ROLE_DATA, "todelete").addCheckBox(delete_prefix + "_" + pos);
			r.addOption(name);

		}
		
		msg.addPara().addContent("Total replicated items: " + pos);
		msg.addPara().addContent("Total replicated size: " + FileUtils.byteCountToDisplaySize(all_file_size));
		
		
	} // list_replicas	
	
}