/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.xoai.util.ItemUtils;


/**
 * XOAIExtensionItemCompilePlugin to calculate access right to the item
 * according to the COAR vocabulary / OpenAIRE requirement <code>
 *   <element name="other">
 *       <element name="accesscondition">
 *          <field name="coarURI"></field>
 *          <field name="euTermURI"></field>
 *          <field name="name"></field>
 *          <field name="embargoEndDate"></field>
 *       </element>
 *   </element>
 * </code>
 *
 */
public class FulltextAccessElementItemCompilePlugin implements XOAIExtensionItemCompilePlugin {

    @Override
    public Metadata additionalMetadata(Context context, Metadata metadata, Item item) {
        ResourcePolicyService rpServ = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        GroupService gServ = EPersonServiceFactory.getInstance().getGroupService();
        Group anonGroup;
        String coarURI = "http://purl.org/coar/access_right/c_14cb";
        String euTermURI = "info:eu-repo/semantics/closedAccess";
        String accessName = "metadata only access";
        Date embargoEndDate = null;
        try {
            anonGroup = gServ.findByName(context, Group.ANONYMOUS);
            List<Bundle> bnds = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
            Date now = new Date();

            main: for (Bundle bnd : bnds) {
                for (Bitstream b : bnd.getBitstreams()) {
                    for (ResourcePolicy rp : rpServ.findByResouceUuidAndActionId(context, b.getID(), Constants.READ, 0,
                        Integer.MAX_VALUE)) {
                        if (rp.getGroup() != null && rp.getGroup().getID().equals(anonGroup.getID())) {
                            // we found an embargo or an openaccess bitstream
                            // exclude temporary access from computation
                            if (rp.getEndDate() == null) {
                                if (rp.getStartDate() == null || rp.getStartDate().before(now)) {
                                    embargoEndDate = null;
                                    coarURI = "http://purl.org/coar/access_right/c_abf2";
                                    euTermURI = "info:eu-repo/semantics/openAccess";
                                    accessName = "open access";
                                    break main;
                                } else {
                                    // keep the first embargo date
                                    coarURI = "http://purl.org/coar/access_right/c_f1cf";
                                    euTermURI = "info:eu-repo/semantics/embargoedAccess";
                                    accessName = "embargoed access";
                                    embargoEndDate = embargoEndDate == null ? rp.getEndDate()
                                        : (embargoEndDate.after(rp.getEndDate()) ? rp.getEndDate() : embargoEndDate);
                                }
                            }
                        } else if (embargoEndDate == null) {
                            // only set to restricted if we have not yet found an embargo
                            coarURI = "http://purl.org/coar/access_right/c_f1cf";
                            accessName = "embargoed access";
                            euTermURI = "info:eu-repo/semantics/restrictedAccess";
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Element ccLicense = ItemUtils.create("accesscondition");
        ccLicense.getField().add(ItemUtils.createValue("coarURI", coarURI));
        ccLicense.getField().add(ItemUtils.createValue("euTermURI", euTermURI));
        ccLicense.getField().add(ItemUtils.createValue("name", accessName));
        if (embargoEndDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            ccLicense.getField().add(ItemUtils.createValue("embargoEndDate", sdf.format(embargoEndDate)));
        }
        Element other;
        List<Element> elements = metadata.getElement();
        if (ItemUtils.getElement(elements, "others") != null) {
            other = ItemUtils.getElement(elements, "others");
        } else {
            other = ItemUtils.create("others");
        }
        other.getElement().add(ccLicense);
        return metadata;
    }

}
