/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.generator.TemplateValueGenerator;
import org.dspace.core.Context;

public class DSpaceTemplateItemService implements TemplateItemService {
	private Map<String, TemplateValueGenerator> generators;
	
	public void setGenerators(Map<String, TemplateValueGenerator> generators) {
		this.generators = generators;
	}

	@Override
	public void applyTemplate(Context context, Item targetItem, Item templateItem) {
        Metadatum[] md = templateItem.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        for (int n = 0; n < md.length; n++)
        {
        	// replace ###SPECIAL-PLACEHOLDER### with the actual value, where the SPECIAL-PLACEHOLDER can be one of
        	// NOW.YYYY-MM-DD SUBMITTER RESEARCHER CURRENTUSER.fullname / email / phone
            if (StringUtils.startsWith(md[n].value, "###") 
            		&& StringUtils.endsWith(md[n].value, "###")) {
            	String[] splitted = md[n].value.substring(3, md[n].value.length()-3).split("\\.", 2);
            	TemplateValueGenerator gen = generators.get(splitted[0]);
            	if (gen != null) {
	            	String extraParams = null;
            		if (splitted.length == 2) {
	            		extraParams = splitted[1];
	            	}
            		Metadatum[] genMetadata = gen.generator(context, targetItem, templateItem, md[n], extraParams);
            		for (Metadatum gm : genMetadata) {
            			targetItem.addMetadata(gm.schema, gm.element, gm.qualifier, gm.language,
                                gm.value, gm.authority, gm.confidence);
            		}
	            	continue;
            	}
            }
        	targetItem.addMetadata(md[n].schema, md[n].element, md[n].qualifier, md[n].language,
                    md[n].value, md[n].authority, md[n].confidence);
        }
	}
	
    public void clearTemplate(Context context, Item targetItem, Item templateItem)
    {
        Metadatum md[] = templateItem.getMetadata("*", "*", "*", "*");
        for(int n = 0; n < md.length; n++) {
            targetItem.clearMetadata(md[n].schema, md[n].element, md[n].qualifier, "*");
        }

    }
}
