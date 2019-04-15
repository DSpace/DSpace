package org.dspace.license;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.Test;

import java.io.FileInputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class CC4LocaleConverterTest {

    @Test
    public void cc3ConvertTest() throws Exception {
        String cc3_path = "src/test/resources/cc3license.xml";

        SAXBuilder saxBuilder = new SAXBuilder();
        Document expectedDocument = saxBuilder.build(new FileInputStream(cc3_path));
        String expectedString = new XMLOutputter().outputString(expectedDocument);

        Document resultDocument = CC4LocaleConverter.builder(new FileInputStream(cc3_path)).build("no");
        String resultString = new XMLOutputter().outputString(resultDocument);

        assertThat("CC3 license not modified", resultString, equalTo(expectedString));
    }

    @Test
    public void cc4ConvertTest() throws Exception {
        String cc4_path = "src/test/resources/cc4license.xml";

        Document resultDocument = CC4LocaleConverter.builder(new FileInputStream(cc4_path)).build("no");
        String resultString = new XMLOutputter().outputString(resultDocument);

        assertThat("CC4 license modified", StringUtils.countMatches(resultString, "http://creativecommons.org/licenses/by/4.0/deed.no"), is(6));
    }

    @Test
    public void cc4ConvertTestDefaultLocale() throws Exception {
        String cc4_path = "src/test/resources/cc4license.xml";

        Document resultDocument = CC4LocaleConverter.builder(new FileInputStream(cc4_path)).build("");
        String resultString = new XMLOutputter().outputString(resultDocument);

        assertThat("CC4 license modified", StringUtils.countMatches(resultString, "http://creativecommons.org/licenses/by/4.0/deed.en"), is(6));
    }
}