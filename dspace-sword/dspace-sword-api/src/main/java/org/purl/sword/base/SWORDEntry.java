/**
 * Copyright (c) 2008-2009, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package org.purl.sword.base;

import java.util.ArrayList;
import java.util.Properties;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.log4j.Logger;
import org.purl.sword.atom.Entry;
import org.purl.sword.atom.Summary;


/**
 * Extension of the ATOM Entry class. This adds support for the additional 
 * SWORD elements. These elements reside inside the ATOM Entry object, 
 * created in org.w3.atom.Entry class. 
 * 
 * @author Neil Taylor
 */
public class SWORDEntry extends Entry 
{

   /**
    * Specifies whether the document was run in noOp mode, i.e.
    * if the document records that no operation was taken for
    * the deposit other than to generate a response.
    */
   protected SwordNoOp swordNoOp;

   /**
    * Use to supply a verbose description. 
    */
   protected SwordVerboseDescription swordVerboseDescription;
   
   /**
    * Used for a human readable statement about what treatment
    * the deposited resource has received. Include either a
    * text description or a URI.
    */
   protected SwordTreatment swordTreatment;

   /**
    * The user agent
    */
   protected SwordUserAgent swordUserAgent;
   
   /** 
    * The packaging infomation
    */
   private SwordPackaging swordPackaging;

   /**
    * The logger.
    */
   private static Logger log = Logger.getLogger(SWORDEntry.class);

   
   /**
    * Create a new SWORDEntry with the given namespace and element. This method is
    * not normally used, instead the default constructor should be used as this will
    * set the namespace and element correctly.
    * 
    * @param namespace The namespace of the element
    * @param element The element name
    */
   public SWORDEntry(String namespace, String element, String namespaceUri)
   {
	   super(namespace, element, namespaceUri);
   }
   
   /**
    * A default constructor.
    */
   public SWORDEntry()
   {
       super();
   }

   public SWORDEntry(XmlName name)
   {
       super(name); 
   }

   protected void initialise()
   {
      super.initialise();
      swordNoOp = null;
      swordPackaging = null;
      swordVerboseDescription = null;
      swordTreatment = null;
      swordUserAgent = null;
   }
   
   /**
    * Get the current value of NoOp.  
    * 
    * @return True if the value is set, false otherwise. 
    */
   public boolean isNoOp()
   {
      if( swordNoOp == null )
      {
          return false;
      }
      return swordNoOp.getContent();
   }

   /**
    * Call this method to set noOp. It should be called even by internal 
    * methods so that the object can determine if the value has been set 
    * or whether it just holds the default value.  
    * 
    * @param noOp
    */
   public void setNoOp(boolean noOp)
   {
      swordNoOp = new SwordNoOp(noOp); 
   }
   
   /**
    * Determine if the noOp value has been set. This should be called 
    * if you want to know whether false for noOp means that it is the 
    * default value (i.e. no code has set it) or it is a value that
    * has been actively set. 
    * 
    * @return True if the value has been set. Otherwise, false. 
    */
   public boolean isNoOpSet()
   {
      if( swordNoOp == null )
      {
          return false;
      }
      return swordNoOp.isSet();
   }

   /**
    * Get the Verbose Description for this entry. 
    * 
    * @return The description. 
    */
   public String getVerboseDescription()
   {
      if( swordVerboseDescription == null )
      {
          return null;
      }
      return swordVerboseDescription.getContent();
   }

   /**
    * Set the verbose description. 
    * 
    * @param verboseDescription The description. 
    */
   public void setVerboseDescription(String verboseDescription)
   {
      swordVerboseDescription = new SwordVerboseDescription(verboseDescription);
   }

   /**
    * Get the treatment value. 
    * 
    * @return The treatment. 
    */
   public String getTreatment()
   {
      if( swordTreatment == null )
      {
         return null;
      }
      return swordTreatment.getContent();
   }

   /**
    * Set the treatment value. 
    *  
    * @param treatment The treatment. 
    */
   public void setTreatment(String treatment)
   {
      swordTreatment = new SwordTreatment(treatment);
   }
   
   /**
    * Get the user agent
    * 
    * @return the user agent
    */
   public String getUserAgent() 
   {
       if( swordUserAgent == null )
       {
           return null;
       }
	   return swordUserAgent.getContent();
   }
   
   /**
    * Set the user agent
    * 
    * @param userAgent the user agent
    */
   public void setUserAgent(String userAgent)
   {
	   swordUserAgent = new SwordUserAgent(userAgent);
   }
   
   /**
    * Get the packaging format
    * 
    * @return the packaging format
    */
   public String getPackaging()
   {
       if( swordPackaging == null )
       {
           return null;
       }
	   return swordPackaging.getContent();
   }
   
   /**
    * Set the packaging format
    * 
    * @param packaging the packaging format
    */
   public void setPackaging(String packaging)
   {
       this.swordPackaging = new SwordPackaging(packaging);
   }
   
