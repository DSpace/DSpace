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
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
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
 *         &lt;element ref="{http://www.tei-c.org/ns/1.0}ref" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.editLike.attributes"/&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.naming.attributes"/&gt;
 *       &lt;attGroup ref="{http://www.tei-c.org/ns/1.0}tei_att.datable.attributes"/&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "content"
})
@XmlRootElement(name = "trash")
public class Trash {

    @XmlElementRef(name = "ref", namespace = "http://www.tei-c.org/ns/1.0", type = Ref.class, required = false)
    @XmlMixed
    protected List<Object> content;
    @XmlAttribute(name = "evidence")
    protected List<String> evidences;
    @XmlAttribute(name = "instant")
    protected String instant;
    @XmlAttribute(name = "unit")
    protected String unit;
    @XmlAttribute(name = "quantity")
    protected String quantity;
    @XmlAttribute(name = "precision")
    protected TeiDataCertainty precision;
    @XmlAttribute(name = "atMost")
    protected String atMost;
    @XmlAttribute(name = "min")
    protected String min;
    @XmlAttribute(name = "atLeast")
    protected String atLeast;
    @XmlAttribute(name = "max")
    protected String max;
    @XmlAttribute(name = "confidence")
    protected Double confidence;
    @XmlAttribute(name = "extent")
    protected String extent;
    @XmlAttribute(name = "scope")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String scope;
    @XmlAttribute(name = "source")
    protected List<String> sources;
    @XmlAttribute(name = "key")
    protected String key;
    @XmlAttribute(name = "ref")
    protected List<String> reves;
    @XmlAttribute(name = "nymRef")
    protected List<String> nymReves;
    @XmlAttribute(name = "role")
    protected List<String> roles;
    @XmlAttribute(name = "to-iso")
    protected String toIso;
    @XmlAttribute(name = "notBefore-iso")
    protected String notBeforeIso;
    @XmlAttribute(name = "notAfter-iso")
    protected String notAfterIso;
    @XmlAttribute(name = "from-iso")
    protected String fromIso;
    @XmlAttribute(name = "when-iso")
    protected String whenIso;
    @XmlAttribute(name = "notBefore-custom")
    protected List<String> notBeforeCustoms;
    @XmlAttribute(name = "notAfter-custom")
    protected List<String> notAfterCustoms;
    @XmlAttribute(name = "datingPoint")
    protected String datingPoint;
    @XmlAttribute(name = "when-custom")
    protected List<String> whenCustoms;
    @XmlAttribute(name = "from-custom")
    protected List<String> fromCustoms;
    @XmlAttribute(name = "to-custom")
    protected List<String> toCustoms;
    @XmlAttribute(name = "datingMethod")
    protected String datingMethod;
    @XmlAttribute(name = "calendar")
    protected String calendar;
    @XmlAttribute(name = "period")
    protected String period;
    @XmlAttribute(name = "notAfter")
    protected String notAfter;
    @XmlAttribute(name = "notBefore")
    protected String notBefore;
    @XmlAttribute(name = "from")
    protected String from;
    @XmlAttribute(name = "when")
    protected String when;
    @XmlAttribute(name = "to")
    protected String to;

