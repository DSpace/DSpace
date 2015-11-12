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
public class AuthorsList {
    public List<Author> author = new ArrayList<Author>();
    public AuthorsList() { }
}
