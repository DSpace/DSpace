/*
 */
package org.datadryad.rest.legacymodels;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.datadryad.rest.models.Author;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class LegacyAuthorsList {
    public List<String> author = new ArrayList<String>();
    public LegacyAuthorsList() {};

}
