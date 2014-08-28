/*
 */
package org.datadryad.rest.legacymodels;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * For XML/JSON conversion?
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class LegacyClassifications {
    public List<String> keyword = new ArrayList<String>();
    public LegacyClassifications() {  }
}
