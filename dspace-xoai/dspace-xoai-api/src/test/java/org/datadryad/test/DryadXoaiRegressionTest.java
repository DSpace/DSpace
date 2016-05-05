
package org.datadryad.test;

import com.lyncode.xoai.dataprovider.OAIDataProvider;
import com.lyncode.xoai.dataprovider.exceptions.InvalidContextException;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.xoai.DSpaceOAIDataProvider;
import org.dspace.xoai.data.DSpaceIdentify;
import org.dspace.xoai.data.DSpaceItemRepository;
import org.dspace.xoai.data.DSpaceItemSolrRepository;
import org.dspace.xoai.data.DSpaceSetRepository;
import org.dspace.xoai.solr.DSpaceSolrServer;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.After;
import org.junit.Before;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class DryadXoaiRegressionTest extends ContextSolrUnitTest
{

    protected DryadXoaiRegressionTest() {
        super();
    }

    protected OAIDataProvider dataProvider;

    @Before
    public void setUp() {
        super.setUp();
    }

    protected void initProvider(String requestURL, String pathInfo) {
        // XOAI request context, e.g., from "/xoai/request?verb=GetRecord&..."
        String contextUrl = "request";

        // static init for mocking an xoai environment
        DSpaceSolrServer.setServer(mockSolrStatsServer);
        DSpaceOAIDataProvider provider = new DSpaceOAIDataProvider();
        provider.init();

        // initialize a mock Request object
        StringBuffer sb = new StringBuffer(requestURL);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ContextUtil.DSPACE_CONTEXT)).thenReturn(context);
        when(request.getContextPath()).thenReturn(null);
        when(request.getRequestURL()).thenReturn(sb);
        when(request.getPathInfo()).thenReturn(pathInfo);

        DSpaceIdentify identify = new DSpaceIdentify(context, request);
        DSpaceSetRepository setRepository = new DSpaceSetRepository(context);
        DSpaceItemRepository itemRepository = new DSpaceItemSolrRepository();
        try {
            dataProvider = new OAIDataProvider(contextUrl, identify, setRepository, itemRepository);
        } catch (InvalidContextException e) {
            e.printStackTrace();
        }
    }
    @After
    public void tearDown() {
        super.tearDown();
    }

    protected boolean compareDocument(Document d1, Document d2) {
        return compareElement(d1.getRootElement(), d2.getRootElement());
    }

    // regex to match whitespace-only text nodes, for skipping
    private static final String textFind = "^[ \t]*[\r\n]+[ \t]*$";

    private Content forward(Iterator<Content> c) {
        if (!c.hasNext())
            return null;
        Content next = c.next();
        if(next.getValue().toString().matches(textFind))
            return forward(c);
        if (skipContent(next))
            return forward(c);
        return next;
    }

    protected boolean compareElement(Element e1, Element e2) {
        if (   e1.getName().equals(e2.getName())
            && e1.getNamespace().getURI().toString().equals(e2.getNamespace().getURI().toString()))
        {
            if (!compareAttributes(e1.getAttributes(), e2.getAttributes()))
                throw new RuntimeException("Attribute mismatch: " + e1.getAttributes().toString() + " ||| " + e2.getAttributes().toString());
            Iterator<Content> i1 = e1.getContent().iterator();
            Iterator<Content> i2 = e2.getContent().iterator();
            while(true) {
                Content c3 = forward(i1);
                Content c4 = forward(i2);
                if (!compareContent(c3, c4))
                    throw new RuntimeException("No match: " + c3.toString() + " ||| " + c4.toString());
                if (!i1.hasNext() && !i2.hasNext())
                    return true;
            }
        } else {
            throw new RuntimeException("No match: " + e1.toString() + " ||| " + e2.toString());
        }
    }

    protected boolean compareAttributes(List<Attribute> as1, List<Attribute> as2) {
        if (as1.size() == 0 && as2.size() == 0)
            return true;
        if (as1.size() != as2.size())
            return false;
        List<String> ss1 = new ArrayList<String>();
        List<String> ss2 = new ArrayList<String>();
        for (Attribute a : as1)
            ss1.add(a.toString());
        for (Attribute a : as2)
            ss2.add(a.toString());
        return ss1.containsAll(ss2) && ss2.containsAll(ss1);
    }

    protected boolean compareContent(Content c1, Content c2) {
        if (c1 == null && c2 == null) return true;
        if (c1 == null || c2 == null) return false;
        if (!c1.getCType().equals(c2.getCType())) return false;
        if (c1.equals(c2)) return true;
        switch (c1.getCType()) {
            case Text:
                return c1.getValue().toString().equals(c2.getValue().toString());
            case Comment: case DocType: case EntityRef: case ProcessingInstruction:
                return c1.getValue().toString().equals(c2.getValue().toString());
            case Element:
                return compareElement((Element) c1, (Element) c2);
        }
        // unreachable
        throw new RuntimeException("No match: " + c1.toString() + " ||| " + c2.toString());
    }

    protected boolean skipContent(Content c) {
        if (c instanceof Element) {
            Element e = (Element) c;
            String name = e.getName().toString();
            if (  name.equals("responseDate") || name.equals("datestamp") || name.equals("request")
               || name.equals("baseURL") || name.equals("earliestDatestamp") || name.equals("date"))
                return true;
        }
        return false;
    }
}
