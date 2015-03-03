/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;

/**
 * Servlet for retrieving item record in serveral bibliographic formats.
 * <P>
 * This functionality is based in Andres Quast BibFormatTag for DSpace 1.7.2
 * </P>
 * 
 * @author David Andrés Maznzano Herrera <damanzano>
 */
public class BibliographyServlet extends DSpaceServlet{
    /** log4j logger */
    private static Logger log = Logger.getLogger(BibliographyServlet.class);
    /** Item to display */
    private Item item;
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException {

        String path = request.getPathInfo();
        String bibformat = request.getParameter("bib");
        String handle = request.getParameter("handle_item");
        String filename="";
        
        DSpaceObject dso = null;

        if (handle != null) {
            dso = HandleManager.resolveToObject(context, handle);

            if (dso == null) {
                log.error(LogManager.getHeader(context, "invalid_id", "path=" + path));
                JSPManager.showInvalidIDError(request, response, StringEscapeUtils.escapeHtml(path), -1);

                return;
            }
        }

        if (dso.getType() == Constants.ITEM) {
            item = (Item) dso;

            String textOutput = null;

            if (bibformat.equals("tex")) {
                //textOutput = (String) getServletConfig().getServletContext().getAttribute("bibliography.bibData");
                textOutput = renderBibTex(item, request);
            } else if (bibformat.equals("en")) {
                //textOutput = (String) getServletConfig().getServletContext().getAttribute("bibliography.enData");
                textOutput = renderEN(item, request);
            } else if (bibformat.equals("ris")) {
                textOutput= renderRIS(item, request);
            } else if(bibformat.equals("csv")){
               textOutput=renderCSV(item, request);
            } else if(bibformat.equals("tsv")){
               textOutput=renderTSV(item, request);
            }  
            else {
                log.error(LogManager.getHeader(context, "invalid_format", "path=" + path));
                JSPManager.showInvalidIDError(request, response, StringEscapeUtils.escapeHtml(path), -1);
                textOutput = null;
            }
            
            // Pipe the bits
            // Set the response MIME type
            if(bibformat.equals("csv"))
            {
              response.setContentType("text/csv; charset=UTF-8");
              filename= handle.replaceAll("/", "-") + ".csv";
              response.setHeader("Content-Disposition", "attachment; filename=" + filename);
              
            } else if(bibformat.equals("tsv")){
              response.setContentType("text/tab-separated-values; charset=UTF-8");
              filename= handle.replaceAll("/", "-") + ".tsv";
              response.setHeader("Content-Disposition", "attachment; filename=" + filename); 
            }
            else{
              response.setContentType("text/plain;charset=UTF-8");  
            }
            
            //response.setCharacterEncoding("UTF-8");

            PrintWriter out = response.getWriter();
            out.write(textOutput);
            out.flush();
            response.getWriter().flush();  
        }

    }

