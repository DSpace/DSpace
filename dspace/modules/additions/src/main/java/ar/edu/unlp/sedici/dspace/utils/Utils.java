package ar.edu.unlp.sedici.dspace.utils;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

public class Utils {
	
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
	public static int countAvailableItems(Context context) throws SQLException
	{
		String myQuery = "SELECT count(*) / 100 * 100 as total FROM item WHERE in_archive='1' and withdrawn='0'";

		TableRow row = DatabaseManager.querySingle(context, myQuery);

		int count = Integer.valueOf(row.getIntColumn("total"));
		return count;
	}

}
