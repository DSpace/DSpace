/**
 * Copyright (c) 2007, Aberystwyth University
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
package org.purl.sword.test;

/**
 *   Author   : $Author: nst $
 *   Date     : $Date: 2007/09/21 15:18:57 $
 *   Revision : $Revision: 1.3 $
 *   Name     : $Name:  $
 */

import org.purl.sword.base.*;

import org.w3.atom.*;

/**
 * Simple test class for the ServiceDocument and DepositResponse 
 * classes in the SWORD common classes. 
 * 
 * @author Neil Taylor
 */
public class SwordTest
{
   /**
    * Start the test of the ServiceDocument followed by the 
    * DepositResponse. 
    * 
    * @param args
    */
   public static void main(String[] args)
   {
      SwordTest test = new SwordTest();
      test.serviceDocumentTest(); 
      
      test.depositResponseTest();
   }
   
   /**
    * Create a test ServiceDocument class. Marshall and Unmarshall the
    * data to ensure that it is preserved when transformed to and 
    * from an XML string. 
    */
   public void serviceDocumentTest()
   {
      // create a new service document 
      Service service = new Service(ServiceLevel.ZERO);
      service.setVerbose(true);
      service.setNoOp(false);
      
      // add some workspace/collections
      Workspace workspace = new Workspace();
      workspace.setTitle("This is a test");
      
      Collection collection = new Collection(); 
      collection.setTitle("The first collection");
      collection.setLocation("http://www.somewhere.com/here");
      
      workspace.addCollection(collection);
      
      service.addWorkspace(workspace);
      
      workspace = new Workspace();
      workspace.setTitle("This is a second test");
      
      collection = new Collection(); 
      collection.setTitle("The second collection");
      collection.setLocation("http://www.somewhere.com/here/something");
      collection.addAccepts("application/zip");
      collection.addAccepts("application/xml");
      collection.setAbstract("An abstract goes in here");
      collection.setCollectionPolicy("A collection policy");
      collection.setMediation(true);
      collection.setFormatNamespace("a namespace in here");
      collection.setTreatment("treatment in here too");
      
      workspace.addCollection(collection);
      
      service.addWorkspace(workspace);
      
      // create the service document, marshall, unmarshall and marshall again. 
      ServiceDocument document = new ServiceDocument(service);
      
      // display the XML document that has been constructed
      String doc = document.marshall(); 
      System.out.println(doc);
      
      try
      {
    	   ServiceDocument unmarshalledDocument = new ServiceDocument(); 
         unmarshalledDocument.unmarshall(doc);
      
         System.out.println(unmarshalledDocument.marshall());
      }
      catch( Exception e )
      {
    	  e.printStackTrace();
      }
      
   }
   
   /**
    * Create a test DepositResponse class. Marshall and Unmarshall the
    * data to ensure that it is preserved when transformed to and 
    * from an XML string. 
    */
   public void depositResponseTest()
   {
      try
      {
         
         DepositResponse response = new DepositResponse(HttpHeaders.ACCEPTED);
         SWORDEntry entry = new SWORDEntry(); 
         entry.setId("atom:com.intrallect.atomTest3p0:60");
         Title title = new Title();
         title.setContent("Burning Stubble");
         title.setType(ContentType.TEXT);
         entry.setTitle(title);

         // add authors
         Author author = new Author();
         author.setName("Sword Tester");
         author.setEmail("sword@ukoln.ac.uk");
         author.setUri("http://www.ukoln.ac.uk/repositories/digirep/index/SWORD");
         entry.addAuthors(author);

         author = new Author();
         author.setName("CASIS Tester");
         author.setEmail("nst@aber.ac.uk");
         author.setUri("http://www.aber.ac.uk/casis/");
         entry.addAuthors(author);

         // add links
         Link link = new Link();
         link.setRel("edit-media");
         link.setHref("http://bagel.intrallect.com:5555/intralibrary3p0/IntraLibrary-Deposit/edit-media/learning_object_id60");
         link.setHreflang("en");
         link.setTitle("Edit Media Title");
         link.setType("edit media type");
         link.setContent("some content in here");

         entry.addLink(link);

         link = new Link();
         link.setRel("edit");
         link.setHref("http://bagel.intrallect.com:5555/intralibrary3p0/IntraLibrary-Deposit/edit/learning_object_id60");
         link.setHreflang("en");
         link.setTitle("Edit Title");
         link.setType("edit type");
         link.setContent("some content in here");

         entry.addLink(link);

         entry.addCategory("test category");
         entry.addCategory("second test category");

         Contributor contributor = new Contributor();
         contributor.setName("Neil Taylor");
         contributor.setEmail("nst@aber.ac.uk");
         contributor.setUri("http://www.aber.ac.uk/casis/");

         entry.addContributor(contributor);

         Rights rights = new Rights();
         rights.setType(ContentType.TEXT);
         rights.setContent("Rights declaration.");
         entry.setRights(rights);

         Content content = new Content(); 
         content.setSource("http://bagel.intrallect.com:5555/intralibrary3p0/IntraLibrary?command=open-preview&amp;learning_object_key=i189n4207t");
         content.setType("application/zip");
         entry.setContent(content);

         Generator generator = new Generator(); 
         generator.setContent("Test Generator ID");
         generator.setUri("http://www.somewhere.com/");
         generator.setVersion("1.1");
         
         Source source = new Source();
         source.setGenerator(generator);
         entry.setSource(source);
         
         entry.setPublished("2007-08-02T10:13:14Z");
         entry.setUpdated("2007-08-02T10:22:17Z");
         
         
         entry.setFormatNamespace("Test format namespace");
         entry.setTreatment("Treatment description");
         entry.setNoOp(true);
         entry.setVerboseDescription("A Verbose Description.");
          
         
         response.setEntry( entry );

         String test = response.marshall(); 
         System.out.println(test);
         System.out.println("=================");

         DepositResponse unmarshalledDocument = new DepositResponse(HttpHeaders.CREATED);
         unmarshalledDocument.unmarshall(test);
         System.out.println(unmarshalledDocument.marshall());
      }
      catch( Exception e )
      {
         e.printStackTrace();
      }
   }

}
