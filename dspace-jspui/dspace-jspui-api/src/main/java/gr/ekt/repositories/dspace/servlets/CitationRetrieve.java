/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package gr.ekt.repositories.dspace.servlets;

import gr.ekt.repositories.dspace.utils.CitationFormat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dspace.content.Item;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.html.simpleparser.StyleSheet;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.codec.Base64;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet implementation class CitationRetrieve
 */
public class CitationRetrieve extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CitationRetrieve() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String format = (String) request.getParameter("format");
		
		String outputFormat = (String) request.getParameter("submit_button");
                String citeOutputFormat = "";
		if(outputFormat.equals("text"))
			citeOutputFormat = "text";
		if(outputFormat.equals("html") || outputFormat.equals("word") || outputFormat.equals("pdf"))
			citeOutputFormat = "html";
		String responseFormat = outputFormat;
		
		HttpSession sessionResults = request.getSession();
		Item sessionItem = (Item) sessionResults.getAttribute("item");
		Item[] sessionItems = (Item[]) sessionResults.getAttribute("items");

		if(format.equals("")){
			format = "ieee";
		}
		
		if(outputFormat.equals("html")){
			response.setContentType("text/html; charset=UTF-8");
			response.setHeader("Content-Disposition","attachment; filename=" + format + "Export.html");
		}
		else if(outputFormat.equals("text")){
			if(format.equals("bibtex")){
				response.setContentType("plain/text; charset=UTF-8");
				response.setHeader("Content-Disposition","attachment; filename=" + format + "Export.bib");	
			}
			else if(format.equals("ris")){
				response.setContentType("plain/text; charset=UTF-8");
				response.setHeader("Content-Disposition","attachment; filename=" + format + "Export.ris");	
			}
			else{
				response.setContentType("plain/text; charset=UTF-8");
				response.setHeader("Content-Disposition","attachment; filename=" + format + "Export.txt");	
			}
		}
                else if(outputFormat.equals("word")){
                    response.setContentType("application/msword");
                    response.setHeader("Content-Disposition","attachment; filename=" + format + ".doc");
                }
                else if(outputFormat.equals("pdf")){
                    response.setContentType("application/pdf; charset=UTF-8");
                    response.setHeader("Content-Disposition","attachment; filename=" + format + ".pdf");
                }
                
                CitationFormat ct = null;
                try {			
			if(sessionItem != null)
				ct = new CitationFormat(sessionItem, format, citeOutputFormat, citeOutputFormat);
			if(sessionItems != null)
				ct = new CitationFormat(sessionItems, format, citeOutputFormat, citeOutputFormat);
			
                        ct.postToCiteProc();
                        
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		if(outputFormat.equals("html")){
                    Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
                    writer.write("<html><head><meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\"/></head><body>");
                    writer.write(ct.getExport().replaceAll("LINEBREAK", "\r\n"));
                    writer.write("</body></html>");
                    writer.flush();
                    writer.close();
		}
                
                else if(outputFormat.equals("word")){
                                        
                    Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
                    writer.write("<html><head><meta content=\"application/msword; charset=UTF-8\" http-equiv=\"Content-Type\"/></head><body>");		
                    String exportStr = ct.getExport();
                   
                    writer.write(exportStr.replaceAll("LINEBREAK", "\r\n"));                    
                    writer.write("</body></html>");
                    writer.flush();
                    writer.close();
		}
                
                else if(outputFormat.equals("text")){
                    Writer writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
                    writer.write(ct.getExport().replaceAll("LINEBREAK", "\r\n"));
                    writer.flush();
                    writer.close();
		}
                
                else if(outputFormat.equals("pdf")){
                    String exportStr = ct.getExport();
                    try {
                        Document pdfDocument = new Document();                                                
                        StringReader htmlreader = new StringReader(exportStr);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PdfWriter.getInstance(pdfDocument,response.getOutputStream());
                        
                        pdfDocument.open();
                        
                        FontFactory.register(sessionResults.getServletContext().getRealPath("/WEB-INF/")+"/arialuni.ttf");
                        StyleSheet styles = new StyleSheet();
                        styles.loadTagStyle("body", "font", "Arial Unicode MS");
                        styles.loadTagStyle("body", "face", "Arial Unicode MS");
                        styles.loadTagStyle("body", "encoding", BaseFont.IDENTITY_H);
                        styles.loadTagStyle("body", "leading", "12,0");
                        ArrayList arrayElementList = HTMLWorker.parseToList(htmlreader, styles);
                        
                        for (int i = 0; i < arrayElementList.size(); ++i) {
                            Element e = (Element) arrayElementList.get(i);
                            pdfDocument.add(e);
                        }
                        pdfDocument.close();
                        
                    } catch (DocumentException ex) {
                        Logger.getLogger(CitationRetrieve.class.getName()).log(Level.SEVERE, null, ex);
                    }
		}
		

	}

}