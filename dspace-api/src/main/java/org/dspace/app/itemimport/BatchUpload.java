/**
 * 
 */
package org.dspace.app.itemimport;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
	
	/**
	 * 
	 */
	public BatchUpload(String dirPath) {
		
		this.initializeWithFile(new File(dirPath));
		
	}

	public BatchUpload(File dir) {
		
		this.initializeWithFile(dir);
		
	}


	private void initializeWithFile(File dir){
		
		this.dir = dir;
		
		String dirName = dir.getName();
		long timeMillis = Long.parseLong(dirName);
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(timeMillis);
		this.date = calendar.getTime();
		
		try {
			this.itemsImported = BatchUpload.countLines(dir + File.separator + "mapfile");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (File file : dir.listFiles()){
			if (file.isDirectory()){
				this.totalItems = file.list().length;
			}
		}
		
		this.successful = this.totalItems == this.itemsImported;
		
	}
	
	static private int countLines(String filename) throws IOException {
	    LineNumberReader reader  = new LineNumberReader(new FileReader(filename));
	    int cnt = 0;
	    String lineRead = "";
	    while ((lineRead = reader.readLine()) != null) {}

	    cnt = reader.getLineNumber(); 
	    reader.close();
	    return cnt;
	}

	public Date getDate() {
		return date;
	}

	public File getDir() {
		return dir;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public int getItemsImported() {
		return itemsImported;
	}

	public int getTotalItems() {
		return totalItems;
	}
	
	public String getDateFormatted(){
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
		
		return df.format(date);
	}
}
