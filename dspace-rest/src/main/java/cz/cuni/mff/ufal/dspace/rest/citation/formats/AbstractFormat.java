package cz.cuni.mff.ufal.dspace.rest.citation.formats;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;

@XmlRootElement
public class AbstractFormat {
    @XmlTransient
    private String format;

    public AbstractFormat(){}

    public AbstractFormat(String format){
        this.format = format;
    }
    @XmlValue
    public String getValue() {
        return value;
    }

    public void setValue(String val){
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xp = xpf.newXPath();

            if(format.equals("bibtex")) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document document = db.parse(new InputSource(new StringReader(val)));
                String text = xp.evaluate("/*/text()", document);
                String[] parts = text.replaceAll(" +", " ").split("=");
                StringBuffer sb = new StringBuffer();
                sb.append(parts[0].replace(",", ",\n"));
                for(int i=1; i<parts.length-1; i++){
                    int lastComma = parts[i].lastIndexOf(',')+1;
                    sb.append('=').append(parts[i].substring(0, lastComma)).
                            append("\n").append(parts[i].substring(lastComma));
                }
                sb.append('=').append(parts[parts.length-1]);

                this.value = sb.toString();
            }
            else if (format.equals("html")){
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                //ugly hack
                //take the first &gt;
                int gtIdx = val.indexOf(">")+1;
                int ltIdx = val.lastIndexOf("<");
                StringBuffer sb = new StringBuffer();
                //we can get rid of  this when nobody is using the old citationbox, fix needs to be in oai export first
                sb.append(val.substring(0, gtIdx)).append("<![CDATA[").
                        append(val.substring(gtIdx, ltIdx).replaceAll(" xmlns[^> ]*","")).append("]]>").append(val.substring(ltIdx));
                Document document = db.parse(new InputSource(new StringReader(sb.toString())));
                String text = xp.evaluate("/*/text()",document);
                this.value = text;
            }
            else {
                this.value = val;
            }
        } catch (Exception e) {
            //
        }
    }

    private String value;


}
