package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.cocoon.environment.Request;
import org.apache.commons.io.FileUtils;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import cz.cuni.mff.ufal.dspace.b2safe.ReplicationManager;
import fr.cines.eudat.repopack.b2safe_rp_core.DataObject;

public class ControlPanelReplicationTabHelper {
	
	private final static String delete_prefix = "checkbox-delete";
	private final static String url_hdl_prefix = ConfigurationManager.getProperty("handle.canonical.prefix");


	public static void showConfiguration(Division mainDiv) throws WingException {
		
		Division div = mainDiv.addDivision("replication_div", "itemlist");
		div.addPara("", "header").addContent("Configuration");
		Table table = div.addTable("replication_config", 1, 2, "font_smaller");
		
		Row row_tmp = null;
		
		boolean isReplicationOn = ReplicationManager.isReplicationOn();
		row_tmp = table.addRow(null, Row.ROLE_DATA, isReplicationOn ? "alert alert-success" : "alert alert-error");
		row_tmp.addCellContent("Replication Service Status");
		row_tmp.addCellContent(isReplicationOn ? "on" : "off");
		row_tmp = table.addRow(null, Row.ROLE_DATA, isReplicationOn ? "alert alert-success" : "alert alert-error");
		row_tmp.addCellContent("Replication On/Off");
		row_tmp.addCell().addButton("submit_repl_on_toggle").setValue("Turn on/off");

		if(isReplicationOn) {

			Map<String, String> serverInfo = null;
			try {
				serverInfo = ReplicationManager.getServerInformation();
			} catch(Exception e) {

				
			}
           if ( null != serverInfo ) {
	  	        for ( Map.Entry<String, String> entry : serverInfo.entrySet() ) {
	  		        row_tmp = table.addRow(Row.ROLE_DATA);
			        row_tmp.addCellContent(entry.getKey());
			        row_tmp.addCellContent(entry.getValue());
                }
           }
			Properties config = ReplicationManager.getConfiguration();
	
            java.util.Enumeration e = config.propertyNames();

            while (e.hasMoreElements()) {
                  String key = (String) e.nextElement();
                  if ( key.toLowerCase().contains("password") ) {
                      continue;
                  }
				Row row = table.addRow(Row.ROLE_DATA);
				row.addCellContent(key);
				String value = config.getProperty(key);
				if (value == null) {
					value = "N/A";
				}
				row.addCellContent(value);
			}
		}
	}
	
