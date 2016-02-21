package cz.cuni.mff.ufal;

import java.util.Hashtable;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FileTreeViewGenerator {
	
	public static String parse(NodeList nl) {
		
		FileInfo root = new FileInfo("root");
		Node n = nl.item(0).getFirstChild();
		do {			
			String fileInfo = n.getFirstChild().getTextContent();
			String f[] = fileInfo.split("\\|");	
			String fileName = "";
			String path = f[0];
			long size = Long.parseLong(f[1]);
			if(!path.endsWith("/")) {
				fileName = path.substring(path.lastIndexOf('/')+1);
				if(path.lastIndexOf('/')!=-1) {
					path = path.substring(0, path.lastIndexOf('/'));
				} else {
					path = "";
				}
			}

			FileInfo current = root;
			for(String p : path.split("/")) {
				if(current.sub.containsKey(p)) {
					current = current.sub.get(p);
				} else {
					FileInfo temp = new FileInfo(p);
					current.sub.put(p, temp);
					current = temp;
				}
			}
			
			if(!fileName.isEmpty()) {
				FileInfo temp = new FileInfo(fileName, size);
				current.sub.put(fileName, temp);
			}
			
		} while((n=n.getNextSibling())!=null);
		
		int folderID = 0;
		
		StringBuilder result = new StringBuilder();
		result.append("<ul class='treeview'>");
		for(FileInfo in : root.sub.values()) {
			printFileInfo(in, result, ++folderID);
		}
		result.append("</ul>");
		
		return result.toString();
	}
	
	static void printFileInfo(FileInfo f, StringBuilder result, int folderID) {
		if(f.isDirectory) {
			result.append("<li>");
				result.append("<span class='foldername'><a role='button' data-toggle='collapse' href='#folder_" + folderID + "'>").append(f.name).append("</a></span>");
				result.append("<ul id='folder_" + folderID + "' class='in' style='height: auto;'>");
				for(FileInfo in : f.sub.values()) {
					printFileInfo(in, result, ++folderID);
				}
				result.append("</ul>");
			result.append("</li>");
		} else {
			result.append("<li>");
			result.append("<span class='filename'>").append(f.name).append("</span>");
			result.append("<span class='size pull-right'>").append(humanReadableFileSize(f.size)).append("</span>");
			result.append("</li>");
		}
	}
	
	static String humanReadableFileSize(long bytes) {
	    int thresh = 1024;
	    if(Math.abs(bytes) < thresh) {
	        return bytes + " B";
	    }
	    String units[] = {"kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
	    int u = -1;
	    do {
	        bytes /= thresh;
	        ++u;
	    } while(Math.abs(bytes) >= thresh && u < units.length - 1);
	    return bytes + " " + units[u];
	}	

}

class FileInfo {
	
	public String name;
	public long size;
	public boolean isDirectory;
	
	public Hashtable<String, FileInfo> sub = null;
	
	public FileInfo(String name) {
		this.name = name;
		sub = new Hashtable<String, FileInfo>();
		isDirectory = true;
	}
	
	public FileInfo(String name, long size) {
		this.name = name;
		this.size = size;
		isDirectory = false;
	}
	
}
