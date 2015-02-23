/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cocoon.generation;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameterizable;
import org.apache.avalon.framework.parameters.Parameters;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.SystemUtils;

import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceException;
import org.apache.excalibur.source.SourceValidity;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;

import java.util.Map;

/**
 * Read a plain text file and produce a valid XML file.
 * <pre>
 * &lt;text xmlns="http://chaperon.sourceforge.net/schema/text/1.0"&gt;
 *  Text 123 bla
 * &lt;/text&gt;
 * </pre>
 *
 * @author <a href="mailto:stephan@apache.org">Stephan Michels </a>
 * @author <a href="mailto:rolf.schumacher@hamburg.de">Rolf Schumacher</a>
 * @version CVS $Id: TextGenerator.java 433543 2006-08-22 06:22:54Z crossley $
 */
public class TextGenerator extends ServiceableGenerator implements Parameterizable,
                                                                   CacheableProcessingComponent
{
  /** The URI of the text element */
  public static final String URI = "http://chaperon.sourceforge.net/schema/text/1.0";
  private static final char[] initNonXmlChars =
  {
    ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', 
  //  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  
  ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '
  };

  /** The input source */
  private Source inputSource;
  private String encoding;
  private char[] nonXmlChars;
  private boolean localizable = false;

  /**
   * Recycle this component. All instance variables are set to <code>null</code>.
   */
  public void recycle()
  {
    if (inputSource!=null)
      super.resolver.release(inputSource);

    inputSource = null;
    encoding = null;
    nonXmlChars = null;

    super.recycle();
  }

  /**
   * Provide component with parameters.
   *
   * @param parameters the parameters
   *
   * @throws ParameterException if parameters are invalid
   */
  public void parameterize(Parameters parameters) throws ParameterException
  {
    this.localizable = parameters.getParameterAsBoolean("localizable", false);
  }

  /**
   * Set the SourceResolver, objectModel Map, the source and sitemap Parameters used to process the
   * request.
   *
   * @param resolver Source resolver
   * @param objectmodel Object model
   * @param src Source
   * @param parameters Parameters
   *
   * @throws IOException
   * @throws ProcessingException
   * @throws SAXException
   */
  public void setup(SourceResolver resolver, Map objectmodel, String src, Parameters parameters)
    throws ProcessingException, SAXException, IOException {
    super.setup(resolver, objectmodel, src, parameters);
    try {
      this.encoding = parameters.getParameter("encoding", null);
      this.inputSource = resolver.resolveURI(src);

      String nXmlCh = parameters.getParameter("nonXmlChars", String.valueOf(initNonXmlChars));
      if (nXmlCh.length() != initNonXmlChars.length)
        throw new ProcessingException("Error during resolving of '"+src+"'.",
                                      new SourceException("length of attribute string 'nonXmlChars' is "+
                                                          nXmlCh.length()+" where it should be "+
                                                          initNonXmlChars.length+"!"));

      this.nonXmlChars = nXmlCh.toCharArray();
    } catch (SourceException se) {
      throw new ProcessingException("Error during resolving of '"+src+"'.", se);
    }
  }

  /**
   * Generate the unique key. This key must be unique inside the space of this component.
   *
   * @return The generated key hashes the src
   */
  public Serializable getKey()
  {
    return inputSource.getURI()
        + ";localizable=" + localizable
        + ";encoding=" + encoding;
  }

  /**
   * Generate the validity object.
   *
   * @return The generated validity object or <code>null</code> if the component is currently not
   *         cacheable.
   */
  public SourceValidity getValidity()
  {
    return this.inputSource.getValidity();
  }

  /**
   * Generate XML data.
   *
   * @throws IOException
   * @throws ProcessingException
   * @throws SAXException
   */
  public void generate() throws IOException, SAXException, ProcessingException {
    InputStreamReader in = null;

    try {
      final InputStream sis = this.inputSource.getInputStream();
      if (sis == null) {
        throw new ProcessingException("Source '" + this.inputSource.getURI() + "' not found");
      }

      if (encoding != null) {
        in = new InputStreamReader(sis, encoding);
      } else {
        in = new InputStreamReader(sis);
      }
    } catch (SourceException se) {
      throw new ProcessingException("Error during resolving of '" + this.source + "'.", se);
    }

    LocatorImpl locator = new LocatorImpl();

    locator.setSystemId(this.inputSource.getURI());
    locator.setLineNumber(1);
    locator.setColumnNumber(1);

    contentHandler.setDocumentLocator(locator);
    contentHandler.startDocument();
    contentHandler.startPrefixMapping("", URI);

    AttributesImpl atts = new AttributesImpl();
    if (localizable) {
      atts.addAttribute("", "source", "source", "CDATA", locator.getSystemId());
      atts.addAttribute("", "line", "line", "CDATA", String.valueOf(locator.getLineNumber()));
      atts.addAttribute("", "column", "column", "CDATA", String.valueOf(locator.getColumnNumber()));
    }

    contentHandler.startElement(URI, "text", "text", atts);

    LineNumberReader reader = new LineNumberReader(in);
    String line;
    String newline = null;

    while (true) {
      if (newline==null) {
        line = convertNonXmlChars(reader.readLine());
      } else {
        line = newline;
      }
      if (line==null) {
        break;
      }
      newline = convertNonXmlChars(reader.readLine());
      if (newline != null) {
          line += SystemUtils.LINE_SEPARATOR;
      }
      locator.setLineNumber(reader.getLineNumber());
      locator.setColumnNumber(1);
      contentHandler.characters(line.toCharArray(), 0, line.length());
      if (newline==null) {
        break;
      }
    }
    reader.close();
    contentHandler.endElement(URI, "text", "text");
    contentHandler.endPrefixMapping("");
    contentHandler.endDocument();
  }

  private String convertNonXmlChars(String s) {
    if (s != null) {
        int nv;
        char[] sc = s.toCharArray();
    
        for (int i = 0; i<sc.length; i++) {
          nv = sc[i];
    
          if ((nv>=0) && (nv<nonXmlChars.length)) {
            //do not convert white space characters
            if ((nv!=9) && (nv!=10) && (nv!=13))
              sc[i] = nonXmlChars[nv];
          }
        }
        return String.valueOf(sc);
    } else {
        return null;
    }
  }
}