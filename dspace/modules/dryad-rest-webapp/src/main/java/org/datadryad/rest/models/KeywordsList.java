/*
 */
package org.datadryad.rest.models;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * For XML/JSON conversion?
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
public class KeywordsList {
    public List<String> keyword = new ArrayList<String>();
    public KeywordsList() { }
}
