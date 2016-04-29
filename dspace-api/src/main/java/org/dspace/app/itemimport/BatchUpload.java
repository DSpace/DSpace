/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author kstamatis
 *
 */
public class BatchUpload {

	private Date date;
	private File dir;
	private boolean successful;
	private int itemsImported;
	private int totalItems = 0;
	private List<String> handlesImported = new ArrayList<String>();
	private String errorMsg = "";
	private String errorMsgHTML = "";
	
	/**
         * Initialize with directory
	 * @param dirPath directory path
	 */
	public BatchUpload(String dirPath) {
		
		this.initializeWithFile(new File(dirPath));
		
	}

        /**
         * Initialize with directory
         * @param dir directory path
         */
	public BatchUpload(File dir) {
		
		this.initializeWithFile(dir);
		
	}

        /**
         * Initialize with directory
         * @param dir directory path
         */
	private void initializeWithFile(File dir){
		
		this.dir = dir;
		
		String dirName = dir.getName();
		long timeMillis = Long.parseLong(dirName);
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(timeMillis);
		this.date = calendar.getTime();
		
		try {
			this.itemsImported = countLines(dir + File.separator + "mapfile");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (File file : dir.listFiles()){
			if (file.isDirectory()){
				this.totalItems = file.list().length;
			}
		}
		
		this.successful = this.totalItems == this.itemsImported;
		
		//Parse possible error message
		
		File errorFile = new File(dir + File.separator + "error.txt");
		if (errorFile.exists()){
			try {
				readFile(dir + File.separator + "error.txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
        /**
         * Count lines in file
         * @param filename file name
         * @return lines in file
         * @throws IOException if IO error
         */
	private int countLines(String filename) throws IOException {
	    LineNumberReader reader  = new LineNumberReader(new FileReader(filename));
	    int cnt = 0;
	    String lineRead = "";
	    while ((lineRead = reader.readLine()) != null) {
	    	String[] parts = lineRead.split(" ");
	    	if (parts.length > 1)
	    		handlesImported.add(parts[1].trim());
	    	else 
	    		handlesImported.add(lineRead);
	    }

	    cnt = reader.getLineNumber(); 
	    reader.close();
	    return cnt;
	}
	
        /**
         * Read a file
         * @param filename file name
         * @throws IOException if IO error
         */
	private void readFile(String filename) throws IOException {
	    LineNumberReader reader  = new LineNumberReader(new FileReader(filename));
	    String lineRead = "";
	    while ((lineRead = reader.readLine()) != null) {
	    	this.errorMsg += lineRead + "\n";
	    	
	    	if (lineRead.startsWith("\tat ")){
	    		this.errorMsgHTML += "<span class=\"batchimport-error-tab\">" + lineRead + "</span><br/>";
	    	}
	    	else if (lineRead.startsWith("Caused by")){
	    		this.errorMsgHTML += "<span class=\"batchimport-error-caused\">" + lineRead + "</span><br/>";
	    	}
	    	else {
	    		this.errorMsgHTML += lineRead + "<br/>";
	    	}
	    }
	    reader.close();
	}

        /**
         * Get date
         * @return Date
         */
	public Date getDate() {
		return date;
	}

        /**
         * Get path to directory
         * @return directory
         */
	public File getDir() {
		return dir;
	}

        /**
         * Whether successulf
         * @return true or false
         */
	public boolean isSuccessful() {
		return successful;
	}

        /**
         * Get items imported
         * @return number of items
         */
	public int getItemsImported() {
		return itemsImported;
	}

        /**
         * Get total items
         * @return total
         */
	public int getTotalItems() {
		return totalItems;
	}
	
        /**
         * Get formatted date (DD/MM/YY)
         * @return date as string
         */
	public String getDateFormatted(){
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
		
		return df.format(date);
	}

        /**
         * Get handles of imported files
         * @return list of handles
         */
	public List<String> getHandlesImported() {
		return handlesImported;
	}

        /**
         * Get error message
         * @return error message
         */
	public String getErrorMsg() {
		return errorMsg;
	}

        /**
         * Get error message as HTML
         * @return error message string as HTML
         */
	public String getErrorMsgHTML() {
		return errorMsgHTML;
	}
}