    public String renderBibTex(Item item, HttpServletRequest request) {
        //angenommen ich bekomme eine Liste von Items: items Dann muss diese gerendert werden
        //create a stringbuffer for storing the metadata
        String schema = "dc";
        String BibTexHead = "@";

        //define needed metadatafields
        int bibType = 1;
        String[] DC2Bib = new String[5];
        DC2Bib[1] = "dc.creator, dc.title, dc.relation.ispartofseries, dc.date.issued, dc.identifier.issn";
        DC2Bib[2] = "dc.creator, dc.contributor, dc.title, dc.publisher, dc.date.issued, dc.identifier.isbn";
        DC2Bib[3] = "dc.creator, dc.contributor, dc.title, dc.publisher, dc.date.issued, dc.identifier.isbn";

        StringBuffer sb = new StringBuffer();

        //Parsing metadatafields
        boolean tyFound = false;

        //First needed for BibTex: dc.type
        DCValue[] types = item.getMetadata(schema, "type", Item.ANY, Item.ANY);

        for (int j = 0; (j < types.length) && !tyFound; j++) {
            String type = types[j].value;

            log.debug("DEBUG, Der gefundene Typ: " + type);//DEBUG

            if (type.equalsIgnoreCase("Article")) {
                sb.append(BibTexHead + "article{");
                bibType = 1;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Book")) {
                sb.append(BibTexHead + "book{");
                bibType = 2;
                tyFound = true;
            } else if (type.equalsIgnoreCase("BookChapter")) {
                sb.append(BibTexHead + "inbook{");
                bibType = 3;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Thesis")) {
                sb.append(BibTexHead + "thesis{");
                bibType = 3;
                tyFound = true;
            } else if (type.equalsIgnoreCase("TechnicalReport")) {
                sb.append(BibTexHead + "techreport{");
                bibType = 1;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Preprint")) {
                sb.append(BibTexHead + "article{");
                bibType = 1;
                tyFound = true;
            }
        }

        //set type in case no type is given 
        if (!tyFound) {
            sb.append(BibTexHead + "article{");
            bibType = 1;
        }

        sb.append("bibliotecadigitalicesi" + item.getHandle() + ",\n");

        //Now get all the metadata needed for the requested objecttype
        StringTokenizer st = new StringTokenizer(DC2Bib[bibType], ",");
            //String[] st=DC2Bib[bibType].split("\\,");

        while (st.hasMoreTokens()) {
            String field = st.nextToken().trim();
            String[] eq = field.split("\\.");
            schema = eq[0];
            String element = eq[1];
            String qualifier = Item.ANY;
            if (eq.length > 2 && eq[2].equals("*")) {
                qualifier = Item.ANY;
            } else if (eq.length > 2) {
                qualifier = eq[2];
            } else {
                qualifier = null;
            }
            //log.info("Field:"+field+";Tokens:"+eq.toString()+";Element:"+eq[1]+";Qualifier:"+qualifier);
            DCValue[] values = item.getMetadata(schema, element, qualifier, Item.ANY);
                //log.info("DCVALUES:"+values);

            //Parse the metadata into a record
            for (int k = 0; k < values.length; k++) {
                if (element.equals("contributor") || element.equals("creator")) {
                    if (k == 0) {
                        if (element.equals("creator")) {
                            sb.append("author =    {" + values[k].value);
                        } else {
                            sb.append(qualifier + " =    {" + values[k].value);
                        }

                    } else {
                        sb.append("    and " + values[k].value);
                    }
                    if (k == (values.length - 1)) {
                        sb.append("},");
                    }

                } else if (element.equals("relation")) {
                    if (k == 0) {
                        sb.append("journal" + " =    {" + values[k].value + "},");
                    }
                    if (k == 1) {
                        sb.append("volume" + " =    {" + values[k].value + "},");
                    }
                } else if (element.equals("title")) {
                    if (k == 0) {
                        sb.append("title =    {" + values[k].value + "},");
                    }
                } else if (element.equals("date")) {
                    if (k == 0) {
                        //formating the Date
                        DCDate dd = new DCDate(values[k].value);
                        //String date = UIUtil.displayDate(dd, false, false);
                        String date = dd.displayDate(false, false, UIUtil.getSessionLocale(request)).trim();
                        int last = date.length();
                        date = date.substring((last - 4), (last));

                        sb.append("year =    {" + date + "},");
                    }
                } else {
                    if (k == 0) {
                        sb.append(qualifier + " =    {" + values[k].value + "},");
                    }
                }
                sb.append("\n");
            }
        }
        sb.append("}\n\n");

        String bibData = sb.toString();
        return bibData;
    }