	public static void addForm(Division div) throws WingException {
		List form = div.addList("standalone-programs", List.TYPE_FORM, "cp-programs");
		org.dspace.app.xmlui.wing.element.Item item_row = form.addItem();

		item_row.addButton("submit_repl_list_home").setValue("List HomeDir");
		item_row.addButton("submit_repl_list_replicas").setValue("List Replicas");
		item_row.addButton("submit_repl_tobe").setValue("Tobe Replicated");
		item_row.addButton("submit_repl_not_tobe").setValue("Cannot Replicate");

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

	public static boolean shouldListReplicas(Request request) {
		return request.getParameter("submit_repl_list_replicas") != null
				|| request.getParameter("submit_repl_delete") != null;
	}

	public static String executeCommand(Request request, Context context) {
		String message = null;

		if (request.getParameter("submit_repl_list_home") != null) {
			message = "Files in home directory:                     \n\n";
			try {
				for (DataObject one_do : ReplicationManager.list()) {
                    String name = one_do.getFileName();
                    if ( null == name ) {
                        name = one_do.getRemoteDirPath();
                    }
					message += name + "\n";
				}
			} catch (Exception e) {
				message = e.getLocalizedMessage();
			}
		}

		else if (request.getParameter("submit_repl_on_toggle") != null) {
			boolean on = ReplicationManager.isReplicationOn();
			ReplicationManager.setReplicationOn(!on);
			message = String.format("Replication turned %s", !on ? "on" : "off");
		}

		else if (request.getParameter("submit_repl_tobe") != null) {
			message = showTobeReplicated(context);
		}

		else if (request.getParameter("submit_repl_not_tobe") != null) {
			message = showCannotReplicate(context);
		}

		// Replicate specific handle
		//
		else if (request.getParameter("submit_repl_replicate") != null) {
			message = replicate(request, context);
		}

		// Replicate missing items async.
		//
		else if (request.getParameter("submit_repl_missing") != null) {
			try {
				String param = request.getParameter("submit_repl_missing_count");
				int count = 10;
				try {
					count = Integer.valueOf(param);
				} catch (Exception e) {
				}
				ReplicationManager.replicateMissing(context, count);
				message = String.format("Replication of [%s] items started", count);
			} catch (Exception e) {
				message = "Could not replicate missing: " + e.toString();
			}

		}

		// Delete path
		//
		else if (request.getParameter("submit_repl_delete") != null) {
			try {
				String param = request.getParameter("submit_repl_delete_filepath");
				if (null == param || 0 == param.length()) {
					
					@SuppressWarnings("unchecked")
					Map<String, String> params = request.getParameters();
					
					message = "";
					
					for (String key : params.keySet()) {
						if (key.startsWith(delete_prefix)) {
							param = params.get(key);							
							message += String.format("Deleting [%s] ... ", param);
							message += ReplicationManager.delete(param);
							message += "\n";
						}
					}

				} else {
					if(ReplicationManager.delete(param)) {
						message = "Successfully deleted " + param;
					}
				}
			} catch (Exception e) {
				message += "Could not delete path: " + e.toString();
			}

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
				ReplicationManager.retrieveFile(remPath, file.getAbsolutePath());
				message = "file retrieved and stored to " + file.getAbsolutePath();
			} catch (Exception e) {
				message += "Could not download path: " + e.toString();
			}

		}

		return message;
	}

	public static String showTobeReplicated(Context context) {
		String message = "Replicas to be replicated:\n";
		int size = 0;
		ItemIterator it;
		try {
			it = Item.findAll(context);
			while (null != it.next()) {
				++size;
			}

			message += String.format("All items (%d), public: (%d)\n", size,
					ReplicationManager.getPublicItemHandles().size());
			java.util.List<String> tobe = ReplicationManager.listMissingReplicas();
			message += String.format("Tobe replicated (%d):\n", tobe.size());
			for (String f : tobe)
				message += String.format("%s (%s%s)\n", f, url_hdl_prefix, f);
		} catch (Exception e) {
			message += "Could not get list of all items: " + e.toString();
		}
		return message;
	}

	public static String showCannotReplicate(Context context) {
		String message = "Replicas to be replicated:\n";
		int size = 0;
		ItemIterator it;
		try {
			java.util.List<String> pubItems = ReplicationManager.getPublicItemHandles();
			java.util.List<String> nonPubItems = ReplicationManager.getNonPublicItemHandles();
			message += String.format("All items (%d), public: (%d)\n", size, pubItems.size());
			message += String.format("NOT going to be replicated (%d):\n", nonPubItems.size());

			it = Item.findAll(context);
			while (it.next() != null) {
				size++;
				for (String f : nonPubItems) {
					//message += String.format("%s (%s%s)\n", f, url_hdl_prefix, f);
				}
			}
		} catch (SQLException e) {
			message += "Could not get list of all items: " + e.toString();
		}
		return message;
	}

	public static String replicate(Request request, Context context) {
		String message = "Replicating...\n";
		try {
			String handle = null;
			if (null != request.getParameter("submit_repl_replicate_handle")) {
				handle = request.getParameter("submit_repl_replicate_handle");
				if (handle.length() == 0) {
					handle = null;
				}
			}
			if (handle != null) {
				Item item = (Item) HandleManager.resolveToObject(context, handle);
				if (item != null) {
					try {

						ReplicationManager.replicate(context, handle, item, true);
						message += "Replication started...";
					} catch (Exception e) {
						message += "Replication failed - " + e.toString();
					}

				} else {
					message = String.format("Invalid handle [%s] supplied - cannot find the handle!", handle);
				}
			} else {
				message = "No handle supplied!";
			}
		} catch (Exception e) {
			message += "Could not replicate item: " + e.toString();
		}

		return message;
	}

