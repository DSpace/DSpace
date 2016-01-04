/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.util.Map;

import org.dspace.app.xmlui.aspect.administrative.controlpanel.AbstractControlPanelTab;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.DSpaceApi;

public class ControlPanelConfigurationTab extends AbstractControlPanelTab {

	private static final Message T_DSPACE_HEAD = message("xmlui.administrative.ControlPanel.dspace_head");
	private static final String T_UNSET = "UNSET";

	/**
	 * Guarantee a non-null String.
	 *
	 * @param value candidate string.
	 * @return {@code value} or a constant indicating an unset value.
	 */
	private static String notempty(String value) { return (null == value || "".equals(value)) ? T_UNSET : value; }

	@Override
	public void addBody(Map objectModel, Division div) throws WingException {
		// LIST: DSpace
		List dspace = div.addList("dspace");
		dspace.setHead(T_DSPACE_HEAD);


		// ufal
		dspace.addLabel("License database URL");
		final String licDBurl = DSpaceApi.getFunctionalityManager().get("lr.utilities.db.url");
		if (licDBurl != null && !licDBurl.equals("")) {
			dspace.addItem(licDBurl);
		} else {
			dspace.addItem("unknown");
		}
		dspace.addLabel("Site handle (e.g., used in curation)");
		dspace.addItem(Site.getSiteHandle());

		dspace.addLabel("OAI url");
		String oai_url = notempty(ConfigurationManager.getProperty("oai", "dspace.oai.url"));
		dspace.addItemXref(oai_url, oai_url);

		dspace.addLabel("Solr log url");
		String oai_solr = notempty(ConfigurationManager.getProperty("solr.log.server"));
		dspace.addItemXref(oai_solr, oai_solr);

		List ufaladd = div.addList("LINDAT_Utilities");
		ufaladd.setHead("LINDAT Utilities");

		ufaladd.addLabel("Help mail");
		ufaladd.addItem(notempty(ConfigurationManager.getProperty("lr.help.mail")));

		ufaladd.addLabel("Assetstore");
		ufaladd.addItem(notempty(ConfigurationManager.getProperty("assetstore.dir")));

		ufaladd.addLabel("Postgresql logging dir (default)");
		ufaladd.addItem("/var/log/postgresql");
	}

}

