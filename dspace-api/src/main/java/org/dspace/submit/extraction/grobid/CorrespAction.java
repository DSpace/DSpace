/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.extraction.grobid;

import java.util.ArrayList;
import java.util.List;
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
 *       &lt;choice&gt;
 *         &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.correspActionPart" maxOccurs="unbounded"/&gt;
 *         &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.pLike" maxOccurs="unbounded"/&gt;
 *       &lt;/choice&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.typed.attribute.subtype"/&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.global.attributes"/&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.sortable.attributes"/&gt;
 *       &lt;attribute name="type"&gt;
 *         &lt;simpleType&gt;
 *           &lt;union memberTypes=" {http://www.w3.org/2001/XMLSchema}Name"&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *                 &lt;enumeration value="sent"/&gt;
 *               &lt;/restriction&gt;
 *             &lt;/simpleType&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *                 &lt;enumeration value="received"/&gt;
 *               &lt;/restriction&gt;
 *             &lt;/simpleType&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *                 &lt;enumeration value="transmitted"/&gt;
 *               &lt;/restriction&gt;
 *             &lt;/simpleType&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *                 &lt;enumeration value="redirected"/&gt;
 *               &lt;/restriction&gt;
 *             &lt;/simpleType&gt;
 *             &lt;simpleType&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *                 &lt;enumeration value="forwarded"/&gt;
 *               &lt;/restriction&gt;
 *             &lt;/simpleType&gt;
 *           &lt;/union&gt;
 *         &lt;/simpleType&gt;
 *       &lt;/attribute&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "psAndTrashes",
    "datesAndEmailsAndAddresses"
})
@XmlRootElement(name = "correspAction")
public class CorrespAction {

    @XmlElements( {
        @XmlElement(name = "p", type = P.class),
        @XmlElement(name = "trash", type = Trash.class)
    })
    protected List<Object> psAndTrashes;
    @XmlElements( {
        @XmlElement(name = "date", type = Date.class),
        @XmlElement(name = "email", type = Email.class),
        @XmlElement(name = "address", type = Address.class),
        @XmlElement(name = "affiliation", type = Affiliation.class),
        @XmlElement(name = "name", type = Name.class),
        @XmlElement(name = "orgName", type = OrgName.class),
        @XmlElement(name = "persName", type = PersName.class),
        @XmlElement(name = "country", type = Country.class),
        @XmlElement(name = "region", type = Region.class),
        @XmlElement(name = "settlement", type = Settlement.class),
        @XmlElement(name = "idno", type = Idno.class),
        @XmlElement(name = "surname", type = Surname.class),
        @XmlElement(name = "forename", type = Forename.class),
        @XmlElement(name = "roleName", type = RoleName.class),
        @XmlElement(name = "note", type = Note.class)
    })
    protected List<Object> datesAndEmailsAndAddresses;
    @XmlAttribute(name = "type")
    protected String type;
    @XmlAttribute(name = "subtype")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String subtype;
    @XmlAttribute(name = "coords")
    @XmlSchemaType(name = "anySimpleType")
    protected String coords;
    @XmlAttribute(name = "n")
    protected String n;
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;
    @XmlAttribute(name = "rendition")
    protected List<String> renditions;
    @XmlAttribute(name = "style")
    protected String style;
    @XmlAttribute(name = "rend")
    protected List<String> rends;
    @XmlAttribute(name = "id", namespace = "http://www.w3.org/XML/1998/namespace")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "base", namespace = "http://www.w3.org/XML/1998/namespace")
    protected String base;
    @XmlAttribute(name = "resp")
    protected List<String> resps;
    @XmlAttribute(name = "cert")
    protected TeiDataCertainty cert;
    @XmlAttribute(name = "select")
    protected List<String> selects;
    @XmlAttribute(name = "corresp")
    protected List<String> corresps;
    @XmlAttribute(name = "sameAs")
    protected String sameAs;
    @XmlAttribute(name = "exclude")
    protected List<String> excludes;
    @XmlAttribute(name = "copyOf")
    protected String copyOf;
    @XmlAttribute(name = "prev")
    protected String prev;
    @XmlAttribute(name = "synch")
    protected List<String> synches;
    @XmlAttribute(name = "next")
    protected String next;
    @XmlAttribute(name = "space", namespace = "http://www.w3.org/XML/1998/namespace")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String space;
    @XmlAttribute(name = "sortKey")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String sortKey;

    /**
     * Gets the value of the psAndTrashes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the psAndTrashes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPSAndTrashes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link P }
     * {@link Trash }
     */
    public List<Object> getPSAndTrashes() {
        if (psAndTrashes == null) {
            psAndTrashes = new ArrayList<Object>();
        }
        return this.psAndTrashes;
    }

    /**
     * Gets the value of the datesAndEmailsAndAddresses property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the datesAndEmailsAndAddresses property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDatesAndEmailsAndAddresses().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Date }
     * {@link Email }
     * {@link Address }
     * {@link Affiliation }
     * {@link Name }
     * {@link OrgName }
     * {@link PersName }
     * {@link Country }
     * {@link Region }
     * {@link Settlement }
     * {@link Idno }
     * {@link Surname }
     * {@link Forename }
     * {@link RoleName }
     * {@link Note }
     */
    public List<Object> getDatesAndEmailsAndAddresses() {
        if (datesAndEmailsAndAddresses == null) {
            datesAndEmailsAndAddresses = new ArrayList<Object>();
        }
        return this.datesAndEmailsAndAddresses;
    }

    /**
     * Gets the value of the type property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the subtype property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSubtype() {
        return subtype;
    }

    /**
     * Sets the value of the subtype property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSubtype(String value) {
        this.subtype = value;
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
    public List<String> getRenditions() {
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
    public List<String> getRends() {
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
    public List<String> getResps() {
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
    public List<String> getSelects() {
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
    public List<String> getCorresps() {
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
    public List<String> getExcludes() {
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
    public List<String> getSynches() {
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
     * Gets the value of the sortKey property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getSortKey() {
        return sortKey;
    }

    /**
     * Sets the value of the sortKey property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSortKey(String value) {
        this.sortKey = value;
    }

}
