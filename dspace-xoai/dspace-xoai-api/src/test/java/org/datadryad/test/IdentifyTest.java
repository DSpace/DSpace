
package org.datadryad.test;
import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import org.datadryad.test.DryadXoaiRegressionTest;
import org.dspace.core.ConfigurationManager;
import org.dspace.xoai.util.XOAICacheManager;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifyTest extends DryadXoaiRegressionTest
{
    public IdentifyTest() {
        super();
    }
    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testIdentify() throws IOException, JDOMException {
        this.initProvider("", "/xoai/request");

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("verb", Arrays.asList("Identify"));
        OAIRequestParameters requestParameters = new OAIRequestParameters(map);

        OutputStream out = new ByteArrayOutputStream();
        String id = "foobarbazquux";
        XOAICacheManager.handle(id, dataProvider, requestParameters, out);
        out.flush();
        out.close();

        SAXBuilder builder = new SAXBuilder();
        Document resultDoc = builder.build(new StringReader(out.toString()));

        File expectedFile = new File(this.getClass().getResource("/responses/Identify.xml").getFile());
        Document expectedDoc = builder.build(new FileReader(expectedFile));

        assert(compareDocument(resultDoc, expectedDoc) == true);
    }
}
