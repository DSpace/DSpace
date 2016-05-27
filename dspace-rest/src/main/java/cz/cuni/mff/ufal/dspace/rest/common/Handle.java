package cz.cuni.mff.ufal.dspace.rest.common;

import org.dspace.handle.HandlePlugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Created by okosarko on 13.10.15.
 */
@XmlRootElement(name="handle")
@XmlAccessorType(XmlAccessType.NONE)
public class Handle {

    public String handle;
    @XmlElement
    public String url;
    @XmlElement
    public String title;
    @XmlElement
    public String repository;
    @XmlElement
    public String submitdate;
    @XmlElement
    public String reportemail;
    @XmlElement
    public String subprefix;
    @XmlElement
    public String datasetName;
    @XmlElement
    public String datasetVersion;
    @XmlElement
    public String query;


    public Handle(){

    }

    public Handle(String handle, String url, String title, String repository, String submitdate, String reportemail, String datasetName, String datasetVersion, String query, String subprefix) {
        this.handle = handle;
        this.url = url;
        this.title = title;
        this.repository = repository;
        this.submitdate = submitdate;
        this.reportemail = reportemail;
        this.datasetName = datasetName;
        this.datasetVersion = datasetVersion;
        this.query = query;
        this.subprefix = subprefix;
    }

    public Handle(String handle, String magicURL){
        this.handle = handle;
        //similar to HandlePlugin
        String[] splits = magicURL.split(HandlePlugin.magicBean);
        this.url = splits[splits.length - 1];
        this.title = splits[1];
        this.repository = splits[2];
        this.submitdate = splits[3];
        this.reportemail = splits[4];
        if(splits.length == 9){
            if(isNotBlank(splits[5])) {
                this.datasetName = splits[5];
            }
            if(isNotBlank(splits[6])) {
                this.datasetVersion = splits[6];
            }
            if(isNotBlank(splits[7])) {
                this.query = splits[7];
            }
        }
        this.subprefix = handle.split("/",2)[1].split("-",2)[0];
    }

    @XmlElement
    public String getHandle() {
        return "http://hdl.handle.net/" + handle;
    }
}