   /**
    * Overrides the marshall method in the parent Entry. This will 
    * call the parent marshall method and then add the additional 
    * elements that have been added in this subclass.  
    */
   public Element marshall()
   {
      Element entry = super.marshall(); 
      return entry;
   }
   
   protected void marshallElements(Element entry)
   {
	   super.marshallElements(entry);

       if( swordTreatment != null )
       {
           entry.appendChild(swordTreatment.marshall());
       }

       if( swordVerboseDescription != null )
       {
           entry.appendChild(swordVerboseDescription.marshall());
       }

       if (swordNoOp != null)
	   {
          entry.appendChild(swordNoOp.marshall());
	   }

       if( swordUserAgent != null )
       {
           entry.appendChild(swordUserAgent.marshall());
       }

       if( swordPackaging != null )
       {
           entry.appendChild(swordPackaging.marshall()); 
       }
	   
   }

   /**
    * Overrides the unmarshall method in the parent Entry. This will 
    * call the parent method to parse the general Atom elements and
    * attributes. This method will then parse the remaining sword
    * extensions that exist in the element. 
    * 
    * @param entry The entry to parse. 
    * 
    * @throws UnmarshallException If the entry is not an atom:entry 
    *              or if there is an exception extracting the data. 
    */
   public SwordValidationInfo unmarshallWithValidation(Element entry, Properties validationProperties)
   throws UnmarshallException
   {
      SwordValidationInfo result = super.unmarshallWithoutValidate(entry, validationProperties);

      processUnexpectedAttributes(entry, result);

      // retrieve all of the sub-elements
      Elements elements = entry.getChildElements();
      Element element = null; 
      int length = elements.size();

      for(int i = 0; i < length; i++ )
      {
    	  element = elements.get(i);

    	  if (isInstanceOf(element, SwordTreatment.elementName()))
    	  {
             if( swordTreatment == null )
             {
                 swordTreatment = new SwordTreatment();
                 result.addUnmarshallElementInfo(
                         swordTreatment.unmarshall(element, validationProperties));
             }
             else if( validationProperties != null )
             {
                 SwordValidationInfo info = new SwordValidationInfo(SwordTreatment.elementName(),
                           SwordValidationInfo.DUPLICATE_ELEMENT,
                           SwordValidationInfoType.WARNING);
                 info.setContentDescription(element.getValue());
                 result.addUnmarshallElementInfo(info);
             }
    	  }
    	  else if (isInstanceOf(element, SwordNoOp.elementName()))
    	  {
    		 if( swordNoOp == null )
             {
                 swordNoOp = new SwordNoOp();
                 result.addUnmarshallElementInfo(swordNoOp.unmarshall(element, validationProperties));
             }
             else if( validationProperties != null )
             {
                 SwordValidationInfo info = new SwordValidationInfo(SwordNoOp.elementName(),
                           SwordValidationInfo.DUPLICATE_ELEMENT,
                           SwordValidationInfoType.WARNING);
                 info.setContentDescription(element.getValue());
                 result.addUnmarshallElementInfo(info);
             }
    	  }
    	  else if (isInstanceOf(element, SwordVerboseDescription.elementName()))
    	  {
    		 if( swordVerboseDescription == null )
             {
                 swordVerboseDescription = new SwordVerboseDescription();
                 result.addUnmarshallElementInfo(swordVerboseDescription.unmarshall(element, validationProperties));
             }
             else if( validationProperties != null )
             {
                 SwordValidationInfo info = new SwordValidationInfo(SwordVerboseDescription.elementName(),
                           SwordValidationInfo.DUPLICATE_ELEMENT,
                           SwordValidationInfoType.WARNING);
                 info.setContentDescription(element.getValue());
                 result.addUnmarshallElementInfo(info);
             }
    	  }
    	  else if (isInstanceOf(element, SwordUserAgent.elementName()))
    	  {
    		 if( swordUserAgent == null )
             {
                 swordUserAgent = new SwordUserAgent();
                 result.addUnmarshallElementInfo(swordUserAgent.unmarshall(element, validationProperties));
             }
             else if( validationProperties != null )
             {
                 SwordValidationInfo info = new SwordValidationInfo(SwordUserAgent.elementName(),
                           SwordValidationInfo.DUPLICATE_ELEMENT,
                           SwordValidationInfoType.WARNING);
                 info.setContentDescription(element.getValue());
                 result.addUnmarshallElementInfo(info);
             }
    	  }
    	  else if (isInstanceOf(element, SwordPackaging.elementName()))
    	  {
    		  if( swordPackaging == null )
             {
                 swordPackaging = new SwordPackaging();
                 result.addUnmarshallElementInfo(swordPackaging.unmarshall(element, validationProperties));
             }
             else if( validationProperties != null )
             {
                 SwordValidationInfo info = new SwordValidationInfo(SwordPackaging.elementName(),
                           SwordValidationInfo.DUPLICATE_ELEMENT,
                           SwordValidationInfoType.WARNING);
                 info.setContentDescription(element.getValue());
                 result.addUnmarshallElementInfo(info);
             }
    	  }
          else if (validationProperties != null )
          {
              XmlName name = new XmlName(element);
              if( ! isElementChecked(name) )
              {
                 SwordValidationInfo info = new SwordValidationInfo(name,
                           SwordValidationInfo.UNKNOWN_ELEMENT,
                           SwordValidationInfoType.INFO);
                 info.setContentDescription(element.getValue());
                 result.addUnmarshallElementInfo(info);
              }
          }

      } // for
      return result;
   }
   
