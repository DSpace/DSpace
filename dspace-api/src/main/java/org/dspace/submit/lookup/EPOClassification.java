/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import org.dspace.app.util.XMLUtils;
import org.w3c.dom.Element;

/***
 * Classification in EPO.
 * 
 * {@code
 * <patent-classification sequence="1">
 *    <classification-scheme office="EP" scheme="CPCI"/>
 *    <section>F</section>
 *    <class>24</class>
 *    <subclass>F</subclass>
 *    <main-group>7</main-group>
 *    <subgroup>007</subgroup>
 *    <classification-value>I</classification-value>
 *    <generating-office>EP</generating-office>
 * </patent-classification>
 * }
 * 
 * @author fcadili (franceso.cadili at 4science.it)
 */
public class EPOClassification {
    private String section;
    private String classOf;
    private String subclass;
    private String mainGroup;
    private String subGroup;

    public EPOClassification(Element classification) {
        section = XMLUtils.getElementValue(classification, "section");
        classOf = XMLUtils.getElementValue(classification, "class");
        subclass = XMLUtils.getElementValue(classification, "subclass");
        mainGroup = XMLUtils.getElementValue(classification, "main-group");
        subGroup = XMLUtils.getElementValue(classification, "subgroup");
    }

    /***
     * {@link https://en.wikipedia.org/wiki/International_Patent_Classification}
     * 
     * Each classification symbol is of the form A01B 1/00 (which represents "hand
     * tools"). The first letter represents the "section" consisting of a letter
     * from A ("Human Necessities") to H ("Electricity").
     * 
     * Combined with a two digit number, it represents the "class" (class A01
     * represents "Agriculture; forestry; animal husbandry; trapping; fishing"). The
     * final letter makes up the "subclass" (subclass A01B represents "Soil working
     * in agriculture or forestry; parts, details, or accessories of agricultural
     * machines or implements, in general").
     * 
     * The subclass is followed by a one-to-three-digit "group" number, an oblique
     * stroke and a number of at least two digits representing a "main group" or
     * "subgroup". A patent examiner assigns classification symbols to patent
     * application or other document in accordance with classification rules, and
     * generally at the most detailed level which is applicable to its content.
     */
    @Override
    public String toString() {
        return section + classOf + subclass + " " + mainGroup + "/" + subGroup;
    }
}
