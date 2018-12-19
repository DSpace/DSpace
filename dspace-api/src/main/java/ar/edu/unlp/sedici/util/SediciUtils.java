/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.util;

public class SediciUtils {


	public static String codificarURL(String url){
		char[] reservados={'!','#','$','%','&','(',')','*','+',',','/',':',';','=','?','@','[',']',']', ' '};
		for (char caracter: reservados) {
			url=url.replace(caracter, '_');
		}
		//remplazo la comilla simple
		url=url.replace("'", "_"); 	
 
		return url;
	}
}
