/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.util.List;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.license.factory.LicenseServiceFactory;
import org.dspace.license.service.CreativeCommonsService;
import org.dspace.xoai.util.ItemUtils;

/**
 * XOAIExtensionItemCompilePlugin aims to add structured information about the
 * creative commons license applied to the item (if any).
 * The xoai document will be enriched with a structure like that
 * <code>
 *   <element name="other">
 *       <element name="cc">
 *          <field name="uri"></field>
 *          <field name="name"></field>
 *       </element>
 *   </element>
 * </code>
 *
 */
public class CCElementItemCompilePlugin implements XOAIExtensionItemCompilePlugin {

    @Override
    public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
        CreativeCommonsService creativeCommonsService = LicenseServiceFactory.getInstance().getCreativeCommonsService();
        String licenseURI = creativeCommonsService.getLicenseURI(item);
        String licenseName = creativeCommonsService.getLicenseName(item);
        // licence uri is mandatory, name is optional
        if (StringUtils.isNotBlank(licenseURI)) {
            Element ccLicense = ItemUtils.create("cc");
            ccLicense.getField().add(ItemUtils.createValue("uri", licenseURI));
            if (StringUtils.isNotBlank(licenseName)) {
                ccLicense.getField().add(ItemUtils.createValue("name", licenseName));
            }
            Element other;
            List<Element> elements = metadata.getElement();
            if (ItemUtils.getElement(elements, "others") != null) {
                other = ItemUtils.getElement(elements, "others");
            } else {
                other = ItemUtils.create("others");
            }
            other.getElement().add(ccLicense);
        }
        return metadata;
    }

}
