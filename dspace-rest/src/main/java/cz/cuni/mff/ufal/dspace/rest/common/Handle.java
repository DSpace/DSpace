package cz.cuni.mff.ufal.dspace.rest.common;

import org.dspace.handle.HandlePlugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Created by okosarko on 13.10.15.
 */
@XmlRootElement(name="handle")
@XmlAccessorType(XmlAccessType.NONE)
public class Handle {

    public static final String HANDLE_URL = "http://hdl.handle.net/";
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
    @XmlElement
    public String token;


    public Handle(){

    }

    public Handle(String handle, String url, String title, String repository, String submitdate, String reportemail, String datasetName, String datasetVersion, String query, String token, String subprefix) {
        this.handle = handle;
        this.url = url;
        this.title = title;
        this.repository = repository;
        this.submitdate = submitdate;
        this.reportemail = reportemail;
        this.datasetName = datasetName;
        this.datasetVersion = datasetVersion;
        this.query = query;
        this.token = token;
        this.subprefix = subprefix;
    }

    public Handle(String handle, String magicURL){
        this.handle = handle;
        //similar to HandlePlugin
        String[] splits = magicURL.split(HandlePlugin.magicBean,10);
        this.url = splits[splits.length - 1];
        this.title = splits[1];
        this.repository = splits[2];
        this.submitdate = splits[3];
        this.reportemail = splits[4];
        if(isNotBlank(splits[5])) {
            this.datasetName = splits[5];
        }
        if(isNotBlank(splits[6])) {
            this.datasetVersion = splits[6];
        }
        if(isNotBlank(splits[7])) {
            this.query = splits[7];
        }
        if(isNotBlank(splits[8])){
            this.token = splits[8];
        }
        this.subprefix = handle.split("/",2)[1].split("-",2)[0];
    }

    public String getMagicUrl(){
        return Handle.getMagicUrl(this.title, this.submitdate, this.reportemail, this.datasetName, this.datasetVersion, this.query, this.url);
    }

    public static String getMagicUrl(String title, String submitdate, String reportemail, String datasetName, String datasetVersion, String query, String url){
        String magicURL = "";
        String token = UUID.randomUUID().toString();
        for (String part : new String[]{title, HandlePlugin.repositoryName, submitdate, reportemail, datasetName, datasetVersion, query, token, url}){
            if(isBlank(part)){
                //optional dataset etc...
                part = "";
            }
            magicURL += HandlePlugin.magicBean + part;
        }
        return magicURL;
    }

    @XmlElement
    public String getHandle() {
        return HANDLE_URL + handle;
    }

    public void setHandle(String handle){
        this.handle = handle.replace(Handle.HANDLE_URL,"");
    }
}
