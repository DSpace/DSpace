/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.extraction.grobid;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.global" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;sequence minOccurs="0"&gt;
 *           &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.divTop"/&gt;
 *           &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *             &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.global"/&gt;
 *             &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.divTop"/&gt;
 *           &lt;/choice&gt;
 *         &lt;/sequence&gt;
 *         &lt;choice&gt;
 *           &lt;sequence maxOccurs="unbounded"&gt;
 *             &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.divLike"/&gt;
 *             &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.global" maxOccurs="unbounded" minOccurs="0"/&gt;
 *           &lt;/sequence&gt;
 *           &lt;sequence&gt;
 *             &lt;sequence maxOccurs="unbounded"&gt;
 *               &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.common"/&gt;
 *               &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.global" maxOccurs="unbounded" minOccurs="0"/&gt;
 *             &lt;/sequence&gt;
 *             &lt;sequence maxOccurs="unbounded" minOccurs="0"&gt;
 *               &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.divLike"/&gt;
 *               &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.global" maxOccurs="unbounded" minOccurs="0"/&gt;
 *             &lt;/sequence&gt;
 *           &lt;/sequence&gt;
 *         &lt;/choice&gt;
 *         &lt;sequence maxOccurs="unbounded" minOccurs="0"&gt;
 *           &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.divBottom"/&gt;
 *           &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.global" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;/sequence&gt;
 *       &lt;/sequence&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.global.attributes"/&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.declaring.attributes"/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "linksAndAnchorsAndNotes"
})
@XmlRootElement(name = "body")
public class Body {

