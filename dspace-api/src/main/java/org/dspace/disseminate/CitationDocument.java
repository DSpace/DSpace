/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.bouncycastle.crypto.CryptoException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;

import java.awt.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;


/**
 * The Citation Document produces a dissemination package (DIP) that is different that the archival package (AIP).
 * In this case we append the descriptive metadata to the end (configurable) of the document. i.e. last page of PDF.
 * So instead of getting the original PDF, you get a cPDF (with citation information added).
 *
 * @author Peter Dietz (peter@longsight.com)
 * @author riccardo fazio
 */
public class CitationDocument {
    /**
     * Class Logger
     */
    private static Logger log = Logger.getLogger(CitationDocument.class);

    private static File tempDir;

    private String font;
    private String fontSize;
    private String header1;
    private String header2;
    private String[] fields;
    private String footer;

    private int ygap = 15;
    private int xpos;
    private int ypos;
    private String xinit;
    private String yinit;

    private final String IMG_SEPARATOR ="###";

    private float cellMargin=2f;
    
    private String ownerpassword = "";
    private boolean copysecurity = false;


    public CitationDocument() {}

    public CitationDocument(String configuration) {
        //Load enabled collections

        font = ConfigurationManager.getProperty(configuration,"font");
        fontSize = ConfigurationManager.getProperty(configuration,"fontSize");
        // Configurable text/fields, we'll set sane defaults
        header1 = ConfigurationManager.getProperty(configuration, "header1");

        header2 = ConfigurationManager.getProperty(configuration, "header2");

        xinit= ConfigurationManager.getProperty(configuration, "xstartposition");
        yinit= ConfigurationManager.getProperty(configuration, "ystartposition");
                    
        ypos = StringUtils.isNotBlank(yinit)?Integer.parseInt(yinit):730;
        xpos = StringUtils.isNotBlank(xinit)?Integer.parseInt(xinit):30;

        String fieldsConfig = ConfigurationManager.getProperty(configuration, "fields");
        if(StringUtils.isNotBlank(fieldsConfig)) {
            fields = fieldsConfig.split(",");
        } else {
            fields = new String[]{"dc.date.issued", "dc.title", "dc.creator", "dc.contributor.author", "dc.publisher",  "dc.identifier.citation", "dc.identifier.uri"};
        }

        footer = ConfigurationManager.getProperty(configuration, "footer");
        

        //Ensure a temp directory is available
        String tempDirString = ConfigurationManager.getProperty("dspace.dir") + "/temp";
        tempDir = new File(tempDirString);
        if(!tempDir.exists()) {
            boolean success = tempDir.mkdir();
            if(success) {
                log.info("Created temp directory at: " + tempDirString);
            } else {
                log.info("Unable to create temp directory at: " + tempDirString);
            }
        }
        
        ownerpassword = ConfigurationManager.getProperty(configuration, "ownerpassword");
        copysecurity = ConfigurationManager.getBooleanProperty(configuration, "copysecurity");
    }


    private Boolean citationAsFirstPage = null;

    private Boolean isCitationFirstPage(String configuration) {
        if(citationAsFirstPage == null) {
            citationAsFirstPage = ConfigurationManager.getBooleanProperty(configuration, "citation_as_first_page", true);
        }

        return citationAsFirstPage;
    }


    public File makeCitedDocument(Bitstream bitstream)
            throws IOException, SQLException, AuthorizeException, COSVisitorException {
    	return makeCitedDocument(null, bitstream,"disseminate-citation");
    }

