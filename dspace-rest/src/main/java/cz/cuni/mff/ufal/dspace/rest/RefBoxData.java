package cz.cuni.mff.ufal.dspace.rest;

import cz.cuni.mff.ufal.dspace.rest.citation.formats.AbstractFormat;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class RefBoxData {
    private String title;
    private ExportFormats exportFormats;
    private AbstractFormat displayText;
    private FeaturedServices featuredServices;

    public RefBoxData(){}

    @XmlElement(required = true)
    public String getTitle() {
        return title;
    }

    @XmlElement(required = true)
    public FeaturedServices getFeaturedServices() {
        return featuredServices;
    }

    @XmlElement(required = true)
    public AbstractFormat getDisplayText() {
        return displayText;
    }

    @XmlElement(required = true)
    public ExportFormats getExportFormats() {
        return exportFormats;
    }

    public void setTitle(String title){
        this.title = title;
    }
    public void setExportFormats(ExportFormats exportFormats){
        this.exportFormats = exportFormats;
    }
    public void setDisplayText(AbstractFormat displayText){
        this.displayText = displayText;
    }
    public void setFeaturedServices(FeaturedServices featuredServices){
        this.featuredServices = featuredServices;
    }
}
