package cz.cuni.mff.ufal.dspace.rest.common;

import org.dspace.handle.HandlePlugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
    

    public Handle(){

    }

    public Handle(String handle, String url, String title, String repository, String submitdate, String reportemail, String subprefix) {
        this.handle = handle;
        this.url = url;
        this.title = title;
        this.repository = repository;
        this.submitdate = submitdate;
        this.reportemail = reportemail;
        this.subprefix = subprefix;
    }

    public Handle(String handle, String magicURL){
        this.handle = handle;
        //similar to HandlePlugin
        String[] splits = magicURL.split(HandlePlugin.magicBean, 6);
        this.url = splits[splits.length - 1];
        this.title = splits[1];
        this.repository = splits[2];
        this.submitdate = splits[3];
        this.reportemail = splits[4];
        this.subprefix = handle.split("/",2)[1].split("-",2)[0];
    }

    @XmlElement
    public String getHandle() {
        return "http://hdl.handle.net/" + handle;
    }
}