    public String renderEN(Item item, HttpServletRequest request) {
        //angenommen ich bekomme eine Liste von Items: items Dann muss diese gerendert werden
        //create a stringbuffer for storing the metadata
        String schema = "dc";
        String ENHead = "%0 ";
        String ENFoot = "%~ GOEDOC, SUB GOETTINGEN";

        //define needed metadatafields
        int bibType = 1;
        //variable document types may need various metadata fields
        String[] DC2Bib = new String[5];
        DC2Bib[1] = "dc.creator, dc.title, dc.relation.ispartofseries, dc.date.issued, dc.identifier.issn, dc.identifier.uri, dc.description.abstract, dc.subject";
        DC2Bib[2] = "dc.creator, dc.contributor, dc.title, dc.publisher, dc.date.issued, dc.identifier.isbn,  dc.identifier.uri, dc.description.abstract, dc.subject";
        DC2Bib[3] = "dc.creator, dc.contributor, dc.title, dc.publisher, dc.date.issued, dc.identifier.isbn,  dc.identifier.uri, dc.description.abstract, dc.subject";

        StringBuffer sb = new StringBuffer();

        //Parsing metadatafields
        boolean tyFound = false;
        
        //First needed for BibTex: dc.type
        DCValue[] types = item.getMetadata(schema, "type", Item.ANY, Item.ANY);

        for (int j = 0; (j < types.length) && !tyFound; j++) {
            String type = types[j].value;

            log.debug("DEBUG, Der gefundene Typ: " + type);//DEBUG

            if (type.equalsIgnoreCase("Article")) {
                sb.append(ENHead + "Journal Article");
                bibType = 1;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Book")) {
                sb.append(ENHead + type);
                bibType = 2;
                tyFound = true;
            } else if (type.equalsIgnoreCase("BookSection")) {
                sb.append(ENHead + "");
                bibType = 3;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Thesis")) {
                sb.append(ENHead + "Thesis");
                bibType = 3;
                tyFound = true;
            } else if (type.equalsIgnoreCase("TechnicalReport")) {
                sb.append(ENHead + "Report");
                bibType = 1;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Preprint")) {
                sb.append(ENHead + "Journal Article");
                bibType = 1;
                tyFound = true;
            }
        }

        //set type in case no type is given 
        if (!tyFound) {
            sb.append(ENHead + "Journal Article");
            bibType = 1;
        }

        sb.append(" \n");

        //Now get all the metadata needed for the requested objecttype
        StringTokenizer st = new StringTokenizer(DC2Bib[bibType], ",");
            //String[] st=DC2Bib[bibType].split("\\,");

        while (st.hasMoreTokens()) {
            String field = st.nextToken().trim();
            String[] eq = field.split("\\.");
            schema = eq[0];
            String element = eq[1];
            String qualifier = Item.ANY;
            if (eq.length > 2 && eq[2].equals("*")) {
                qualifier = Item.ANY;
            } else if (eq.length > 2) {
                qualifier = eq[2];
            } else {
                qualifier = null;
            }

            DCValue[] values = item.getMetadata(schema, element, qualifier, Item.ANY);

            //Parse the metadata into a record
            for (int k = 0; k < values.length; k++) {
                if (element.equals("contributor") || element.equals("creator")) {
                    if (k == 0) {
                        sb.append("%A " + values[k].value);
                    } else {
                        sb.append("%A " + values[k].value);
                    }

                } else if (element.equals("relation")) {
                    if (k == 0) {
                        sb.append("%J " + values[k].value);
                    }
                    if (k == 1) {
                        sb.append("%V " + values[k].value);
                    }
                } else if (element.equals("title")) {
                    if (k == 0) {
                        sb.append("%T " + values[k].value);
                    }
                } else if (element.equals("description")) {
                    if (k == 0) {
                        sb.append("%X " + values[k].value);
                    }
                } else if (element.equals("identifier") && qualifier.equals("issn")) {
                    if (k == 0) {
                        sb.append("%@ " + values[k].value);
                    }
                } else if (element.equals("identifier") && qualifier.equals("uri")) {
                    sb.append("%U " + values[k].value);
                } else if (element.equals("subject")) {
                    sb.append("%K " + values[k].value);
                } else if (element.equals("date")) {
                    if (k == 0) {
                        //formating the Date
                        DCDate dd = new DCDate(values[k].value);
                        //String date = UIUtil.displayDate(dd, false, false).trim();
                        String date = dd.displayDate(false, false, UIUtil.getSessionLocale(request)).trim();
                        int last = date.length();
                        date = date.substring((last - 4), (last));

                        sb.append("%D " + date);
                    }
                } else {
                    if (k == 0) {
                        sb.append(qualifier + " " + values[k].value);
                    }
                }
                sb.append("\n");
            }
        }
        sb.append(ENFoot + "\n\n");

        String ENData = sb.toString();
        return ENData;
    }
    
