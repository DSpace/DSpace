/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.atom;

import org.purl.sword.base.Namespaces;
import org.purl.sword.base.XmlName;

/**
 * Represents an ATOM Rights element. This is a simple subclass of the 
 * TextConstruct class. 
 * 
 * @author Neil Taylor
 */
public class Rights extends TextConstruct
{
   /**
	* Local name for the element. 
	*/
   private static final XmlName XML_NAME = new XmlName(
           Namespaces.PREFIX_ATOM, "rights", Namespaces.NS_ATOM);



   /** 
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'rights'. 
    */
   public Rights()
   {
      super(XML_NAME);
   }

   public static XmlName elementName()
   {
       return XML_NAME;
   }

}
