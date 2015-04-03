/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.File;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.dspace.IOUtils;

/**
 * UFAL addition.
 */
public class ControlPanelProgramsTab extends AbstractControlPanelTab {

	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelProgramsTab.class);


	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);
		
		div.addPara(null, "alert")
				.addContent(
						"[Ran] These programs are usually called from installation "
								+ "bin directory 'bin/dspace' and the appropriate classes are taken "
								+ "from source 'dspace/config/launcher.xml'");

		List form = div.addList("standalone-programs");
		form.setHead("Standalone programs to run");

		form.addLabel(null, "bold").addContent("SOLR backend (browse/search) (index-discovery)");
		form.addItem().addButton("submit_update_solr").setValue("Clean then reindex solr (-f)");
		
		form.addLabel();
		form.addItem().addButton("submit_rebuild_solr").setValue("Update solr (without clean!) (-b)");
		
		form.addLabel();
		form.addItem().addButton("submit_clean_solr").setValue("Clean (force) solr (-c -f)");
		
		form.addLabel();
		form.addItem().addButton("submit_opt_solr").setValue("Optimise solr (you sure?) (-o)");

		form.addLabel();
		form.addItem().addContent(" ");
		form.addLabel(null, "bold").addContent("Assetstore");
		form.addItem().addButton("submit_verify_assetstore").setValue("Verify assetstore (can take long)");
		
		form.addLabel();
		form.addItem().addContent(" ");
		form.addLabel(null, "bold").addContent("Other");
		Item item = form.addItem();
		Button tmp = item.addButton("submit_cleanup");
		tmp.setValue("Cleanup assetstore");
		item.addHighlight("container-fluid text-error").addContent("Will remove files from disk AND from bitstream table.");
		item.addHighlight("container-fluid text-error").addContent("If not done with delete files, checksum curation can express discontent!");
		
		form.addLabel();
		form.addItem().addButton("submit_test_email").setValue("Test email");
		form.addLabel();
		form.addItem().addButton("submit_sitemaps").setValue("Generate sitemaps");

		form.addLabel();
		Item it = form.addItem();
		it.addText("submit_ufal_stats_email").setValue("default");

		form.addLabel();		
		form.addItem().addContent(" ");
		
		form.addLabel(null, "bold").addContent("Statistics");
		it.addButton("submit_ufal_stats").setValue("Send UFAL stats");
		form.addItem().addButton("submit_general_statistics").setValue("Compile stats (stat-initial/stat-general)");
        form.addLabel();
        form.addItem().addButton("submit_update_bot_flag").setValue("Update isBot flag (limit one run to 100.000?!)");
		

		//
		String message = null;
		String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
		// solr
		//
		if (request.getParameter("submit_update_solr") != null) {
			message = IOUtils.run(new File(dspace_dir + "/bin/"), new String[] {
					"./dspace", "index-discovery", "-f" });
		} else if (request.getParameter("submit_rebuild_solr") != null) {
			message = IOUtils.run(new File(dspace_dir + "/bin/"), new String[] {
					"./dspace", "index-discovery", "-b" });
		} else if (request.getParameter("submit_opt_solr") != null) {
			message = IOUtils.run(new File(dspace_dir + "/bin/"), new String[] {
					"./dspace", "index-discovery", "-o" });
		} else if (request.getParameter("submit_clean_solr") != null) {
			message = IOUtils.run(new File(dspace_dir + "/bin/"), new String[] {
					"./dspace", "index-discovery", "-c", "-f" });
		}
		// assetstore
		//
		else if (request.getParameter("submit_verify_assetstore") != null) {
	    	String assetstore_dir = dspace_dir + "/assetstore";
	    	message = String.format("Running python main.py --dir=%s\n", assetstore_dir);
	    	message += IOUtils.run( new File(dspace_dir+"/bin/validators/assetstore/"), 
	    			new String[] {"python", "main.py", "--dir=" + assetstore_dir} );
		}	
		// statistics
		// 
        else if (request.getParameter("submit_general_statistics") != null) {
            message = IOUtils.run( new File(dspace_dir+"/bin/"), 
                            new String[] {"./dspace", "stat-initial"} );
            message += "\n=========================================\n\n";
            message += IOUtils.run( new File(dspace_dir+"/bin/"), 
                    new String[] {"./dspace", "stat-general"} );
        }
        else if (request.getParameter("submit_update_bot_flag") != null) {
            message = IOUtils.run( new File(dspace_dir+"/bin/"), 
                    new String[] {"./dspace", "stats-util", "-m"} );

	    }
		// others
		//
		else if (request.getParameter("submit_cleanup") != null) {
			message = IOUtils.run(new File(dspace_dir + "/bin/"), new String[] {
					"./dspace", "cleanup" });
		} else if (request.getParameter("submit_test_email") != null) {
			message = IOUtils.run(new File(dspace_dir + "/bin/"), new String[] {
					"./dspace", "test-email" });
		} else if (request.getParameter("submit_sitemaps") != null) {
			message = IOUtils.run(new File(dspace_dir + "/bin/"), new String[] {
					"./dspace", "generate-sitemaps" });
		} else if (request.getParameter("submit_ufal_stats") != null) {
			String email = "";
			if (null != request.getParameter("submit_ufal_stats_email")) {
				email = request.getParameter("submit_ufal_stats_email");
			}
			if (email == null || email.length() == 0) {
				message = IOUtils.run(new File(dspace_dir + "/bin/"),
						new String[] { "./dspace", "ufal-info" });
			} else {
				message = IOUtils.run(new File(dspace_dir + "/bin/"),
						new String[] { "./dspace", "ufal-info", "--email",
								email });
			}
		}

		if (message != null) {
			div.addDivision("result", "alert alert-info")
					.addPara("programs-output", "programs-result")
					.addContent(message);
		}
	}

}

