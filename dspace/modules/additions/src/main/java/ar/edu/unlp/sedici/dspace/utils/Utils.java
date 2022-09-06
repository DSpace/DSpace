package ar.edu.unlp.sedici.dspace.utils;

import java.sql.SQLException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dspace.identifier.DOI;

public class Utils {
	
	protected static Logger log = LoggerFactory.getLogger(Utils.class);

	public static boolean isEmail(String correo) {
        Pattern pat = null;
        Matcher mat = null;
        pat = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
        mat = pat.matcher(correo);
        if (mat.find()) {
            System.out.println("[" + mat.group() + "]");
            return true;
        }else{
            return false;
        }
    }

	/*
	 * Método que retorna la cantidad de items disponibles en el repositorio,
	 * esto es, el total de items archivados pero no retirados,
	 * redondeado hacia abajo a la unidad de centena inferior mas cercana.
	 * Es decir, si hay 67160 items redondea a 67100.
	 */
	public static int countAvailableItems() throws SQLException
	{
		Context context;
		try {
			context = new Context();
		} catch (SQLException e) {
			log.error("No se pudo instanciar el Context", e);
			throw new RuntimeException(e);
		}
		String myQuery = "SELECT count(*) as total FROM item WHERE in_archive = '1' and withdrawn = '0'";

		TableRow row = DatabaseManager.querySingle(context, myQuery);

		int count = Integer.valueOf(row.getIntColumn("total"));
		try {
			context.complete();
		} catch (SQLException e) {
			log.error("No se pudo cerrar el Context", e);
			throw new RuntimeException(e);
		}
		return count;
	}

	public static boolean matchRegex(String text, String regex) {
		return java.util.regex.Pattern.matches(regex, text);
	}

	/*
	 * Devuelve verdadero si el string cumple con el formato de doi.
	 * Es decir, si cumple con algunos de los formatos siguientes:
	 * http(s)://doi.org/10.XXXX/XXXX,http(s)://dx.doi.org/10.XXXX/XXXX o doi:10.XXXX/XXXX
	 */
	public static boolean isDoi(String identifier) {
		String doiRegex = "((https?:\\/\\/(dx\\.)?doi.org\\/)|(doi:)|(DOI:))10\\.[0-9]{4,9}\\/.{1,200}";
		return java.util.regex.Pattern.matches(doiRegex, identifier);
	}
	
	public static String trimAndLowercase(String value) {
	    return value.trim().toLowerCase();
	}
	
	public static String lowercase(String value) {
	    return value.toLowerCase();
	}

}