    /**
     * Gets the value of the content property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the content property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContent().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Ref }
     * {@link String }
     */
    public List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<Object>();
        }
        return this.content;
    }

    /**
     * Gets the value of the evidences property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the evidences property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEvidences().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getEvidences() {
        if (evidences == null) {
            evidences = new ArrayList<String>();
        }
        return this.evidences;
    }

    /**
     * Gets the value of the instant property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getInstant() {
        if (instant == null) {
            return "false";
        } else {
            return instant;
        }
    }

    /**
     * Sets the value of the instant property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setInstant(String value) {
        this.instant = value;
    }

    /**
     * Gets the value of the unit property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the value of the unit property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setUnit(String value) {
        this.unit = value;
    }

    /**
     * Gets the value of the quantity property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getQuantity() {
        return quantity;
    }

    /**
     * Sets the value of the quantity property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setQuantity(String value) {
        this.quantity = value;
    }

    /**
     * Gets the value of the precision property.
     *
     * @return possible object is
     * {@link TeiDataCertainty }
     */
    public TeiDataCertainty getPrecision() {
        return precision;
    }

    /**
     * Sets the value of the precision property.
     *
     * @param value allowed object is
     *              {@link TeiDataCertainty }
     */
    public void setPrecision(TeiDataCertainty value) {
        this.precision = value;
    }

    /**
     * Gets the value of the atMost property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getAtMost() {
        return atMost;
    }

    /**
     * Sets the value of the atMost property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAtMost(String value) {
        this.atMost = value;
    }

    /**
     * Gets the value of the min property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getMin() {
        return min;
    }

    /**
     * Sets the value of the min property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMin(String value) {
        this.min = value;
    }

    /**
     * Gets the value of the atLeast property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getAtLeast() {
        return atLeast;
    }

    /**
     * Sets the value of the atLeast property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAtLeast(String value) {
        this.atLeast = value;
    }

    /**
     * Gets the value of the max property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getMax() {
        return max;
    }

    /**
     * Sets the value of the max property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMax(String value) {
        this.max = value;
    }

    /**
     * Gets the value of the confidence property.
     *
     * @return possible object is
     * {@link Double }
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * Sets the value of the confidence property.
     *
     * @param value allowed object is
     *              {@link Double }
     */
    public void setConfidence(Double value) {
        this.confidence = value;
    }

    /**
     * Gets the value of the extent property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getExtent() {
        return extent;
    }

    /**
     * Sets the value of the extent property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setExtent(String value) {
        this.extent = value;
    }

    /**
     * Gets the value of the scope property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the value of the scope property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setScope(String value) {
        this.scope = value;
    }

    /**
     * Gets the value of the sources property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sources property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSources().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getSources() {
        if (sources == null) {
            sources = new ArrayList<String>();
        }
        return this.sources;
    }

    /**
     * Gets the value of the key property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the key property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * Gets the value of the reves property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reves property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReves().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getReves() {
        if (reves == null) {
            reves = new ArrayList<String>();
        }
        return this.reves;
    }

    /**
     * Gets the value of the nymReves property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nymReves property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNymReves().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getNymReves() {
        if (nymReves == null) {
            nymReves = new ArrayList<String>();
        }
        return this.nymReves;
    }

    /**
     * Gets the value of the roles property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the roles property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRoles().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getRoles() {
        if (roles == null) {
            roles = new ArrayList<String>();
        }
        return this.roles;
    }

    /**
     * Gets the value of the toIso property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getToIso() {
        return toIso;
    }

    /**
     * Sets the value of the toIso property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setToIso(String value) {
        this.toIso = value;
    }

    /**
     * Gets the value of the notBeforeIso property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getNotBeforeIso() {
        return notBeforeIso;
    }

    /**
     * Sets the value of the notBeforeIso property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNotBeforeIso(String value) {
        this.notBeforeIso = value;
    }

    /**
     * Gets the value of the notAfterIso property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getNotAfterIso() {
        return notAfterIso;
    }

    /**
     * Sets the value of the notAfterIso property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNotAfterIso(String value) {
        this.notAfterIso = value;
    }

    /**
     * Gets the value of the fromIso property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getFromIso() {
        return fromIso;
    }

    /**
     * Sets the value of the fromIso property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFromIso(String value) {
        this.fromIso = value;
    }

    /**
     * Gets the value of the whenIso property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getWhenIso() {
        return whenIso;
    }

    /**
     * Sets the value of the whenIso property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setWhenIso(String value) {
        this.whenIso = value;
    }

    /**
     * Gets the value of the notBeforeCustoms property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the notBeforeCustoms property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNotBeforeCustoms().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getNotBeforeCustoms() {
        if (notBeforeCustoms == null) {
            notBeforeCustoms = new ArrayList<String>();
        }
        return this.notBeforeCustoms;
    }

    /**
     * Gets the value of the notAfterCustoms property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the notAfterCustoms property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNotAfterCustoms().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getNotAfterCustoms() {
        if (notAfterCustoms == null) {
            notAfterCustoms = new ArrayList<String>();
        }
        return this.notAfterCustoms;
    }

    /**
     * Gets the value of the datingPoint property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getDatingPoint() {
        return datingPoint;
    }

    /**
     * Sets the value of the datingPoint property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDatingPoint(String value) {
        this.datingPoint = value;
    }

    /**
     * Gets the value of the whenCustoms property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the whenCustoms property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWhenCustoms().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getWhenCustoms() {
        if (whenCustoms == null) {
            whenCustoms = new ArrayList<String>();
        }
        return this.whenCustoms;
    }

    /**
     * Gets the value of the fromCustoms property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the fromCustoms property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFromCustoms().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getFromCustoms() {
        if (fromCustoms == null) {
            fromCustoms = new ArrayList<String>();
        }
        return this.fromCustoms;
    }

    /**
     * Gets the value of the toCustoms property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the toCustoms property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getToCustoms().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     */
    public List<String> getToCustoms() {
        if (toCustoms == null) {
            toCustoms = new ArrayList<String>();
        }
        return this.toCustoms;
    }

    /**
     * Gets the value of the datingMethod property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getDatingMethod() {
        return datingMethod;
    }

    /**
     * Sets the value of the datingMethod property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDatingMethod(String value) {
        this.datingMethod = value;
    }

    /**
     * Gets the value of the calendar property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getCalendar() {
        return calendar;
    }

    /**
     * Sets the value of the calendar property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCalendar(String value) {
        this.calendar = value;
    }

    /**
     * Gets the value of the period property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getPeriod() {
        return period;
    }

    /**
     * Sets the value of the period property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPeriod(String value) {
        this.period = value;
    }

    /**
     * Gets the value of the notAfter property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getNotAfter() {
        return notAfter;
    }

    /**
     * Sets the value of the notAfter property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNotAfter(String value) {
        this.notAfter = value;
    }

    /**
     * Gets the value of the notBefore property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getNotBefore() {
        return notBefore;
    }

    /**
     * Sets the value of the notBefore property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNotBefore(String value) {
        this.notBefore = value;
    }

    /**
     * Gets the value of the from property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the value of the from property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFrom(String value) {
        this.from = value;
    }

    /**
     * Gets the value of the when property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getWhen() {
        return when;
    }

    /**
     * Sets the value of the when property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setWhen(String value) {
        this.when = value;
    }

    /**
     * Gets the value of the to property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the value of the to property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTo(String value) {
        this.to = value;
    }

}