   public SwordValidationInfo unmarshall(Element entry, Properties validationProperties)
   throws UnmarshallException
   {

      SwordValidationInfo result = unmarshallWithValidation(entry, validationProperties);
      if( validationProperties != null )
      {
          result = validate(result, validationProperties);
      }
      return result;
   }

   /**
    * 
    * @param elementName
    * @return
    */
   protected boolean isElementChecked(XmlName elementName)
   {
       if( elementName == null )
       {
           return false;
       }

       return elementName.equals(SwordNoOp.elementName()) |
              elementName.equals(SwordUserAgent.elementName()) | 
              elementName.equals(SwordTreatment.elementName()) |
              elementName.equals(SwordVerboseDescription.elementName()) |
              elementName.equals(SwordPackaging.elementName()) |
              super.isElementChecked(elementName);
   }

   public SwordValidationInfo validate(Properties validationContext)
   {
       return validate(null, validationContext);
   }

   protected SwordValidationInfo validate(SwordValidationInfo info, 
           Properties validationContext)
   {
      boolean validateAll = (info == null);

      SwordValidationInfo swordEntry = super.validate(info, validationContext);
      
      if( swordUserAgent == null )
      {
          String agent = validationContext.getProperty(HttpHeaders.USER_AGENT);

          if( agent != null )
          {
             swordEntry.addValidationInfo(new SwordValidationInfo(SwordUserAgent.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_WARNING +
                     " Clients SHOULD provide a User-Agent request-header (as described in [HTTP1.1] section 14.43). If provided, servers SHOULD store the value in the sword:userAgent element.",
                  SwordValidationInfoType.WARNING));
          }
      }
      else if( swordUserAgent != null && validateAll )
      {
         info.addValidationInfo(swordUserAgent.validate(validationContext));
      }

      // additional rules for sword elements
      if( swordTreatment == null )
      {
          swordEntry.addValidationInfo(new SwordValidationInfo(SwordTreatment.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_ERROR + " MUST be present and contain either a human-readable statement describing treatment the deposited resource has received or a URI that dereferences to such a description.",
                  SwordValidationInfoType.ERROR));
      }
      else if( swordTreatment != null && validateAll )
      {
         info.addValidationInfo(swordTreatment.validate(validationContext));
      }

      // additional rules for sword elements
      if( swordVerboseDescription == null )
      {
          String verbose = validationContext.getProperty(HttpHeaders.X_VERBOSE);
          if( verbose != null )
          {
             swordEntry.addValidationInfo(new SwordValidationInfo(SwordVerboseDescription.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_WARNING + " If the client made the POST request with an X-Verbose:true header, the server SHOULD supply a verbose description of the deposit process.",
                  SwordValidationInfoType.WARNING));
          }
      }
      else if( swordVerboseDescription != null && validateAll )
      {
         info.addValidationInfo(swordVerboseDescription.validate(validationContext));
      }

      if( swordNoOp == null )
      {
          String noOp = validationContext.getProperty(HttpHeaders.X_NO_OP);
          if( noOp != null )
          {
             swordEntry.addValidationInfo(new SwordValidationInfo(SwordNoOp.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_WARNING + " If the client made the POST request with an X-No-Op:true header, the server SHOULD reflect this by including a sword:noOp element with a value of 'true' in the response. See Part A Section 3.1. Servers MAY use a value of 'false' to indicate that the deposit proceeded but MUST NOT use this element to signify an error.",
                  SwordValidationInfoType.WARNING));
          }
      }
      else if( swordNoOp != null && validateAll )
      {
         info.addValidationInfo(swordNoOp.validate(validationContext));
      }

      if( swordPackaging == null )
      {
          swordEntry.addValidationInfo(new SwordValidationInfo(SwordPackaging.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_WARNING + " If the POST request results in the creation of packaged resource, the server MAY use this element to declare the packaging type. If used it SHOULD take a value from [SWORD-TYPES].",
                  SwordValidationInfoType.INFO));
      }
      else if( swordPackaging != null && validateAll )
      {
         info.addValidationInfo(swordPackaging.validate(validationContext));
      }

      return swordEntry;
   }

   /**
    * Overrides the unmarshall method in the parent Entry. This will 
    * call the parent method to parse the general Atom elements and
    * attributes. This method will then parse the remaining sword
    * extensions that exist in the element. 
    * 
    * @param entry The entry to parse. 
    * 
    * @throws UnmarshallException If the entry is not an atom:entry 
    *              or if there is an exception extracting the data. 
    */
   @Override
   public void unmarshall(Element entry)
   throws UnmarshallException
   {
      unmarshall(entry, null);
   }   
}