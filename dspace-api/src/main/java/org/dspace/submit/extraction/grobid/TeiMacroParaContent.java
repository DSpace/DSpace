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
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tei_macro.paraContent complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tei_macro.paraContent"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice maxOccurs="unbounded" minOccurs="0"&gt;
 *         &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.phrase"/&gt;
 *         &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.inter"/&gt;
 *         &lt;group ref="{http://www.tei-c.org/ns/1.0}tei_model.global"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tei_macro.paraContent", propOrder = {
    "content"
})
@XmlSeeAlso( {
    P.class,
    Ref.class,
    Title.class,
    Hi.class
})
public class TeiMacroParaContent {

    @XmlElementRefs( {
        @XmlElementRef(name = "hi", namespace = "http://www.tei-c.org/ns/1.0", type = Hi.class, required = false),
        @XmlElementRef(name = "term", namespace = "http://www.tei-c.org/ns/1.0", type = Term.class, required = false),
        @XmlElementRef(name = "title", namespace = "http://www.tei-c.org/ns/1.0", type = Title.class, required = false),
        @XmlElementRef(name = "graphic", namespace = "http://www.tei-c.org/ns/1.0", type = Graphic.class, required =
            false),
        @XmlElementRef(name = "formula", namespace = "http://www.tei-c.org/ns/1.0", type = Formula.class, required =
            false),
        @XmlElementRef(name = "ptr", namespace = "http://www.tei-c.org/ns/1.0", type = Ptr.class, required = false),
        @XmlElementRef(name = "ref", namespace = "http://www.tei-c.org/ns/1.0", type = Ref.class, required = false),
        @XmlElementRef(name = "date", namespace = "http://www.tei-c.org/ns/1.0", type = Date.class, required = false),
        @XmlElementRef(name = "email", namespace = "http://www.tei-c.org/ns/1.0", type = Email.class, required = false),
        @XmlElementRef(name = "address", namespace = "http://www.tei-c.org/ns/1.0", type = Address.class, required =
            false),
        @XmlElementRef(name = "affiliation", namespace = "http://www.tei-c.org/ns/1.0", type = Affiliation.class,
            required = false),
        @XmlElementRef(name = "name", namespace = "http://www.tei-c.org/ns/1.0", type = Name.class, required = false),
        @XmlElementRef(name = "orgName", namespace = "http://www.tei-c.org/ns/1.0", type = OrgName.class, required =
            false),
        @XmlElementRef(name = "persName", namespace = "http://www.tei-c.org/ns/1.0", type = PersName.class, required
            = false),
        @XmlElementRef(name = "country", namespace = "http://www.tei-c.org/ns/1.0", type = Country.class, required =
            false),
        @XmlElementRef(name = "region", namespace = "http://www.tei-c.org/ns/1.0", type = Region.class, required =
            false),
        @XmlElementRef(name = "settlement", namespace = "http://www.tei-c.org/ns/1.0", type = Settlement.class,
            required = false),
        @XmlElementRef(name = "idno", namespace = "http://www.tei-c.org/ns/1.0", type = Idno.class, required = false),
        @XmlElementRef(name = "surname", namespace = "http://www.tei-c.org/ns/1.0", type = Surname.class, required =
            false),
        @XmlElementRef(name = "forename", namespace = "http://www.tei-c.org/ns/1.0", type = Forename.class, required
            = false),
        @XmlElementRef(name = "roleName", namespace = "http://www.tei-c.org/ns/1.0", type = RoleName.class, required
            = false),
        @XmlElementRef(name = "bibl", namespace = "http://www.tei-c.org/ns/1.0", type = Bibl.class, required = false),
        @XmlElementRef(name = "biblStruct", namespace = "http://www.tei-c.org/ns/1.0", type = BiblStruct.class,
            required = false),
        @XmlElementRef(name = "listBibl", namespace = "http://www.tei-c.org/ns/1.0", type = ListBibl.class, required
            = false),
        @XmlElementRef(name = "label", namespace = "http://www.tei-c.org/ns/1.0", type = Label.class, required = false),
        @XmlElementRef(name = "list", namespace = "http://www.tei-c.org/ns/1.0", type = org.dspace.submit.extraction
            .grobid.List.class, required = false),
        @XmlElementRef(name = "table", namespace = "http://www.tei-c.org/ns/1.0", type = Table.class, required = false),
        @XmlElementRef(name = "link", namespace = "http://www.tei-c.org/ns/1.0", type = Link.class, required = false),
        @XmlElementRef(name = "anchor", namespace = "http://www.tei-c.org/ns/1.0", type = Anchor.class, required =
            false),
        @XmlElementRef(name = "note", namespace = "http://www.tei-c.org/ns/1.0", type = Note.class, required = false),
        @XmlElementRef(name = "figure", namespace = "http://www.tei-c.org/ns/1.0", type = Figure.class, required =
            false)
    })
    @XmlMixed
    protected java.util.List<Object> content;

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
     * {@link Hi }
     * {@link Term }
     * {@link Title }
     * {@link Graphic }
     * {@link Formula }
     * {@link Ptr }
     * {@link Ref }
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
     * {@link Bibl }
     * {@link BiblStruct }
     * {@link ListBibl }
     * {@link Label }
     * {@link org.dspace.submit.extraction.grobid.List }
     * {@link Table }
     * {@link Link }
     * {@link Anchor }
     * {@link Note }
     * {@link Figure }
     * {@link String }
     */
    public java.util.List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<Object>();
        }
        return this.content;
    }

}
