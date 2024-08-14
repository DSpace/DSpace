/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app.plugins;

import java.sql.SQLException;
import java.util.List;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.apache.commons.lang3.StringUtils;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.xoai.app.XOAIExtensionItemCompilePlugin;
import org.dspace.xoai.util.ItemUtils;

/**
 * AccessStatusElementItemCompilePlugin aims to add structured information about the
 * Access Status of the item (if any).

 * The xoai document will be enriched with a structure like that
 * <pre>
 * {@code
 *   <element name="others">
 *       <element name="access-status">
 *          <field name="value">open.access</field>
 *       </element>
 *   </element>
 *   OR
 *   <element name="others">
 *       <element name="access-status">
 *          <field name="value">embargo</field>
 *          <field name="embargo">2024-10-10</field>
 *       </element>
 *   </element>
 * }
 * </pre>
 * Returning Values are based on:
 * @see org.dspace.access.status.DefaultAccessStatusHelper  DefaultAccessStatusHelper
 */
public class AccessStatusElementItemCompilePlugin implements XOAIExtensionItemCompilePlugin {

    @Override
    public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
        AccessStatusService accessStatusService = AccessStatusServiceFactory.getInstance().getAccessStatusService();

        try {
            String accessStatusType;
            accessStatusType = accessStatusService.getAccessStatus(context, item);

            String embargoFromItem = accessStatusService.getEmbargoFromItem(context, item);

            Element accessStatus = ItemUtils.create("access-status");
            accessStatus.getField().add(ItemUtils.createValue("value", accessStatusType));

            if (StringUtils.isNotEmpty(embargoFromItem)) {
                accessStatus.getField().add(ItemUtils.createValue("embargo", embargoFromItem));
            }

            Element others;
            List<Element> elements = metadata.getElement();
            if (ItemUtils.getElement(elements, "others") != null) {
                others = ItemUtils.getElement(elements, "others");
            } else {
                others = ItemUtils.create("others");
            }
            others.getElement().add(accessStatus);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return metadata;
    }

}