    /**
     * Creates a
     * cited document from the given bitstream of the given item. This
     * requires that bitstream is contained in item.
     * <p>
     * The Process for adding a cover page is as follows:
     * <ol>
     *  <li> Load source file into PdfReader and create a
     *     Document to put our cover page into.</li>
     *  <li> Create cover page and add content to it.</li>
     *  <li> Concatenate the coverpage and the source
     *     document.</li>
     * </p>
     *
     * @param bitstream The source bitstream being cited. This must be a PDF.
     * @return The temporary File that is the finished, cited document.
     * @throws java.io.FileNotFoundException
     * @throws SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public File makeCitedDocument(Bitstream bitstream,String configuration)
            throws IOException, SQLException, AuthorizeException, COSVisitorException {
    	return makeCitedDocument(null, bitstream,configuration);
    }
    
    public File makeCitedDocument(Context context,Bitstream bitstream,String configuration)
            throws IOException, SQLException, AuthorizeException, COSVisitorException {
        PDDocument document = new PDDocument();
        PDDocument sourceDocument = new PDDocument();
        String filePath =tempDir.getAbsolutePath() + "/bitstream.cover.pdf";
        String password=null;
        try {
            Item item = (Item) bitstream.getParentObject();
            sourceDocument = sourceDocument.load(bitstream.retrieve());
            if(sourceDocument.isEncrypted()) {
            	password=ownerpassword;
            	sourceDocument.decrypt(password);
            	if(sourceDocument.isEncrypted()) {
            		//try with a blank password
            		password= "";
            		sourceDocument.decrypt(password);
            		if(sourceDocument.isEncrypted()) {
            			throw new CryptoException("Cannot Decrypt");
            		}
            	}
            }

            if(copysecurity && password!= null) {
            	AccessPermission sourceAc = sourceDocument.getCurrentAccessPermission();
            	int keyLength = 128;
            	if(sourceDocument.getEncryptionDictionary() != null) {
            		keyLength = sourceDocument.getEncryptionDictionary().getLength();
            	}
            	//at this stage EncryptionDictionary should not be null 
            	
            	StandardProtectionPolicy spp = new StandardProtectionPolicy(password, "", sourceAc);
            	spp.setEncryptionKeyLength(keyLength);
            	
            	document.protect(spp);
            	
            }
            
            PDPage coverPage = new PDPage(PDPage.PAGE_SIZE_LETTER);
            generateCoverPage(context,document, coverPage, item,configuration);
            addCoverPageToDocument(document, sourceDocument, coverPage,configuration);

            document.save(filePath);
            
        
        }catch(Exception e){
        	log.error(e.getMessage(), e);
    		filePath = BitstreamStorageManager.absolutePath(context, bitstream.getID());
        }
        finally {
            sourceDocument.close();
            document.close();
        }
        return new File(filePath);
    }

    private void generateCoverPage(Context context,PDDocument document, PDPage coverPage, Item item,String configuration) throws IOException, COSVisitorException {
        PDPageContentStream contentStream = new PDPageContentStream(document, coverPage);
        try {

            Locale currLocale = null;
            if(context!= null) {
            	currLocale = context.getCurrentLocale();
            }
            
            

            PDFont pdFont = PDType1Font.HELVETICA;
            if(StringUtils.isNotBlank(font)){
            	if(StringUtils.equalsIgnoreCase(font, "times")){
            		pdFont = PDType1Font.TIMES_ROMAN;
            	}else if(StringUtils.equalsIgnoreCase(font, "courier")){
            		pdFont = PDType1Font.COURIER;
            	}
            }

            contentStream.setNonStrokingColor(Color.BLACK);

            int size = StringUtils.isNotBlank(fontSize)? Integer.parseInt(fontSize):10;

//---            rowHeight = 1.2f*size*3;
            if(StringUtils.isNotBlank(header1)){
            	ypos -=(ygap);
            	String text = StringUtils.replace(header1, "[[date]]", DCDate.getCurrent().toString());
            	ypos= drawStringImageWordWrap(document, coverPage, contentStream, text, xpos, ypos, pdFont, size,item,configuration);
            }

            if(StringUtils.isNotBlank(header2)){
            	String text = StringUtils.replace(header2, "[[date]]", DCDate.getCurrent().toString());
            	ypos= drawStringImageWordWrap(document, coverPage, contentStream, text, xpos, ypos, pdFont, size,item,configuration);
            }

           List<String[]> labelValues = new ArrayList<String[]>();
            int x=0;
            for(String field : fields) {
            	String[] row= StringUtils.split(field,"::");
            	String label="";
            	String valueMetadata="";
            	String value ="";
            	if(row != null && row.length==2){
            		label=row[0];
            		valueMetadata = row[1].trim();
            	}else if(row != null){
            		valueMetadata = row[0].trim();
                }

            	if(StringUtils.isNotBlank(valueMetadata) && StringUtils.equals(valueMetadata, "[[citation]]")){
					label = I18nUtil.getMessage("metadata.coverpage.citation",
							currLocale, false);
            		value=makeCitation(item);
            	}else{
            		label = I18nUtil.getMessage("metadata.coverpage." + valueMetadata,
							currLocale, false);
            		Metadatum[] meta = item.getMetadataByMetadataString(valueMetadata);
            		if(meta != null){
	            		for(int z=0;z<meta.length;z++){
	            			if(z>0) {
	            				value+="; ";
	            			}
	            			value += meta[z].value;

                }
            		}
				}
             	if(StringUtils.isNotBlank(value)){
            		String[] ar = new String[2];
            		ar[0] = label;
            		ar[1]= value;
            		labelValues.add(ar);
            	}

            	x++;
            	
                }
            drawTable(coverPage, contentStream, ypos, xpos, labelValues, pdFont, size, true);
            ypos-=(42*x);
            
            if(StringUtils.isNotBlank(footer)){
            	ypos -=(ygap);
            	String text = StringUtils.replace(footer, "[[date]]", DCDate.getCurrent().toString());
            	ypos= drawStringImageWordWrap(document, coverPage, contentStream, text, xpos, ypos, pdFont, size,item,configuration);
            }

        } finally {
            contentStream.close();
        }
    }

    private void addCoverPageToDocument(PDDocument document, PDDocument sourceDocument, PDPage coverPage,String configuration) throws IOException {
        List<PDPage> sourcePageList = sourceDocument.getDocumentCatalog().getAllPages();
        if(sourcePageList.size()==0) {
        	throw new IOException();

        }
        if (isCitationFirstPage(configuration)) {
            //citation as cover page
            document.addPage(coverPage);
            for (PDPage sourcePage : sourcePageList) {
                document.addPage(sourcePage);
            }
        } else {
            //citation as tail page
            for (PDPage sourcePage : sourcePageList) {
                document.addPage(sourcePage);
            }
            document.addPage(coverPage);
        }
        sourcePageList.clear();
    }

    public int drawStringImageWordWrap(PDDocument document, PDPage page, PDPageContentStream contentStream, String text,
            int startX, int startY, PDFont pdfFont, float fontSize,Item item,String configuration) throws IOException {
    	String[] head = StringUtils.split(text,IMG_SEPARATOR);
    	for(String h: head){
    		String imgProp = ConfigurationManager.getProperty(configuration,"img."+h);
    		
    		if(StringUtils.isNotBlank(imgProp) ){
    			File file = new File(imgProp);
    			if(file!= null && file.exists()){

                	InputStream inImg = new FileInputStream(file);
                	
                	PDXObjectImage poi = new PDJpeg(document,inImg);
                	contentStream.drawImage(poi, startX, startY);
                	startY -=(ygap);
                	inImg.close();
    			}

    		}else{
    			startY = drawStringWordWrap(page,contentStream,h,startX,startY,pdfFont,11);
    			startY -=(ygap);
    			
    		}
    	}
    	return startY;
    }
    
    
    public int drawStringWordWrap(PDPage page, PDPageContentStream contentStream, String text,
                                    int startX, int startY, PDFont pdfFont, float fontSize) throws IOException {
        float leading = 1.5f * fontSize;

        PDRectangle mediabox = page.findMediaBox();
        float margin = 72;
        float width = mediabox.getWidth() - 2*margin;

        List<String> lines = new ArrayList<>();
    	String[] pieces = StringUtils.split(text, " ");
    	String str ="";
    	for(int z=0;z<pieces.length;z++) {
    		if(z>0) {
    			str+=" ";
    		}
    		String piece = str+pieces[z];
    		float size = fontSize * pdfFont.getStringWidth(piece) / 1000;
    		if(size > width) {
    			lines.add(str);
    			str=pieces[z];
    		}else {
    			str=piece;
    		}
    	}
    	lines.add(str);

        contentStream.beginText();
        contentStream.setFont(pdfFont, fontSize);
        contentStream.moveTextPositionByAmount(startX, startY);
        int currentY = startY;
        for (String line: lines)
        {
            contentStream.drawString(line);
            currentY -= leading;
            contentStream.moveTextPositionByAmount(0, -leading);
            }
        contentStream.endText();
        return currentY;
                }

    public int drawStringCellWordWrap(PDPage page, PDPageContentStream contentStream, String text,
            int startX, int startY,float cellWidth, PDFont pdfFont, float fontSize,boolean hasBackground) throws IOException {
    	float leading = 1.5f * fontSize;

    	List<String> lines = new ArrayList<>();
    	
    	String[] pieces = StringUtils.split(text, " ");
    	String str ="";
    	for(int z=0;z<pieces.length;z++) {
    		if(z>0) {
    			str+=" ";
                }
    		String piece = str+pieces[z];
    		float size = fontSize * pdfFont.getStringWidth(piece) / 1000;
    		if(size > cellWidth) {
    			lines.add(str);
    			str=pieces[z];
    		}else {
    			str=piece;
            }
        }
    	lines.add(str);

        contentStream.beginText();
        contentStream.setFont(pdfFont, fontSize);
        contentStream.moveTextPositionByAmount(startX, startY);
        int currentY = startY;
        for (String line: lines)
        {
            contentStream.drawString(line);
            currentY -= leading;
            contentStream.moveTextPositionByAmount(0, -leading);
        }
        contentStream.endText();
    	
    	if(hasBackground) {
	    	float yrect = startY;
	    	contentStream.setNonStrokingColor(220, 220, 220); //gray background
	    	contentStream.fillRect(xpos,currentY,cellWidth -261  , fontSize+startY-currentY);
	    	contentStream.setNonStrokingColor(0, 0, 0);
    	}
        return currentY;
    }

    public String getOwningCommunity(Item item) {
        try {
            Community[] comms = item.getCommunities();
            if(comms.length > 0) {
                return comms[0].getName();
            } else {
                return " ";
            }

        } catch (SQLException e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }

    public String getOwningCollection(Item item) {
        try {
            return item.getOwningCollection().getName();
        } catch (SQLException e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }

    public String getAllMetadataSeparated(Item item, String metadataKey) {
        Metadatum[] Metadatums = item.getMetadataByMetadataString(metadataKey);

        ArrayList<String> valueArray = new ArrayList<String>();

        for(Metadatum Metadatum : Metadatums) {
            if(StringUtils.isNotBlank(Metadatum.value)) {
                valueArray.add(Metadatum.value);
            }
        }

        return StringUtils.join(valueArray.toArray(), "; ");
    }

    /**
     * @param page
     * @param contentStream
     * @param y the y-coordinate of the first row
     * @param margin the padding on left and right of table
     * @param content a 2d array containing the table data
     * @throws IOException
     */
    public void drawTable(PDPage page, PDPageContentStream contentStream,
                                 float y, float margin,
                                 List<String[]> content, PDFont font, int fontSize, boolean cellBorders) throws IOException {
        final int rows = content.size();
        final int cols = 2;
        final float rowHeight = 42f;
        final float tableWidth = page.findMediaBox().getWidth()-(2*margin);
        final float colWidth = tableWidth/(float)cols;


        float textx = margin+cellMargin;
        float texty = y-(1.2f*fontSize);
        float celly= y-rowHeight;
        
        contentStream.drawLine(margin,y,margin+tableWidth,y);
        for(String[] c : content){
        	contentStream.setFont(font, fontSize);
            String label = c[0];
            String val = c[1];
            
            textx += colWidth-130;
        	contentStream.setFont(font, fontSize);
    		contentStream.setNonStrokingColor(0, 0, 0);
    		int ty= drawStringCellWordWrap(page, contentStream, val, (int)textx, (int)texty,colWidth+130, font, fontSize,true);
    		

        	drawStringCellWordWrap(page, contentStream, label, (int)(margin+cellMargin), (int)texty,colWidth-130, PDType1Font.HELVETICA_BOLD_OBLIQUE, fontSize,false);
    		
    		textx += colWidth+130;
    		contentStream.drawLine(margin,ty-1,margin+tableWidth,ty-1);
    		
            celly =ty;
            texty =ty- (1.2f*fontSize);
            textx = margin+cellMargin;
        }
        
        if(cellBorders) {

            //draw the columns
            float nextx = margin;
            for (int i = 0; i <= cols; i++) {
                contentStream.drawLine(nextx,y,nextx,celly);
                if(i%2==0){
                	 nextx+= colWidth-130;	
                }else{
                	nextx += colWidth+130;
                }
                
            }
            }
        }

    private String makeCitation(Item item){
    	String citation ="";
    	String type=item.getMetadata("dc.type");
        CoverpageCitationCrosswalk coverpageCrosswalk = null;

        if (type != null)
        {
            coverpageCrosswalk = (CoverpageCitationCrosswalk) PluginManager
                    .getNamedPlugin(CoverpageCitationCrosswalk.class,
                            type);
            }
        
        if(coverpageCrosswalk != null){
        	citation = coverpageCrosswalk.makeCitation(item);
        }
        
    	return citation;
    }
}
