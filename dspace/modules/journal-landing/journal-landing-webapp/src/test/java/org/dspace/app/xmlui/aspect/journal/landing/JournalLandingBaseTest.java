package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.datadryad.test.ContextUnitTest;
import org.dspace.app.xmlui.objectmanager.DSpaceObjectManager;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.WingDocument;
import org.jdom2.Namespace;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nathan Day
 */
public class JournalLandingBaseTest extends ContextUnitTest
{

    public static final Namespace driNs  = Namespace.getNamespace("dri", "http://di.tamu.edu/DRI/1.0/");
    public static final Namespace il8nNs = Namespace.getNamespace("il8n", "http://apache.org/cocoon/i18n/2.1");
    public static final Map<String, Object> vars = new HashMap<String, Object>();

    protected Request request;
    protected SourceResolver resolver;
    protected Map objectModel;
    protected Parameters parameters;
    protected String src;
    protected WingContext wingContext;
    protected WingDocument doc;

    @Before
    public void setUp() {
        super.setUp();
        request = mock(Request.class);
        objectModel = new HashMap();
        objectModel.put("request", request);
        parameters = new Parameters();
        resolver = mock(SourceResolver.class);
        src = "";
        when(request.getAttribute(ContextUtil.DSPACE_CONTEXT)).thenReturn(context);
        when(request.getContextPath()).thenReturn(null);
        wingContext = new WingContext();
        wingContext.setObjectManager(new DSpaceObjectManager());
        try {
            doc = new WingDocument(wingContext);
        } catch (WingException e) {
            e.printStackTrace();
        }
    }
}

