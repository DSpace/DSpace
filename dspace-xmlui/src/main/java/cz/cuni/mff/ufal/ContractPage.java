/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import org.apache.cocoon.ProcessingException;
import org.apache.xalan.xsltc.util.IntegerArray;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.I18nUtil;
import org.dspace.core.LicenseManager;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ContractPage extends AbstractDSpaceTransformer {
	
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.ContractPage.title");
    private static final Message T_trail = message("xmlui.ContractPage.trail");
    private static final Message T_head = message("xmlui.ContractPage.head");
    private static final Message T_collection_name = message("xmlui.ContractPage.collection_name");
    private static final Message T_explain_non_localized_license = message("xmlui.ContractPage.explain_non_localized_license");

    private static final String alternativeLicenseText;
    static{
        String alternativePath = ConfigurationManager.getProperty("lr","license.alternative.path");
        if(isNotBlank(alternativePath)){
            alternativeLicenseText = LicenseManager.getLicenseText(alternativePath);
        }else{
            alternativeLicenseText = null;
        }
    }


	
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
    	pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
    	Division division = body.addDivision("licenses", "well");
    	
    	division.setHead(T_head);

        boolean showExplanation = ConfigurationManager.getBooleanProperty("lr","license.show_localized_explanation", true);

        if(!context.getCurrentLocale().equals(I18nUtil.getDefaultLocale()) && showExplanation ){
            division.addPara().addContent(T_explain_non_localized_license);
            if(isNotBlank(alternativeLicenseText)){
                addLicense(division, "default_license_alternative", T_collection_name.parameterize("Default license"),
                        alternativeLicenseText);
            }
        }

		for(Collection col : Collection.findAll(context)){
            addLicense(division, Integer.toString(col.getID()), T_collection_name.parameterize(col.getName()),  col.getLicense());
		}
    }

    private void addLicense(Division division, String id, Message head, String text) throws WingException {
        Division license = division.addDivision("license_" + id, "well well-sm well-white");
        license.setHead(head);
        TextArea textArea = license.addPara().addTextArea("license_text_" + id);
        textArea.setSize(34,0); //cols are overridden by .form-control
        textArea.setValue(text);
        textArea.setDisabled();
    }

}


