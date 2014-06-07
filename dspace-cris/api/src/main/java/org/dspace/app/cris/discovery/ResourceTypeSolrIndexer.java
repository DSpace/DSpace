/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.AType;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ACrisObjectWithTypeSupport;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.SolrServiceIndexPlugin;

public class ResourceTypeSolrIndexer implements CrisServiceIndexPlugin,
		SolrServiceIndexPlugin {

	private static final String PLACEHOLDER = "@label@";
	private static final String REGEX = "\\S+(?:\\s*\\|\\|\\|(\\s*\\S+))+";

	@Override
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACrisObject<P, TP, NP, NTP, ACNO, ATNO> crisObject,
			SolrInputDocument document) {

		String acvalue = "";
		String fvalue = "";

		Integer type = crisObject.getType();
		if (type > CrisConstants.CRIS_DYNAMIC_TYPE_ID_START) {
			acvalue = ConfigurationManager.getProperty(
					CrisConstants.CFG_MODULE, "facet.type.crisdo."
							+ crisObject.getTypeText().toLowerCase());
			if (acvalue == null) {
				acvalue = ConfigurationManager.getProperty(
						CrisConstants.CFG_MODULE, "facet.type.crisdo.default");
			}

			if (acvalue == null || PLACEHOLDER.equals(acvalue)) {
			    String separatorFacets = ConfigurationManager.getProperty("discovery", "solr.facets.split.char");
				String label = ((ACrisObjectWithTypeSupport<P, TP, NP, NTP, ACNO, ATNO>) crisObject)
						.getTypo().getLabel();
				acvalue = label.toLowerCase() + (separatorFacets!=null?separatorFacets:SolrServiceImpl.FILTER_SEPARATOR) + label;
			}
			fvalue = acvalue;
		} else {
			acvalue = ConfigurationManager.getProperty(
					CrisConstants.CFG_MODULE, "facet.type."
							+ crisObject.getTypeText().toLowerCase());
			fvalue = acvalue;
		}

		addResourceTypeIndex(document, acvalue, fvalue);
	}

	@Override
	public void additionalIndex(Context context, DSpaceObject dso,
			SolrInputDocument document) {

		String acvalue = ConfigurationManager.getProperty(
				CrisConstants.CFG_MODULE, "facet.type."
						+ dso.getTypeText().toLowerCase());
		String fvalue = acvalue;
		addResourceTypeIndex(document, acvalue, fvalue);

	}

	private void addResourceTypeIndex(SolrInputDocument document,
			String acvalue, String fvalue) {
		document.addField("resourcetype_ac", acvalue);

		Pattern pattern = Pattern.compile(REGEX);
		Matcher matcher = pattern.matcher(acvalue);
		if (matcher.matches()) {
			fvalue = matcher.group(1);
		}
		document.addField("resourcetype_filter", fvalue);
		document.addField("resourcetype_keyword", fvalue);
	}

	@Override
	public <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> void additionalIndex(
			ACNO dso, SolrInputDocument sorlDoc) {
		String acvalue = "";
		String fvalue = "";

		acvalue = ConfigurationManager.getProperty(CrisConstants.CFG_MODULE,
				"facet.type." + dso.getTypeText().toLowerCase());
		fvalue = acvalue;

		addResourceTypeIndex(sorlDoc, acvalue, fvalue);
	}

}
