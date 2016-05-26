/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.authority.ItemMetadataImportFillerConfiguration.MappingDetails;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import it.cilea.osd.jdyna.model.Property;

public class ItemMetadataImportFiller implements ImportAuthorityFiller {
	private static final Logger log = Logger.getLogger(ItemMetadataImportFiller.class);
	
	private boolean allowsUpdateByDefault = false;
	private Map<String, ItemMetadataImportFillerConfiguration> configurations;
	private ApplicationService applicationService;
	
	public void setApplicationService(ApplicationService applicationService) {
		this.applicationService = applicationService;
	}
	
	public void setConfigurations(Map<String, ItemMetadataImportFillerConfiguration> configurations) {
		this.configurations = configurations;
	}

	public void setAllowsUpdateByDefault(boolean allowsUpdateByDefault) {
		this.allowsUpdateByDefault = allowsUpdateByDefault;
	}
	
	@Override
	public boolean allowsUpdate(Context ctx, Item item, List<Metadatum> metadatumList, String authorityKey,
			ACrisObject rp) {
		if (metadatumList != null && metadatumList.size() > 0) {
			Metadatum m = metadatumList.get(0);
			String mdString = m.schema +"." + m.element +(m.qualifier != null?"."+m.qualifier:"");
			ItemMetadataImportFillerConfiguration conf = configurations.get(mdString);
			if (conf != null && conf.getUpdateEnabled() != null) {
				return conf.getUpdateEnabled();
			}
			else if (conf == null) {
				return false;
			}
		}
		return allowsUpdateByDefault;
	}
	
	@Override
	public void fillRecord(Context context, Item item, List<Metadatum> metadata, String hashedValue, ACrisObject crisObject) {
		int idx = 0;
		for (Metadatum m : metadata) {
			String mdString = m.schema +"." + m.element +(m.qualifier != null?"."+m.qualifier:"");
			log.debug("fillRecord -> "+ mdString);
			log.debug("fillRecord -> "+ crisObject.getAnagrafica4view());
			ItemMetadataImportFillerConfiguration conf = configurations.get(mdString);
			if (conf != null) {
				for (Entry<String, MappingDetails> entry : conf.getMapping().entrySet()) {
					String mdInput = entry.getKey();
					log.debug("fillRecord -> conf -> "+ mdInput);
					MappingDetails details = entry.getValue();
					log.debug("fillRecord -> conf -> "+ details.getShortName());
					List<? extends Property> props = (List<? extends Property>) crisObject.getAnagrafica4view().get(details.getShortName());
					String prefix = "";
					if (crisObject instanceof ResearchObject) {
						prefix = ((ResearchObject) crisObject).getTypo().getShortName();
					}
					Metadatum[] inputs = item.getMetadataByMetadataString(mdInput);
					if (details.isUseAll()) {
						for (Metadatum value : inputs) {
							if (value.authority != null) {
								ACrisObject entityByCrisId = applicationService.getEntityByCrisId(value.authority);
								if (!containsValue(props, entityByCrisId)) {
									ResearcherPageUtils.buildGenericValue(crisObject, entityByCrisId, prefix + details.getShortName(), details.getVisibility());
								}
							}
							else {
								if (!containsValue(props, value.value)) {
									ResearcherPageUtils.buildTextValue(crisObject, value.value, prefix + details.getShortName(), details.getVisibility());
								}
							}
						}
					}
					else {
						try {
							Metadatum value = inputs[idx];
							if (value.authority != null) {
								ACrisObject entityByCrisId = applicationService.getEntityByCrisId(value.authority);
								if (!containsValue(props, entityByCrisId)) {
									ResearcherPageUtils.buildGenericValue(crisObject, entityByCrisId, prefix + details.getShortName(), details.getVisibility());
								}
							}
							else {
								if (!containsValue(props, value.value)) {
									ResearcherPageUtils.buildTextValue(crisObject, value.value, prefix + details.getShortName(), details.getVisibility());
								}
							}
						}
						catch (ArrayIndexOutOfBoundsException ex) {
							log.error(LogManager.getHeader(context, "fillRecord", "missing "+mdInput+" for position "+idx+" in item "+item.getID()));
						}
					}
				}
				idx++;
			}
		}
	}
	
	private boolean containsValue(List<? extends Property> props, Object val) {
		for (Property p : props) {
			if (p.getValue().getReal().equals(val)) {
				return true;
			}
		}
		return false;
	}
}
