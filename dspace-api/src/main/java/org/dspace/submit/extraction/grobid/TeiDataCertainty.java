/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.extraction.grobid;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tei_data.certainty.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="tei_data.certainty"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}token"&gt;
 *     &lt;enumeration value="high"/&gt;
 *     &lt;enumeration value="medium"/&gt;
 *     &lt;enumeration value="low"/&gt;
 *     &lt;enumeration value="unknown"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 */
@XmlType(name = "tei_data.certainty")
@XmlEnum
public enum TeiDataCertainty {

    @XmlEnumValue("high")
    HIGH("high"),
    @XmlEnumValue("medium")
    MEDIUM("medium"),
    @XmlEnumValue("low")
    LOW("low"),
    @XmlEnumValue("unknown")
    UNKNOWN("unknown");
    private final String value;

    TeiDataCertainty(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TeiDataCertainty fromValue(String v) {
        for (TeiDataCertainty c : TeiDataCertainty.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
