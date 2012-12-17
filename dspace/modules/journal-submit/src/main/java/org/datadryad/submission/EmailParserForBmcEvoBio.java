package org.datadryad.submission;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * The Class EmailParserForBmcEvoBio.
 * Modified from EmailParserForManuscriptCentral by pmidford 7 July 2011
 */
public class EmailParserForBmcEvoBio extends EmailParser {
    
    // static block

    /** The Pattern for email field. */
    static Pattern Pattern4EmailField = Pattern.compile("^[^:]+:");
    
    /** The Pattern for nonbreaking space. */
    static Pattern Pattern4NonbreakingSpace = Pattern.compile("\\u00A0");
    
    /** The Pattern for newline chars. */
    static Pattern Pattern4NewlineChars = Pattern.compile("\\n+$");
        
    /** The list of the child tag names under Submission_Metadata tag. */
    static List<String> xmlTagNameMetaSubList;
    
    /** The list of the child tag names under Authors tag. */
    static List<String> xmlTagNameAuthorSubList;
    
    /** The field to XML-tag mapping table. */
    static Map<String, String> fieldToXMLTagTable = 
        new LinkedHashMap<String,String>();
    
    /** The Pattern for sender email address. */
    static Pattern Pattern4SenderEmailAddress =
        Pattern.compile("(\"[^\"]*\"|)\\s*(<|)([^@]+@[^@>]+)(>|)");
    
    /** The pattern for separator lines */
    static Pattern Pattern4separatorLine = Pattern
        .compile("^(-|\\+|\\*|/|=|_){2,}+");

    /** The list of mail fields to be excluded */
    static List<String> tagsTobeExcluded; 
    
    /** The set of mail fields to be excluded */
    static Set<String> tagsTobeExcludedSet; 
    
    static {
    	
      	fieldToXMLTagTable.put("Journal Name","Journal");
	fieldToXMLTagTable.put("Print ISSN","ISSN");
	fieldToXMLTagTable.put("Online ISSN","Online_ISSN");
	fieldToXMLTagTable.put("Journal Admin Email","Journal_Admin_Email");
	fieldToXMLTagTable.put("Journal Editor","Journal_Editor");
	fieldToXMLTagTable.put("Journal Editor Email", "Journal_Editor_Email");
	fieldToXMLTagTable.put("Journal Embargo Period", "Journal_Embargo_Period");
	fieldToXMLTagTable.put("MS Dryad ID","Manuscript");
	fieldToXMLTagTable.put("MS Reference Number","Manuscript");
	fieldToXMLTagTable.put("MS reference Number","Manuscript");
	fieldToXMLTagTable.put("MS Title","Article_Title");
	fieldToXMLTagTable.put("MS Authors","Authors");
	fieldToXMLTagTable.put("Contact Author","Corresponding_Author");
	fieldToXMLTagTable.put("Contact Author Email","Email");
	fieldToXMLTagTable.put("Contact Author Address 1","Address_Line_1");
	fieldToXMLTagTable.put("Contact Author Address 2","Address_Line_2");
	fieldToXMLTagTable.put("Contact Author Address 3","Address_Line_3");
	fieldToXMLTagTable.put("Contact Author City","City");
	fieldToXMLTagTable.put("Contact Author State","State");
	fieldToXMLTagTable.put("Contact Author Country","Country");
	fieldToXMLTagTable.put("Contact Author ZIP/Postal Code","Zip");
	fieldToXMLTagTable.put("Keywords","Classification");
	fieldToXMLTagTable.put("Abstract","Abstract");
	fieldToXMLTagTable.put("Article Status","Article_Status");
	
        	
        xmlTagNameAuthorSubList= Arrays.asList(
            "Corresponding_Author",
            "Email",
            "Address_Line_1",
            "Address_Line_2",
            "Address_Line_3",
            "City",
            "State",
            "Country",
            "Zip"
        );
        xmlTagNameMetaSubList= Arrays.asList(
                "Article_Title");
                
        tagsTobeExcluded = Arrays.asList(
            "MS Reference Number",
            "Dryad author url"
        );
        
        tagsTobeExcludedSet = new LinkedHashSet<String>(tagsTobeExcluded);
    }
        
    /** The logger setting. */
    private static Logger LOGGER =
        LoggerFactory.getLogger(EmailParserForManuscriptCentral.class);

    
    
