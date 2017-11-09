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
