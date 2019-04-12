/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.util;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.dspace.app.bulkedit.MetadataExport;
import org.dspace.app.xmlui.cocoon.MetadataExportReader;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.TimeZone;

public class CertificateGenerator extends AbstractReader implements Recyclable
{

    /**
    * Messages to be sent when the user is not authorized to view 
    * a particular bitstream. They will be redirected to the login
    * where this message will be displayed.
    */
    private static final String AUTH_REQUIRED_HEADER = "xmlui.ItemExportDownloadReader.auth_header";
    private static final String AUTH_REQUIRED_MESSAGE = "xmlui.ItemExportDownloadReader.auth_message";

    /**
    * How big a buffer should we use when reading from the bitstream before
    * writing to the HTTP response?
    */
     protected static final int BUFFER_SIZE = 8192;

    /**
    * When should a download expire in milliseconds. This should be set to
    * some low value just to prevent someone hitting DSpace repeatedly from
    * killing the server. Note: there are 60000 milliseconds in a minute.
    * 
    * Format: minutes * seconds * milliseconds
    */
    protected static final int expires = 60 * 60 * 60000;

    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    private static Logger log = Logger.getLogger(MetadataExportReader.class);


    PDDocument pdfDocument = null;
    MetadataExport exporter = null;
    String filename = null;
    File file = null;
    PDType0Font font = null;   
    /**
    * Set up the export reader.
    *
    * See the class description for information on configuration options.
    */
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, par);
        try {
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);
            ChoiceAuthorityManager cmgr = ChoiceAuthorityManager.getManager();
            
            // Si es SeDiCIAdmin
            if (Group.isMember(context, 2)) {
                String handle = par.getParameter("handle");
                DSpaceObject dso = HandleManager.resolveToObject(context, handle);
                
                java.util.List<Integer> itemmd = new ArrayList<Integer>();
                if(dso.getType() == Constants.ITEM) {
                    itemmd.add(dso.getID());
                    exporter = new MetadataExport(context, new ItemIterator(context, itemmd), false);
                    String formTemplate = "/Certificado_2019.pdf";
                    
                    // load the document
                    file = new File("aux");
                    FileUtils.copyFile(org.apache.commons.io.FileUtils.toFile(getClass().getResource(formTemplate)), file);
                    pdfDocument = PDDocument.load(file);
                    
                    PDPageContentStream content = new PDPageContentStream(pdfDocument, pdfDocument.getPage(0), true, true);
                    int FONT_SIZE = 10;
                        
                    content.beginText();
                    content.newLineAtOffset(60, 675);
                    content.setLeading(14.5f);
                    Integer limit = 105;
                    content.setNonStrokingColor(Color.BLACK);
                    font = PDType0Font.load(pdfDocument, org.apache.commons.io.FileUtils.toFile(getClass().getResource("/LiberationSans-Regular.ttf")));

                    content.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    limit = printLine(content, "Por medio del presente certificado, el", limit);
                    content.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
                    limit = printLine(content, "SEDICI", limit);
                    content.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    limit = printLine(content, "confirma el depósito en el repositorio institucional de la UNLP de la obra que se detalla a continuación bajo el identificador", limit);
                    content.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
                    if (dso.getHandle() != null) {
                        limit = printLine(content, dso.getHandle()+ ":", limit);
                    } else {
                        limit = printLine(content, metadatumToString(dso.getMetadataByMetadataString("dc.identifier.uri")), limit);                        
                    }
//                  content.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    PDType0Font font = PDType0Font.load(pdfDocument, org.apache.commons.io.FileUtils.toFile(getClass().getResource("/LiberationSans-Regular.ttf")));

                    content.setFont(font, FONT_SIZE);

                    limit = printNewLine(content);
                    limit = printMetadata(dso, content, limit, "   - Título:", "dc.title");
                    limit = printMetadata(dso, content, limit, "   - Autor(es):", "sedici.creator.person");
                    if ((dso.getMetadata("sedici", "date", "exposure", Item.ANY)).length > 0) {
                        limit = printMetadata(dso, content, limit, "   - Fecha de exposición:", "sedici.date.exposure");            
                    }
                    else {
                        limit = printMetadata(dso, content, limit, "   - Fecha de publicación:", "dc.date.issued");            
                    }
                    limit = printMetadata(dso, content, limit, "   - Tipología:", "dc.type");
                    limit = printMetadata(dso, content, limit, "   - Grado alacanzado:", "thesis.degree.name");
                    limit = printMetadata(dso, content, limit, "   - Unidad académica:", "thesis.degree.grantor");
                    content.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
                    limit = printLine(content, "   - Fecha de carga en el repositorio: ", limit);
                    content.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    limit = printLine(content, metadatumToString(dso.getMetadataByMetadataString("dc.date.accessioned")).substring(0, 10), limit);
                    limit = printNewLine(content);                
                    limit = printMetadata(dso, content, limit, "   - El documento se encuentra disponible en:", "dc.identifier.uri");
                    limit = printNewLine(content);
                        
                    TimeZone timeZone = TimeZone.getTimeZone("GMT-3");
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                    simpleDateFormat.setTimeZone(timeZone);               
                    GregorianCalendar gcalendar = new GregorianCalendar();
                    
                    limit = printLine(content, "Este documento fue generado el día " + String.valueOf(gcalendar.get(Calendar.DAY_OF_MONTH)) + "/" + String.valueOf(gcalendar.get(Calendar.MONTH) + 1) + "/" + String.valueOf(gcalendar.get(Calendar.YEAR)) + " a las " + simpleDateFormat.format(calendar.getTime()) + " hs desde el sitio" , limit);
                    content.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);
                    limit = printLine(content, "http://sedici.unlp.edu.ar", limit);
                    content.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    limit = printLine(content, "por el usuario " + context.getCurrentUser().getEmail() , limit);
                    limit = printNewLine(content);
                    content.showText("-------------------------------------------------------------------------------------------------------------------------------------");
                    
                    content.endText();
                    content.setLineWidth(.5f);
                    content.close();
                        
                    // Save and close the filled out form.
                    filename = handle.replaceAll("/", "-") + ".pdf";
                    pdfDocument.save(file);
                    pdfDocument.close();

                }
                log.info(LogManager.getHeader(context, "certificateexport", "exporting_handle:" + handle));           
                log.info(LogManager.getHeader(context, "certificateexport", "exported_file:" + filename));
              }
              else {
                  if(AuthenticationUtil.isLoggedIn(request)) {
                       String redictURL = request.getContextPath() + "/restricted-resource";
                           HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                           httpResponse.sendRedirect(redictURL);
                           return;
                  }
                  else {
                	  String redictURL = request.getContextPath() + "/login";
                	  AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);
                	  HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
                	  httpResponse.sendRedirect(redictURL);
                	  return;
                  }
              }
        }
        catch (RuntimeException e) {
            throw e;    
        }
        catch (Exception e) {
            throw new ProcessingException("Unable to read bitstream.",e);
        } 
    }

    
    private static Integer printNewLine(PDPageContentStream content) throws IOException {
        Integer limit;
        content.newLine();
        limit = 105;
        return limit;
    }
    
    
    private Integer printMetadata(DSpaceObject dso, PDPageContentStream content, Integer limit, String label, String metadataString) throws IOException {
        Metadatum metadata[] = dso.getMetadataByMetadataString(metadataString);
        if (metadata.length > 0) {
            content.setFont(PDType1Font.HELVETICA_BOLD, 10);
            limit = printLine(content, label, limit);
            content.setFont(font, 10);
            limit = printLine(content, metadatumToString(metadata), limit);
            if (metadataString.equals("thesis.degree.grantor")) {
            	// Hardcodeamos "(Universidad Nacional de La Plata)" porque nos lo pidieron. Podríamos usar autoridades pero el label (cuando se busca por id de autoridad ) no devulve la institucion padre 
                limit = printLine(content, "(Universidad Nacional de La Plata)", limit);                
            }
            limit = printNewLine(content);
        }
        return limit;
    }

   
    /**
     * Write the PDF.
     * 
     */
    public void generate() throws IOException, SAXException, ProcessingException {
        response.setContentType("application/pdf; charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=" + filename);
        out.write(FileUtils.readFileToByteArray(file));
        out.flush();
        out.close();
        file.deleteOnExit();
    }

   
   /**
     * Recycle
     */
    public void recycle() {        
        this.response = null;
        this.request = null;
        this.exporter = null;
        this.filename = null;
        this.pdfDocument = null;
        this.file = null;
        super.recycle();
    }

    private static Integer printLine(PDPageContentStream content, String string, Integer limit) throws IOException {
        String arr[] = string.split(" ", 2);
        if (arr[0] == " ") {
            arr = arr[1].split(" ", 2);
        }
        if (limit - arr[0].length() - 1 <= 0) {
            limit = printNewLine(content);
        }
        while (limit - arr[0].length() - 1 > 0) {
            content.showText(" " + arr[0]);
            limit = limit - arr[0].length() - 1;
            if (arr.length == 2) {
                arr = arr[1].split(" ", 2);
                if (limit - arr[0].length() -1 <= 0) {
                    limit = printNewLine(content);
                }
            } else if(arr.length == 1){
                break;
            }
        }
        return limit;            
    }

    private String metadatumToString(Metadatum[] metadata) {
        String string = "";
        if (metadata.length > 0) {
            for (int i = 0; i < metadata.length; i++) {
                string += metadata[i].value.replaceAll("\\<.*?>","");                        
                if (i < metadata.length - 1) {
                    string += " y ";
                }
                else if (i < metadata.length - 1) {
                    string += ", ";
                }
            }
        }
        return string;
     }
}