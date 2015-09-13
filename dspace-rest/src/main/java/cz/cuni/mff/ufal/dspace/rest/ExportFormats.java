package cz.cuni.mff.ufal.dspace.rest;

import org.dspace.core.ConfigurationManager;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "exportFormats")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExportFormats {
    @XmlElement(name = "exportFormat")
    private List<ExportFormat> exportFormats;

    public ExportFormats(){
        exportFormats = new ArrayList<>();
    }

    public void add(String name, String url, String extract, String dataType){
        exportFormats.add(new ExportFormat(name, url, extract, dataType));
    }

    public static ExportFormats getFormats(String handle){
        ExportFormats formats = new ExportFormats();
        formats.add(Constants.bib_name.toString(),
                String.format(Constants.bib_url_format.toString(),handle),
                "",
                Constants.bib_data_type.toString());
        formats.add(Constants.cmdi_name.toString(),
                String.format(Constants.cmdi_url_format.toString(),handle),
                "",
                Constants.cmdi_data_type.toString());
        return formats;
    }
}

@XmlRootElement(name = "exportFormat")
class ExportFormat{
    public String name;
    public String url;
    public String extract;
    public String dataType;

    public ExportFormat() {}

    public ExportFormat(String name, String url, String extract, String dataType){
        this.name = name;
        this.url = url;
        this.extract = extract;
        this.dataType = dataType;
    }
}

enum Constants {
    bib_name("bibtex"),
    bib_url_format(ConfigurationManager.getProperty("dspace.baseUrl")
            + "/rest/handle/%s/citations/bibtex"),
    bib_data_type("json"),
    cmdi_name("cmdi"),
    cmdi_url_format(ConfigurationManager.getProperty("dspace.baseUrl")
            + "/rest/handle/%s/citations/cmdi"),
    cmdi_data_type("json"),
    bib_extract("function(bt){\n" +
            "bt = bt.replace(/  +/g, \" \");\n" +
            "var res = [];\n" +
            "var indent = 0;\n" +
            "for(var i=0;i<bt.length;i++) {\n" +
            "   if(bt[i] == '{') indent++;\n" +
            "   else\n" +
            "   if(bt[i] == '}') indent--;\n" +
            "   else\n" +
            "   if(bt[i] == '\\n') {\n" +
            "      for(var j=0;j<indent;j++) {\n" +
            "          res.push(\"\\t\");\n" +
            "      }\n" +
            "   }\n" +
            "   res.push(bt[i]);\n" +
            "}\n" +
            "return res.join(\"\")\n" +
            "}"),
    cmdi_extract("function (data) {\n" +
            "      return data[0].documentElement.outerHTML;\n" +
            "    }")
    ;

    private final String value;

    private Constants(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