    @XmlElements( {
        @XmlElement(name = "link", type = Link.class),
        @XmlElement(name = "anchor", type = Anchor.class),
        @XmlElement(name = "note", type = Note.class),
        @XmlElement(name = "figure", type = Figure.class),
        @XmlElement(name = "meeting", type = Meeting.class),
        @XmlElement(name = "head", type = Head.class),
        @XmlElement(name = "div", type = Div.class),
        @XmlElement(name = "p", type = P.class),
        @XmlElement(name = "trash", type = Trash.class),
        @XmlElement(name = "bibl", type = Bibl.class),
        @XmlElement(name = "biblStruct", type = BiblStruct.class),
        @XmlElement(name = "listBibl", type = ListBibl.class),
        @XmlElement(name = "label", type = Label.class),
        @XmlElement(name = "list", type = org.dspace.submit.extraction.grobid.List.class),
        @XmlElement(name = "table", type = Table.class)
    })
    protected java.util.List<Object> linksAndAnchorsAndNotes;
    @XmlAttribute(name = "coords")
    @XmlSchemaType(name = "anySimpleType")
    protected String coords;
    @XmlAttribute(name = "n")
    protected String n;
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;
    @XmlAttribute(name = "rendition")
    protected java.util.List<String> renditions;
    @XmlAttribute(name = "style")
    protected String style;
    @XmlAttribute(name = "rend")
    protected java.util.List<String> rends;
    @XmlAttribute(name = "id", namespace = "http://www.w3.org/XML/1998/namespace")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "base", namespace = "http://www.w3.org/XML/1998/namespace")
    protected String base;
    @XmlAttribute(name = "resp")
    protected java.util.List<String> resps;
    @XmlAttribute(name = "cert")
    protected TeiDataCertainty cert;
    @XmlAttribute(name = "select")
    protected java.util.List<String> selects;
    @XmlAttribute(name = "corresp")
    protected java.util.List<String> corresps;
    @XmlAttribute(name = "sameAs")
    protected String sameAs;
    @XmlAttribute(name = "exclude")
    protected java.util.List<String> excludes;
    @XmlAttribute(name = "copyOf")
    protected String copyOf;
    @XmlAttribute(name = "prev")
    protected String prev;
    @XmlAttribute(name = "synch")
    protected java.util.List<String> synches;
    @XmlAttribute(name = "next")
    protected String next;
    @XmlAttribute(name = "space", namespace = "http://www.w3.org/XML/1998/namespace")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String space;
    @XmlAttribute(name = "decls")
    protected java.util.List<String> decls;

    /**
     * Gets the value of the linksAndAnchorsAndNotes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the linksAndAnchorsAndNotes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLinksAndAnchorsAndNotes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Link }
     * {@link Anchor }
     * {@link Note }
     * {@link Figure }
     * {@link Meeting }
     * {@link Head }
     * {@link Div }
     * {@link P }
     * {@link Trash }
     * {@link Bibl }
     * {@link BiblStruct }
     * {@link ListBibl }
     * {@link Label }
     * {@link org.dspace.submit.extraction.grobid.List }
     * {@link Table }
     */
    public java.util.List<Object> getLinksAndAnchorsAndNotes() {
        if (linksAndAnchorsAndNotes == null) {
            linksAndAnchorsAndNotes = new ArrayList<Object>();
        }
        return this.linksAndAnchorsAndNotes;
    }

    /**
     * Gets the value of the coords property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCoords() {
        return coords;
    }

    /**
     * Sets the value of the coords property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCoords(String value) {
        this.coords = value;
    }

    /**
     * Gets the value of the n property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getN() {
        return n;
    }

    /**
     * Sets the value of the n property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setN(String value) {
        this.n = value;
    }

    /**
     * Gets the value of the lang property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLang() {
        return lang;
    }

    /**
     * Sets the value of the lang property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLang(String value) {
        this.lang = value;
    }

    /**
     * Gets the value of the renditions property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the renditions property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRenditions().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getRenditions() {
        if (renditions == null) {
            renditions = new ArrayList<String>();
        }
        return this.renditions;
    }

    /**
     * Gets the value of the style property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getStyle() {
        return style;
    }

    /**
     * Sets the value of the style property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setStyle(String value) {
        this.style = value;
    }

    /**
     * Gets the value of the rends property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rends property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRends().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getRends() {
        if (rends == null) {
            rends = new ArrayList<String>();
        }
        return this.rends;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the base property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getBase() {
        return base;
    }

    /**
     * Sets the value of the base property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setBase(String value) {
        this.base = value;
    }

    /**
     * Gets the value of the resps property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the resps property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getResps().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getResps() {
        if (resps == null) {
            resps = new ArrayList<String>();
        }
        return this.resps;
    }

    /**
     * Gets the value of the cert property.
     *
     * @return possible object is
     * {@link TeiDataCertainty }
     */
    public TeiDataCertainty getCert() {
        return cert;
    }

    /**
     * Sets the value of the cert property.
     *
     * @param value allowed object is
     *              {@link TeiDataCertainty }
     */
    public void setCert(TeiDataCertainty value) {
        this.cert = value;
    }

    /**
     * Gets the value of the selects property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the selects property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSelects().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getSelects() {
        if (selects == null) {
            selects = new ArrayList<String>();
        }
        return this.selects;
    }

    /**
     * Gets the value of the corresps property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the corresps property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCorresps().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getCorresps() {
        if (corresps == null) {
            corresps = new ArrayList<String>();
        }
        return this.corresps;
    }

    /**
     * Gets the value of the sameAs property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSameAs() {
        return sameAs;
    }

    /**
     * Sets the value of the sameAs property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSameAs(String value) {
        this.sameAs = value;
    }

    /**
     * Gets the value of the excludes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the excludes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getExcludes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getExcludes() {
        if (excludes == null) {
            excludes = new ArrayList<String>();
        }
        return this.excludes;
    }

    /**
     * Gets the value of the copyOf property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCopyOf() {
        return copyOf;
    }

    /**
     * Sets the value of the copyOf property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCopyOf(String value) {
        this.copyOf = value;
    }

    /**
     * Gets the value of the prev property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getPrev() {
        return prev;
    }

    /**
     * Sets the value of the prev property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPrev(String value) {
        this.prev = value;
    }

    /**
     * Gets the value of the synches property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the synches property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSynches().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getSynches() {
        if (synches == null) {
            synches = new ArrayList<String>();
        }
        return this.synches;
    }

    /**
     * Gets the value of the next property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getNext() {
        return next;
    }

    /**
     * Sets the value of the next property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNext(String value) {
        this.next = value;
    }

    /**
     * Gets the value of the space property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSpace() {
        return space;
    }

    /**
     * Sets the value of the space property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSpace(String value) {
        this.space = value;
    }

    /**
     * Gets the value of the decls property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the decls property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDecls().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public java.util.List<String> getDecls() {
        if (decls == null) {
            decls = new ArrayList<String>();
        }
        return this.decls;
    }

}