	public static String listReplicas(Division div, Request request, Context context) throws Exception {
		String message = null;
		java.util.List<DataObject> dos = null;
		try {
			dos = ReplicationManager.list();
		} catch (Exception e) {
			message = e.getLocalizedMessage();
			return message;
		}
		// display it
		Table table = div.addTable("replica_items", 1, 3, "font_smaller");
		Row head = table.addRow(Row.ROLE_HEADER);
		head.addCellContent("#");
		head.addCellContent("STATUS");
		head.addCellContent("ITEM");
		head.addCellContent("REPLICA SIZE");
		head.addCellContent("INFO");
		head.addCellContent("Delete");

		int pos = 0;
		long all_file_size = 0;


		for (DataObject one_do : dos) {

			Row row = table.addRow(Row.ROLE_DATA);
			row.addCellContent(String.valueOf(pos + 1));
			
            // eudat stores the file path here
            String name = one_do.getFileName();
            if ( null == name ) {
                name = one_do.getRemoteDirPath();
            }

			Map<String, String> metadata = ReplicationManager.getMetadataMap(one_do);
			String adminStatus = metadata.get("ADMIN_Status");
			
			String rend_status = "notok";
			if (adminStatus!=null && adminStatus.equals("Archive_ok")) {
				rend_status = "ok";
			} else if (adminStatus!=null && !adminStatus.startsWith("Error")) {
				rend_status = "running";
			}
			
			row.addCell("status", Row.ROLE_DATA, "replica_status " + rend_status).addContent(adminStatus);
			
			String eudatPID = one_do.getEudatPid();
            if ( null == eudatPID ) {
                eudatPID = metadata.get("EUDAT_PID");
            }
			if ( null != eudatPID ) {
				eudatPID = "http://hdl.handle.net/" + eudatPID;
			}

			row.addCell().addXref(eudatPID, name);
			
			// check md5 too
            String original_md5 = metadata.get("OTHER_original_checksum");
			String md5 = one_do.getChecksum();
            if ( null == md5 ) {
                md5 = metadata.get("INFO_Checksum");
            }
			String itemHandle = one_do.getRor();
            if ( null == itemHandle ) {
                itemHandle = metadata.get("EUDAT_ROR");
            }

            long orig_file_size = -1;
            try {
                orig_file_size = Long.valueOf(metadata.get("OTHER_original_filesize"));
            } catch(NumberFormatException e) {
            }

			String sizes = orig_file_size < 0 ? "N/A" : FileUtils.byteCountToDisplaySize(orig_file_size);
			all_file_size += orig_file_size;

			if(itemHandle != null) {
				itemHandle = itemHandle.substring(url_hdl_prefix.length());
				try {
					Item item = (Item) HandleManager.resolveToObject(context, itemHandle);
				} catch (Exception e) {
				}					
			}
			
			row.addCellContent(sizes);
			
			// are md5 ok?
            if (rend_status.equals("ok") && (original_md5 == null || md5 == null)) {
                rend_status = "notok";
            } else if (original_md5 != null && md5 != null && !original_md5.equals(md5)) {
                rend_status = "notok";
            }

            Cell c = row.addCell();
            c.addHighlight(rend_status).addContent("MD5: " + md5);
			
			CheckBox r = row.addCell("todelete", Row.ROLE_DATA, null).addCheckBox(String.format("%s-%d", delete_prefix, pos+1));
			r.addOption(name);
			++pos;
		}

		div.setHead(String.format("Replicated files [#%s] totalling [%s]", dos.size(),
				all_file_size < 0 ? "N/A" : FileUtils.byteCountToDisplaySize(all_file_size)));
		
		return message;
		
	} // list_replicas	
	
}