    /**
     * Parses each String stored in a List and returns its results as a
     * ParsingResult object.
     * 
     * @param message the message
     * 
     * @return the parsing result
     * 
     * @see submit.util.EmailParser#parseEmailMessage(java.util.List)
     */
    public ParsingResult parseMessage(List<String>message) {
            
        LOGGER.trace("***** start of parseEmailMessage() *****");

        int lineCounter = 0;
        String fieldName = null;
        String previousField = null;
        
        String StoredLines = "";
        Map<String, String> dataForXml = new LinkedHashMap<String, String>();
        ParsingResult result = new ParsingResult();

        // Scan each line
        parsingBlock:
        for  (String line : message){

            lineCounter++;
            LOGGER.trace(" raw line="+line);
            
            // match field names
            Matcher matcher = Pattern4EmailField.matcher(line);
            // note: url datum ("http:") is not a field token
            if (matcher.find() && !line.startsWith("http")){
                // field candidate is found
                String matchedField = matcher.toMatchResult().group(0);
                
                // remove the separator (":") from this field
                int colonPosition = matchedField.indexOf(":");
                fieldName = matchedField.substring(0, colonPosition);

                // get the value of this field excluding ":"
                String fieldValue = line.substring(colonPosition+1).trim();
                
                // processing block applicable only for the first line of
                // a new e-mail message
                if (fieldName.equalsIgnoreCase("From")){
                    Matcher me =
                        Pattern4SenderEmailAddress.matcher(fieldValue);
                    if (me.find()){
                        LOGGER.trace("how many groups="+me.groupCount());
                        LOGGER.trace("email address captured:"+me.group(3));
                        result.setSenderEmailAddress(me.group(3));
                    }
                } 
                
                if (!fieldToXMLTagTable.containsKey(fieldName)) {
                    // this field or field-look-like is not saved
                    LOGGER.trace(fieldName + " does not belong to the inclusion-"
                        + "tag-set");
                    if (!tagsTobeExcludedSet.contains(fieldName)) {
                        StoredLines = StoredLines +" "+ line; 
                        LOGGER.trace("new stored line=" + StoredLines);
                        // The field name that was matched will not be used,
                        // reset it to the previous field since we are storing
                        // the entire line
                        fieldName = previousField;
                    } else {
                        LOGGER.trace("\t*** line [" + line + "] is skipped");
                    }
                
                } else {
                    // this field is to be saved

                    // new field is detected; if stored lines exist,
                    // they should be saved now
                    if (!StoredLines.equals("")){
                        // continuous lines were stored for the previous 
                        // field before
                        // save these store lines for the last field
                        if (fieldToXMLTagTable.containsKey(previousField)){
                        	if (previousField.equals("MS Authors")) {
                        		String prevName = fieldToXMLTagTable.get(previousField);
                        		
                       			if (dataForXml.containsKey(prevName)) {
                       				// We ignore first setting b/c it's contained in StoredLines
                       				dataForXml.put(prevName, StoredLines);
                        		}
                        		else {
                        			dataForXml.put(prevName, StoredLines);
                        		}
                        	}
                        	else {
                        		dataForXml.put(fieldToXMLTagTable.get(previousField),
                        				StoredLines);
                        	}

                            LOGGER.trace("lastField to be saved="+previousField);
                            LOGGER.trace("its value="+StoredLines);
                        }
                        // clear the line-storage object
                        StoredLines = "";
                    }
                    
                    if (fieldName.equalsIgnoreCase("Abstract")) {
                        LOGGER.info("reached last parsing field: " + fieldName);
                    }
               
                    LOGGER.trace(fieldToXMLTagTable.get(fieldName)+
                            "="+fieldValue);
                    
                    // if the field is an ID field assign it as the ID for the result object
                    // (the last ID processed will be the ID of the parsed item)
                    if (fieldName.equalsIgnoreCase("MS Dryad ID") || fieldName.equalsIgnoreCase("MS Reference Number")){
                        Matcher mid = Pattern4MS_Dryad_ID.matcher(fieldValue);
                        if (mid.find()){
                            result.setSubmissionId(mid.group(0));
                            LOGGER.info("submissionId="+result.getSubmissionId());
                            
                            if (fieldValue.equals(result.getSubmissionId())){
                                LOGGER.trace("value and ID are the same");
                            } else {
                                LOGGER.warn("fieldvalue=["+fieldValue+"]"+
                                    "\tid="+result.getSubmissionId()+" differ");
                                result.setHasFlawedId(true);
                            }
                        }
                    }
                    
                    // save the data of this field
                    Matcher m2 = Pattern4NonbreakingSpace.matcher(fieldValue);
                    if (m2.find()){
                        LOGGER.trace("nbsp was found at:"+lineCounter);
                        fieldValue = fieldValue.replaceAll("\\u00A0", " ");
                        LOGGER.trace("fieldValue="+fieldValue);
                    }
                    // tentatively save the data of this field
                    // more lines may follow ...
                    dataForXml.put(fieldToXMLTagTable.get(fieldName),
                        fieldValue);
                    StoredLines = fieldValue;
                    LOGGER.trace("tentatively saved value so far:" + StoredLines);
                }
            } else {
                // no colon-separated field matched
                // non-1st lines or blank lines
                // append this line to the storage object (StoredLines)
                
                if ((line != null) ){
                    
                    if (!StoredLines.trim().equals("")){
                        Matcher m3 = Pattern4separatorLine.matcher(line);
                        if (m3.find()) {
                            LOGGER.info("separator line was found; ignore this.");
                            
                            if ((fieldName != null)
                                && (fieldName.equalsIgnoreCase("Abstract"))) {
                                break parsingBlock;
                            }
                        } else {
                        	if (previousField != null && previousField.equals("MS Authors")) {
                        		StoredLines = StoredLines + ";" + line;
                        	}
                        	else {
                        		StoredLines = StoredLines + "\n" + line;
                        	}
                            LOGGER.trace("StoredLines=" + StoredLines);
                        }
                    } else {
                    	if (previousField != null && previousField.equals("MS Authors")) {
                    		StoredLines += (";" + line);
                    	}
                    	else {
                    		StoredLines +=line;
                    	}
                        LOGGER.trace("StoredLines['' case]=" + StoredLines);
                    }
                } 
            }

            previousField = fieldName;            
        }  // end of for
        
        // Exit-processing: if the last matched field is ABSTRACT, 
        // its data are not saved and they must be saved here
        if (previousField.equalsIgnoreCase("Abstract")){
            dataForXml.put(fieldToXMLTagTable.get("Abstract"), 
                StoredLines);
        }
            
		LOGGER.trace("***** end of parseEmailMessage() *****");
        result.setSubmissionData(BuildSubmissionDataAsXML(dataForXml));

        return result;
    }
    


