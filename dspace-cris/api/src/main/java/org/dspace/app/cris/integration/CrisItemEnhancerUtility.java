/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.utils.DSpace;

public class CrisItemEnhancerUtility {
	private static final Logger log = Logger.getLogger(CrisItemEnhancerUtility.class);

	public static List<String> getAllCrisMetadata() {
		DSpace dspace = new DSpace();
		List<String> result = new ArrayList<String>();
		List<CrisItemEnhancer> enhancers = dspace.getServiceManager().getServicesByType(CrisItemEnhancer.class);
		if (enhancers != null) {
			for (CrisItemEnhancer enhancer : enhancers) {
				for (String qual : enhancer.getQualifiers2path().keySet()) {
					result.add("crisitem." + enhancer.getAlias() + "." + qual);
				}
			}
		}
		return result;
	}

	public static List<Metadatum> getCrisMetadata(Item item, String metadata) {
		StringTokenizer dcf = new StringTokenizer(metadata, ".");

		String[] tokens = { "", "", "" };
		int i = 0;
		while (dcf.hasMoreTokens()) {
			tokens[i] = dcf.nextToken().trim();
			i++;
		}
		String schema = tokens[0];
		String element = tokens[1];
		String qualifier = tokens[2];

		if (!"crisitem".equals(schema)) {
			return null;
		}
		if (element == null) {
			log.error("Wrong configuration asked for crisitem metadata with null element");
		}

		List<CrisItemEnhancer> enhancers = getEnhancers(element);
		List<Metadatum> result = new ArrayList<Metadatum>();
		if (Item.ANY.equals(qualifier)) {
			for (CrisItemEnhancer enh : enhancers) {
				Set<String> qualifiers = enh.getQualifiers2path().keySet();
				for (String qual : qualifiers) {
					List<String[]> vals = getCrisMetadata(item, enh, qual);
					for (String[] e : vals) {
						Metadatum dc = new Metadatum();
						dc.schema = "crisitem";
						dc.element = enh.getAlias();
						dc.qualifier = qual;
						dc.value = e[0];
						if (StringUtils.isNotBlank(dc.value)) {
							dc.authority = e[1];
							dc.confidence = StringUtils.isNotEmpty(e[1]) ? Choices.CF_ACCEPTED : Choices.CF_UNSET;
							result.add(dc);
						}
					}
				}
			}

		} else if ("".equals(qualifier)) {
			if (qualifier == null) {
				log.error("Wrong configuration asked for unqualified crisitem." + element
						+ " metadata. All crisitem metadata MUST BE qualified");
			}
			return null;
		} else {
			for (CrisItemEnhancer enh : enhancers) {
				List<String[]> vals = getCrisMetadata(item, enh, qualifier);
				for (String[] e : vals) {
					Metadatum dc = new Metadatum();
					dc.schema = "crisitem";
					dc.element = enh.getAlias();
					dc.qualifier = qualifier;
					dc.value = e[0];
					if (StringUtils.isNotBlank(dc.value)) {
						dc.authority = e[1];
						dc.confidence = StringUtils.isNotEmpty(e[1]) ? Choices.CF_ACCEPTED : Choices.CF_UNSET;
						result.add(dc);
					}
				}
			}
		}
		return result;
	}

	private static List<String[]> getCrisMetadata(Item item, CrisItemEnhancer enh, String qualifier) {
		List<String> mdList = enh.getMetadata();
		List<String> validAuthorities = new ArrayList<String>();
		MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();

		for (String md : mdList) {
			Metadatum[] Metadatums = item.getMetadataByMetadataString(md);
			for (Metadatum dc : Metadatums) {
				try {
					ACrisObject newInstance = enh.getClazz().newInstance();
					if (dc.authority != null
							&& ((newInstance instanceof ResearchObject) ? dc.authority.startsWith(enh.getType())
									: dc.authority.startsWith(newInstance.getAuthorityPrefix()))) {
						if (mam.getMinConfidence(dc.schema, dc.element, dc.qualifier) <= dc.confidence) {
							validAuthorities.add(dc.authority);
							continue;
						}
					}
					// force the placeholder to assure at least one value for linked cris object
					validAuthorities.add(MetadataValue.PARENT_PLACEHOLDER_VALUE);
				} catch (InstantiationException e) {
					log.error(e.getMessage(), e);
				} catch (IllegalAccessException e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		List<String[]> result = new ArrayList<String[]>();
		if (validAuthorities.size() > 0) {
			DSpace dspace = new DSpace();
			String path = enh.getQualifiers2path().get(qualifier);
			if (path == null) {
				return result;
			}
			ApplicationService as = dspace.getServiceManager().getServiceByName("applicationService",
					ApplicationService.class);
			for (String authKey : validAuthorities) {
				if (MetadataValue.PARENT_PLACEHOLDER_VALUE.equals(authKey)) {
					result.add(new String[] {MetadataValue.PARENT_PLACEHOLDER_VALUE, null});
				}
				else {
					result.addAll(
							getPathAsMetadata(as,
									as.get(enh.getClazz(),
											ResearcherPageUtils.getRealPersistentIdentifier(authKey, enh.getClazz())),
							path));
				}
			}

		}
		return result;
	}

	private static <P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>> List<String[]> getPathAsMetadata(
			ApplicationService as, ACrisObject<P, TP, NP, NTP, ACNO, ATNO> aCrisObject, String path) {

		String[] splitted = path.split("\\.", 2);
		List<String[]> result = new ArrayList<String[]>();
		if (aCrisObject != null) {
			List<P> props = aCrisObject.getAnagrafica4view().get(splitted[0]);
			if (splitted.length == 2) {
				for (P prop : props) {
					if (prop.getObject() instanceof ACrisObject) {
						result.addAll(getPathAsMetadata(as, (ACrisObject) prop.getObject(), splitted[1]));
					} else {
						log.error(
								"Wrong configuration, asked for path " + splitted[1] + " on a not CRIS Object value.");
					}
				}

			}
			else {
				if (props.size() > 0) {
					for (P prop : props) {
						if (prop.getObject() instanceof ACrisObject) {
							ACrisObject val = (ACrisObject) prop.getObject();
							result.add(new String[] { val.getName(), ResearcherPageUtils.getPersistentIdentifier(val) });
						} else {
							PropertyEditor editor = prop.getTypo().getRendering().getPropertyEditor(as);
							editor.setValue(prop.getObject());
		
							result.add(new String[] { editor.getAsText(), null });
						}
					}
				}
				else {
					// force the placeholder to assure at least one value for linked cris object
					result.add(new String[] { MetadataValue.PARENT_PLACEHOLDER_VALUE, null });
				}
			}
		}
		return result;
	}

	private static List<CrisItemEnhancer> getEnhancers(String alias) {
		DSpace dspace = new DSpace();
		List<CrisItemEnhancer> enhancers = dspace.getServiceManager().getServicesByType(CrisItemEnhancer.class);
		if (Item.ANY.equals(alias)) {
			return enhancers;
		}
		List<CrisItemEnhancer> result = new ArrayList<CrisItemEnhancer>();
		for (CrisItemEnhancer enhancer : enhancers) {
			if (enhancer.getAlias().equals(alias)) {
				result.add(enhancer);
			}
		}
		return result;
	}
}
