package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.submission.submit;


import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class OwnerChangeForm extends AbstractDSpaceTransformer {

    private static final Message T_title 				= message("Change submitter");
    private static final Message T_trail 				= message("Change submitter");
    private static final Message T_head 				= message("Change submitter");

    private static final Message T_DSPACE_HOME		= message("xmlui.general.dspace_home");
    protected ConfigurationService configurationService;

    private int id;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
                      Parameters parameters) throws ProcessingException, SAXException,
            IOException {
        super.setup(resolver, objectModel, src, parameters);
        DSpace dspace = new DSpace();
        configurationService = dspace.getConfigurationService();
        this.id = parameters.getParameterAsInteger("id", -1);
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
        pageMeta.addTrailLink(null, T_trail);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        Division div = body.addInteractiveDivision("take_item", contextPath + "/submit/" + knot.getId() + ".continue", Division.METHOD_POST, "primary administrative");
        div.setHead(T_head);
        Context context = ContextUtil.obtainContext(objectModel);
        EPerson eperson = context.getCurrentUser();
        if(eperson == null || eperson.getID() < 1){
            throw new AuthorizeException("You are not authorized to view this page.");
        }

        if(id != -1){
            WorkspaceItem wi = WorkspaceItem.find(context, id);
            Item item = wi.getItem();
            Request request = ObjectModelHelper.getRequest(objectModel);
            String paramToken = request.getParameter("share_token");
            if(paramToken == null || paramToken.equals("") || !wi.getShareToken().equals(paramToken)){
                throw new AuthorizeException("Invalid token.");
            }
            EPerson sub = item.getSubmitter();
            div.addPara(String.format("This items currently belongs to %s (%s)", sub.getFullName(), sub.getEmail()));
            div.addList("buttons-list").addItem().addButton("submit_own").setValue("Take It");
            div.addHidden("submission-continue").setValue(knot.getId());
        }

    }
}