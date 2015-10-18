package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.submission.submit;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class DisplayShareLink  extends AbstractDSpaceTransformer{
    private static final Message T_title 				= message("Share link");
    private static final Message T_trail 				= message("Share link");
    private static final Message T_head 				= message("Share link");
    private static final Message T_para 				= message("To pass your submission to another user give them the following link: {0}");

    private static final Message T_DSPACE_HOME		= message("xmlui.general.dspace_home");

    private String link;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
                      Parameters parameters) throws ProcessingException, SAXException,
            IOException {
        super.setup(resolver, objectModel, src, parameters);
        this.link = parameters.getParameter("link", null);
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
        pageMeta.addTrailLink(null, T_trail);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {

        Division div = body.addDivision("share_submission_link");
        div.setHead(T_head);
        if(link != null){
            div.addPara(T_para.parameterize(link));
        }
        else{
            throw new AuthorizeException("Not authorized");
        }
    }
}