    /*
     Autor: Christian David Criollo Lopez
     Fecha: 11-Febrero-2015
     Descripción: Metodo que se encatga de realizar la exportación de un item en formato RIS
    */
     public String renderRIS(Item item, HttpServletRequest request) {
         //create a stringbuffer for storing the metadata
        String schema = "dc";
        String RisHead="TY  - ";
        String RisFoot="ER  - ";

        //define needed metadatafields
        int risType = 1;
        String[] DC2Bib = new String[5];
        // Metadatos para articulos o articulos de revista
        DC2Bib[1] = "dc.creator, dc.title, dc.relation.ispartofseries, dc.publisher, dc.date.issued, dc.identifier.issn, dc.description.abstract, dc.subject, dc.identifier.uri";
        // Metadatos para libros o partes de libro
        DC2Bib[2] = "dc.creator, dc.contributor, dc.title, dc.publisher, dc.pubplace, dc.date.issued, dc.identifier.isbn,  dc.identifier.uri, dc.description.abstract, dc.subject";
        //Metadatos para trabajos de grado, tesis de maestria, doctorado
        DC2Bib[3] = "dc.creator, dc.contributor, dc.title, dc.publisher, dc.date.issued, dc.identifier.isbn,  dc.identifier.uri, dc.description.abstract, dc.subject";

        StringBuffer sb = new StringBuffer();

        //Parsing metadatafields
        boolean tyFound = false;

        //First needed for BibTex: dc.type
        DCValue[] types = item.getMetadata(schema, "type", Item.ANY, Item.ANY);

        for (int j = 0; (j < types.length) && !tyFound; j++) {
            String type = types[j].value;

            log.debug("DEBUG, Der gefundene Typ: " + type);//DEBUG

            if (type.equalsIgnoreCase("Article")) {
                sb.append(RisHead + "JOUR");
                risType = 1;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Book")) {
                sb.append(RisHead + "BOOK");
                risType = 2;
                tyFound = true;
            } else if (type.equalsIgnoreCase("BookChapter") || type.equalsIgnoreCase("bookPart")) {
                sb.append(RisHead + "CHAP");
                risType = 2;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Thesis")) {
                sb.append(RisHead + "THES");
                risType = 3;
                tyFound = true;
            } else if (type.equalsIgnoreCase("Preprint")) {
                sb.append(RisHead + "JOUR");
                risType = 1;
                tyFound = true;
            } else if(type.equalsIgnoreCase("bachelorThesis") || (type.equalsIgnoreCase("masterThesis") || (type.equalsIgnoreCase("doctoralThesis")))){
                sb.append(RisHead + "THES");
                risType = 3;
                tyFound = true; 
            }
        } 

        //set type in case no type is given 
        if (!tyFound) {
            sb.append(RisHead + " " + "JOUR");
            risType = 1;
        }

         sb.append(" \n");

        //Now get all the metadata needed for the requested objecttype
        StringTokenizer st = new StringTokenizer(DC2Bib[risType], ",");
            //String[] st=DC2Bib[bibType].split("\\,");

        while (st.hasMoreTokens()) {
            String field = st.nextToken().trim();
            String[] eq = field.split("\\.");
            schema = eq[0];
            String element = eq[1];
            String qualifier = Item.ANY;
            if (eq.length > 2 && eq[2].equals("*")) {
                qualifier = Item.ANY;
            } else if (eq.length > 2) {
                qualifier = eq[2];
            } else {
                qualifier = null;
            }
            //log.info("Field:"+field+";Tokens:"+eq.toString()+";Element:"+eq[1]+";Qualifier:"+qualifier);
            DCValue[] values = item.getMetadata(schema, element, qualifier, Item.ANY);
                //log.info("DCVALUES:"+values);

            //Parse the metadata into a record
            for (int k = 0; k < values.length; k++) {
                if (element.equals("contributor") || element.equals("creator")) {
                    if (k == 0) {
                        if (element.equals("creator")) {
                            sb.append("AU  - " + values[k].value);
                        } else {
                            sb.append("A2  - " + values[k].value);
                        }
                    }
                    if (k > 0) {
                        if (element.equals("creator")) {
                            sb.append("AU  - " + values[k].value);
                        } else {
                            sb.append("A2  - " + values[k].value);
                        }
                    }
                } else if (element.equals("title")) {
                    if (k == 0) {
                        sb.append("TI  - " + values[k].value);
                    }
                } else if (element.equals("description") && qualifier.equals("abstract")) {
                    if (k == 0) {
                        sb.append("AB  - " + values[k].value);
                    }
                } else if (element.equals("identifier") && qualifier.equals("issn")) {
                    if (k == 0) {
                        sb.append("SN  - " + values[k].value);
                    }
                } else if (element.equals("relation") && qualifier.equals("ispartofseries")) {
                    if (k == 0) {
                        sb.append("JO  - " + values[k].value);
                    }
                } else if (element.equals("identifier") && qualifier.equals("uri")) {
                    sb.append("UR  - " + values[k].value);
                } else if (element.equals("subject")) {
                    sb.append("KW  - " + values[k].value);
                } else if(element.equals("publisher")){
                    sb.append("PB  - " + values[k].value);
                } else if(element.equals("pubplace")){
                    sb.append("CY  - " + values[k].value);
                } else if (element.equals("date")) {
                    if (k == 0) {
                        //formating the Date
                        DCDate dd = new DCDate(values[k].value);
                        //String date = UIUtil.displayDate(dd, false, false).trim();
                        String date = dd.displayDate(false, false, UIUtil.getSessionLocale(request)).trim();
                        int last = date.length();
                        date = date.substring((last - 4), (last));

                        sb.append("PY  - " + date);
                    }
                } else {
                    if (k == 0) {
                        sb.append(qualifier + " " + values[k].value);
                    }
                  }
                  sb.append("\n");
               }     
             }
            
            sb.append(RisFoot + "\n");
            String risData= sb.toString();
            return risData;
         }
         
     
         /*
            Autor: Christian David Criollo Lopez
            Fecha: 16-Febrero-2015
            Descripción: Metodo que se encatga de realizar la exportación de un item en formato CSV
         */
         public String renderCSV(Item item, HttpServletRequest request)  {
             
          //String header= "Authors,Title,Year,Abstract,Author Keywords,Editors,Publisher,Document Type,Source";    
          //Header para articulos o articulos de revista
          String headerArticle="Authors,Title,Year,Link,Abstract,Author Keywords,Publisher,language of Original Document,Document Type";
          //Header para trabajos de grado,tesis de maestria, doctorado
          String headerThesis="Authors,Title,Year,Link,Abstract,Author Keywords,Editors,Publisher,pubplace,Language of Original Document,Document Type";
          //Header para libros o partes de libro
          String headerChapBook="Authors,Title,Year,Link,Abstract,Author Keywords,Editors,isbn,Publisher, pubplace, Document Type"; 
          
          String delimitador = ",";
          String separador = "\n";
          String schema = "dc";
          String header="";
          
          //define needed metadatafields
            int csvType = 1;
            String[] DC2Bib = new String[5];
            // Metadatos para articulos o articulos de revista
            DC2Bib[1] = "dc.creator, dc.title, dc.date.issued, dc.identifier.uri, dc.description.abstract, dc.subject, dc.publisher, dc.language.iso,dc.type";
            // Metadatos para libros o partes de libro
            DC2Bib[2] = "dc.creator, dc.title, dc.date.issued, dc.identifier.uri, dc.description.abstract, dc.subject, dc.contributor, dc.identifier.isbn, dc.publisher,dc.pubplace, dc.type";
            //Metadatos para trabajos de grado, tesis de maestria, doctorado
            DC2Bib[3] = "dc.creator, dc.title, dc.date.issued, dc.identifier.uri, dc.description.abstract, dc.subject, dc.contributor, dc.publisher, dc.pubplace, dc.language.iso, dc.type";
            
            StringBuffer sb = new StringBuffer();
            
            //Parsing metadatafields
            boolean tyFound = false;

        //First needed for BibTex: dc.type
        DCValue[] types = item.getMetadata(schema, "type", Item.ANY, Item.ANY);

        for (int j = 0; (j < types.length) && !tyFound; j++) {
            String type = types[j].value;

            //log.debug("DEBUG, Der gefundene Typ: " + type);//DEBUG

            if (type.equalsIgnoreCase("Article")) {
                csvType = 1;
                header= headerArticle;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("Book")) {
                csvType = 2;
                header= headerChapBook;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("BookChapter") || type.equalsIgnoreCase("bookPart")) {
                csvType = 2;
                header= headerChapBook;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("Thesis")) {
                csvType = 3;
                header= headerThesis;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("Preprint")) {
                csvType = 1;
                header= headerArticle;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if(type.equalsIgnoreCase("bachelorThesis") || (type.equalsIgnoreCase("masterThesis") || (type.equalsIgnoreCase("doctoralThesis")))){
                csvType = 3;
                header= headerThesis;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true; 
            }
        } 

        //set type in case no type is given 
        if (!tyFound) {
            sb.append(headerArticle.toString());
            sb.append(separador);
            csvType = 1;
        }
            //sb.append(header.toString());
            //sb.append(separador);
           
            StringTokenizer st = new StringTokenizer(DC2Bib[csvType], ",");
           
        while (st.hasMoreTokens()) {
            String field = st.nextToken().trim();
            String[] eq = field.split("\\.");
            schema = eq[0];
            String element = eq[1];
            String qualifier = Item.ANY;
            if (eq.length > 2 && eq[2].equals("*")) {
                qualifier = Item.ANY;
            } else if (eq.length > 2) {
                qualifier = eq[2];
            } else {
                qualifier = null;
            }
           
            //log.info("Field:"+field+";Tokens:"+eq.toString()+";Element:"+eq[1]+";Qualifier:"+qualifier);
            DCValue[] values = item.getMetadata(schema, element, qualifier, Item.ANY);
             // log.info("DCVALUES:"+values);
            
            //Parse the metadata into a record
            for (int k = 0; k < values.length; k++) {
                if (element.equals("creator")) {
                    if ( k==0){
                      sb.append("\""); 
                      sb.append(values[k].value);
                    }
                    else if (k > 0){
                      sb.append(values[k].value);
                    }
                     if(k == values.length-1){
                       sb.append("\"");  
                     } 
                    
                } else if (element.equals("title")) {
                    if (k == 0) {
                        sb.append("\"");
                        sb.append(values[k].value);
                        sb.append("\"");
                    }
                } else if (element.equals("date")) {
                    if (k == 0) {
                        //formating the Date
                        DCDate dd = new DCDate(values[k].value);
                        //String date = UIUtil.displayDate(dd, false, false).trim();
                        String date = dd.displayDate(false, false, UIUtil.getSessionLocale(request)).trim();
                        int last = date.length();
                        date = date.substring((last - 4), (last));
                        sb.append(date);
                     
                    }
                    else {
                    if (k == 0) {
                        sb.append(qualifier + " " + values[k].value);
                    }
                  }
                }  else if(element.equals("identifier") && qualifier.equals("uri")){
                    if(k==0){
                      sb.append("\"");  
                      sb.append(values[k].value);
                      sb.append("\"");  
                    }
                }
                  else if (element.equals("description") && qualifier.equals("abstract")) {
                    if (k == 0) {
                       sb.append("\""); 
                       sb.append(values[k].value);
                       sb.append("\""); 
                    }
                } else if (element.equals("subject")) {
                    if ( k==0){
                      sb.append("\""); 
                      sb.append(values[k].value);
                    }
                    else if (k > 0){
                      sb.append(values[k].value);
                    }
                     if(k == values.length-1){
                       sb.append("\"");  
                     }  
                } else if(element.equals("contributor")){
                   if (k == 0){
                      sb.append("\"");  
                      sb.append(values[k].value);
                      sb.append("\"");  
                    }
                } else if(element.equals("publisher")){
                   if(k==0){ 
                    sb.append("\"");
                    sb.append(values[k].value);
                    sb.append("\"");
                   }
                    
                } else if(element.equals("pubplace")){
                   if(k==0){ 
                    sb.append("\"");
                    sb.append(values[k].value);
                    sb.append("\"");
                   }
                    
                } else if (element.equals("language") && qualifier.equals("iso")) {
                    if (k == 0) {
                        sb.append("\"");
                        sb.append(values[k].value);
                        sb.append("\"");
                    }
                } else if (element.equals("type")) {
                    if (k == 0) {
                       sb.append("\""); 
                       sb.append(values[k].value);
                       sb.append("\""); 
                    }
                }
                sb.append(delimitador);
            }
          }
          String csvData= sb.toString();
          return csvData;
       }
         
          /*
            Autor: Christian David Criollo Lopez
            Fecha: 27-Febrero-2015
            Descripción: Metodo que se encatga de realizar la exportación de un item en formato TSV
         */
         public String renderTSV(Item item, HttpServletRequest request)  {
            
          //Header para articulos o articulos de revista
          String headerArticle="Authors\tTitle\tYear\tLink\tAbstract\tAuthor Keywords\tPublisher\tlanguage of Original Document\tDocument Type";
          //Header para trabajos de grado,tesis de maestria, doctorado
          String headerThesis="Authors\tTitle\tYear\tLink\tAbstract\tAuthor Keywords\tEditors\tPublisher\tpubplace\tLanguage of Original Document\tDocument Type";
          //Header para libros o partes de libro
          String headerChapBook="Authors\tTitle\tYear\tLink\tAbstract\tAuthor Keywords\tEditors\tisbn\tPublisher\tpubplace\tDocument Type"; 
          
          String delimitador = "\t";
          String separador = "\n";
          String schema = "dc";
          String header="";
          
          //define needed metadatafields
            int tsvType = 1;
            String[] DC2Bib = new String[5];
            // Metadatos para articulos o articulos de revista
            DC2Bib[1] = "dc.creator, dc.title, dc.date.issued, dc.identifier.uri, dc.description.abstract, dc.subject, dc.publisher, dc.language.iso,dc.type";
            // Metadatos para libros o partes de libro
            DC2Bib[2] = "dc.creator, dc.title, dc.date.issued, dc.identifier.uri, dc.description.abstract, dc.subject, dc.contributor, dc.identifier.isbn,dc.publisher,dc.pubplace,dc.type";
            //Metadatos para trabajos de grado, tesis de maestria, doctorado
            DC2Bib[3] = "dc.creator, dc.title, dc.date.issued, dc.identifier.uri, dc.description.abstract, dc.subject, dc.contributor, dc.publisher, dc.pubplace, dc.language.iso, dc.type";
            
            StringBuffer sb = new StringBuffer();
            
            //Parsing metadatafields
            boolean tyFound = false;

        //First needed for BibTex: dc.type
        DCValue[] types = item.getMetadata(schema, "type", Item.ANY, Item.ANY);

        for (int j = 0; (j < types.length) && !tyFound; j++) {
            String type = types[j].value;

            //log.debug("DEBUG, Der gefundene Typ: " + type);//DEBUG

            if (type.equalsIgnoreCase("Article")) {
                tsvType = 1;
                header= headerArticle;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("Book")) {
                tsvType = 2;
                header= headerChapBook;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("BookChapter") || type.equalsIgnoreCase("bookPart")) {
                tsvType = 2;
                header= headerChapBook;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("Thesis")) {
                tsvType = 3;
                header= headerThesis;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if (type.equalsIgnoreCase("Preprint")) {
                tsvType = 1;
                header= headerArticle;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true;
            } else if(type.equalsIgnoreCase("bachelorThesis") || (type.equalsIgnoreCase("masterThesis") || (type.equalsIgnoreCase("doctoralThesis")))){
                tsvType = 3;
                header= headerThesis;
                sb.append(header.toString());
                sb.append(separador);
                tyFound = true; 
            }
        } 

        //set type in case no type is given 
        if (!tyFound) {
            sb.append(headerArticle.toString());
            sb.append(separador);
            tsvType = 1;
        }
            //sb.append(header.toString());
            //sb.append(separador);
           
            StringTokenizer st = new StringTokenizer(DC2Bib[tsvType], ",");
           
        while (st.hasMoreTokens()) {
            String field = st.nextToken().trim();
            String[] eq = field.split("\\.");
            schema = eq[0];
            String element = eq[1];
            String qualifier = Item.ANY;
            if (eq.length > 2 && eq[2].equals("*")) {
                qualifier = Item.ANY;
            } else if (eq.length > 2) {
                qualifier = eq[2];
            } else {
                qualifier = null;
            }
           
            //log.info("Field:"+field+";Tokens:"+eq.toString()+";Element:"+eq[1]+";Qualifier:"+qualifier);
            DCValue[] values = item.getMetadata(schema, element, qualifier, Item.ANY);
             // log.info("DCVALUES:"+values);
            
            //Parse the metadata into a record
            for (int k = 0; k < values.length; k++) {
                if (element.equals("creator")) {
                    if ( k==0){
                      sb.append("\""); 
                      sb.append(values[k].value);
                    }
                    else if (k > 0){
                      sb.append(values[k].value);
                    }
                     if(k == values.length-1){
                       sb.append("\"");  
                     } 
                    
                } else if (element.equals("title")) {
                    if (k == 0) {
                        sb.append("\"");
                        sb.append(values[k].value);
                        sb.append("\"");
                    }
                } else if (element.equals("date")) {
                    if (k == 0) {
                        //formating the Date
                        DCDate dd = new DCDate(values[k].value);
                        //String date = UIUtil.displayDate(dd, false, false).trim();
                        String date = dd.displayDate(false, false, UIUtil.getSessionLocale(request)).trim();
                        int last = date.length();
                        date = date.substring((last - 4), (last));
                        sb.append(date);
                     
                    }
                    else {
                    if (k == 0) {
                        sb.append(qualifier + " " + values[k].value);
                    }
                  }
                }  else if(element.equals("identifier") && qualifier.equals("uri")){
                    if(k==0){
                      sb.append("\"");
                      sb.append(values[k].value);
                      sb.append("\"");
                    }
                }
                  else if (element.equals("description") && qualifier.equals("abstract")) {
                    if (k == 0) {
                       sb.append("\""); 
                       sb.append(values[k].value);
                       sb.append("\"");
                    }
                } else if (element.equals("subject")) {
                    if ( k==0){
                      sb.append("\""); 
                      sb.append(values[k].value);
                    }
                    else if (k > 0){
                      sb.append(values[k].value);
                    }
                     if(k == values.length-1){
                       sb.append("\"");  
                     }  
                } else if(element.equals("contributor")){
                   if (k == 0){
                      sb.append("\"");
                      sb.append(values[k].value);
                      sb.append("\"");
                    }
                } else if(element.equals("publisher")){
                   if(k==0){
                    sb.append("\"");   
                    sb.append(values[k].value);
                    sb.append("\"");
                   }
                    
                } else if(element.equals("pubplace")){
                   if(k==0){
                    sb.append("\"");  
                    sb.append(values[k].value);
                    sb.append("\"");
                   }
                    
                } else if (element.equals("language") && qualifier.equals("iso")) {
                    if (k == 0) {
                        sb.append("\"");
                        sb.append(values[k].value);
                        sb.append("\"");
                    }
                } else if (element.equals("type")) {
                    if (k == 0) {
                       sb.append(values[k].value);
                    }
                }
                sb.append(delimitador);
            }
          }
          String tsvData= sb.toString();
          return tsvData;
       }
    }
