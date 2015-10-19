package cz.cuni.mff.ufal.dspace.rest.common;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by okosarko on 13.10.15.
 */
@XmlRootElement(name="handle")
public class Handle {

    @XmlElement
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
}