    /**
     * Write submission data as xml.
     * 
     * TODO: rewrite this so we're not constructing xml by appending strings!
     * 
     * @param emailData the email data
     * 
     * @return the string builder
     */
    StringBuilder BuildSubmissionDataAsXML(Map<String, String> emailData){
        // this method writes data as XML at once from a String
        
        StringBuilder sb = new StringBuilder();
        Set<Map.Entry<String, String>> keyvalue = emailData.entrySet();
        
        for (Iterator<Map.Entry<String, String>> it = 
                keyvalue.iterator();it.hasNext(); ){
            Map.Entry<String, String> et = it.next();
            
            if (et.getKey().equals("Manuscript")){
                
                sb.append("<Submission_Metadata>\n");
                
                sb.append("\t<"+et.getKey()+">"
                        +getStrippedText(StringUtils.stripToEmpty(
                            et.getValue()))
                        +"</"+et.getKey()+">\n");
                target1:while(true){
                    Map.Entry<String, String> etx = it.next();
                    sb.append("\t<"+etx.getKey()+">"
                        +getStrippedText(StringUtils.stripToEmpty(
                            etx.getValue()))
                        +"</"+etx.getKey()+">\n");
                    if (etx.getKey().equals(
                        xmlTagNameMetaSubList.get(
                            xmlTagNameMetaSubList.size()-1))){
                        sb.append("</Submission_Metadata>\n");
                        break target1;
                    }
                }
            } else if (et.getKey().equals("Classification")) {
                sb.append("<Classification>\n");
                String[] keywords = et.getValue().split(",");
                for (String kw : keywords){
                    sb.append("\t<keyword>"
                    +getStrippedText(StringUtils.stripToEmpty(kw))
                    +"</keyword>\n");
                }
                sb.append("</Classification>\n");
            } else if (et.getKey().equals("Authors")){
            	
                sb.append("<Authors>\n");
                String[] authors = et.getValue().split(",");
                for (String el : authors){  
                    if (el.contains(" and ")){
                        String[] lastAuthors = el.split(" and ");
                        for (String lel : lastAuthors){
                            sb.append("\t<Author>" +
                                    flipName(getStrippedText(
                                            StringUtils.stripToEmpty(lel)))
                                     +"</Author>\n");                            
                        }
                    }
                    else {   //normal case
                        sb.append("\t<Author>"+
                                flipName(getStrippedText(
                                        StringUtils.stripToEmpty(el)))
                                 +"</Author>\n");                            
                        }
                }
                sb.append("</Authors>\n");
            } else if (et.getKey().equals("Contact_Author")){
                sb.append("<Corresponding_Author>\n\t<Name>"+
			  flipName(StringUtils.stripToEmpty(et.getValue()))+"</Name>\n");
                target:while(true){
                    Map.Entry<String, String> etx = it.next();
                    sb.append("\t<"+etx.getKey()+">"
                        +getStrippedText(
                            StringUtils.stripToEmpty(etx.getValue()))
                        +"</"+etx.getKey()+">\n");
                    if (etx.getKey().equals(xmlTagNameAuthorSubList.get(
                            xmlTagNameAuthorSubList.size()-1))){
                        sb.append("</Corresponding_Author>\n");
                        break target;
                    }
                }
            } else {
                
                if (StringUtils.stripToEmpty(et.getValue()).equals("")){
                    sb.append("<"+et.getKey()+" />\n");
                } else {
                	sb.append("<"+et.getKey()+">");
                	sb.append(getStrippedText(et.getValue()));
                    sb.append("</"+et.getKey()+">\n");
                }  
            }
       
        }  // end of for
        
        return sb;
    }
}
