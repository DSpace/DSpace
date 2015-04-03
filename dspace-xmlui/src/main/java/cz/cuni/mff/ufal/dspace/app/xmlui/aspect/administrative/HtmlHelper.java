/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;

import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.HtmlHelper.header_class;



/**
 */
public class HtmlHelper {

	// types
	public enum header_class {
		DEFAULT,
		OK,
		WARNING,
		NOT_OK,
		REQUIRED,
		REQUIRED_MISSING,
		OPTIONAL,
		OPTIONAL_MISSING,
		THIS_IS_CHECK,
	}
	
	// variables
	private final Division main_div;
	private final String link;
	private TableHelper last_table = null;
	
	// ctor
	public HtmlHelper( Division div, String web_link ) {
		main_div = div;
		link = web_link;
	}
	
	public void file_chooser( final String[] files ) throws WingException {
		file_chooser( files, "Choose different file" );
	}

	public void file_chooser( final String[] files, String header ) throws WingException {
		header1( header, "clickable" );
		List l = main_div.addList( "files", null, "well well-small nav nav-pills nav-stacked" );
		for ( String file : files ) {
			l.addItemXref( link + "&extra=" + file, file );
		}
		main_div.addPara(" ");
	}

	public void exception( String msg, Exception e ) {
		try {
			main_div.addPara("exception", "alert alert-error").addContent( msg );
		} catch (WingException e1) {
		}
	}

	public void warning( String msg ) throws WingException {
		main_div.addPara( "warning", "alert").addContent( msg );		
	}

	public void ok( String msg ) throws WingException {
		main_div.addPara( "ok", "ok").addContent( msg );		
	}
	
	public void header( String msg ) throws WingException {
		main_div.addPara( "header", "header").addContent( msg );		
	}
	public void header( String msg, String cls ) throws WingException {
		main_div.addPara( "header", "header " + cls).addContent( msg );		
	}

	public void header1( String msg ) throws WingException {
		main_div.addPara( "header1", "header1").addContent( msg );		
	}
	public void header1( String msg, String cls ) throws WingException {
		main_div.addPara( "header1", "header1 btn btn-default " + cls).addContent( msg );		
	}
	
	public void table( String name, boolean hideable ) {
		last_table = new TableHelper(main_div, name, hideable ? "table_hideable linebreak" : "linebreak" );
	}
	
	public void table( String name ) {
		table( name, false );
	}

	public void table_header( String[] columns, header_class hcls ) throws WingException {
		assert last_table != null;
		last_table.add_header( columns, hcls );
	}
	public void table_header( String[] columns ) throws WingException {
		assert last_table != null;
		last_table.add_header( columns, header_class.DEFAULT );
	}

	public void table_add( String[] columns, header_class hcls ) throws WingException {
		assert last_table != null;
		last_table.add( columns, hcls );
	}
	public void table_add( String[] columns ) throws WingException {
		assert last_table != null;
		last_table.add( columns, header_class.DEFAULT );
	}
	
	public void space() throws WingException {
		content( " " );
	}
	
	public void content( String msg ) throws WingException {
		main_div.addPara( msg );
	}
	
	public void button( String name, String value ) throws WingException {
		main_div.addDivision("control-button", name).addSimpleHTMLFragment(true, value);
	}
	

	public void failed() throws WingException {
		main_div.addHidden( "check-failed" );
	}

	//
	public void success() throws WingException {
		main_div.addHidden( "check-success" );
	}
	
	
		
	// helpers
	// make more java enum->str
	//
	public static String cls( header_class hcls ) {
		switch (hcls) {
			case OK:
				return "header-ok alert alert-success";
			case WARNING:
				return "header-warning alert";
			case NOT_OK:
				return "header-not-ok alert alert-error";
			case REQUIRED:
				return "required";
			case REQUIRED_MISSING:
				return "required-missing alert alert-error";
			case OPTIONAL:
				return "optional";
			case OPTIONAL_MISSING:
				return "optional-missing";
			case THIS_IS_CHECK:
				return "this-is-check";
			default:
				break;
		}
		return null;
	}	

	//
	//
	//
	public static void main( String[] args ) {
	}

} // HtmlHelper


//
//
//
class TableHelper {
	
	private Table tab = null;
	private final Division main_div;
	private final String name;
	private final String cls;

	public TableHelper( Division div, String tab_name, String class_string ) {
		main_div = div;
		name = tab_name;
		cls = class_string;
	}
	
	public void add_header( String[] columns, header_class hcls ) throws WingException {
		if ( null == tab ) {
			tab = main_div.addTable( name, 1, columns.length, cls );
		}
		Row headRow = tab.addRow();
		Cell headCell = headRow.addCell( null, Cell.ROLE_HEADER, 0, 2, HtmlHelper.cls(hcls) );
		headCell.addContent( columns[0] );
	}

	public void add( String[] columns, header_class hcls ) throws WingException {
		if ( null == tab ) {
			tab = main_div.addTable( name, 1, columns.length, cls );
		}
		Row row = tab.addRow( null, null, HtmlHelper.cls(hcls) );
		for ( int i = 0; i < columns.length; ++i ) {
			if ( columns[i].startsWith("http") ) {
				row.addCell().addXref( columns[i], columns[i], "blank");
			} else {
				row.addCellContent( columns[i] );
			}
		}
	}

	
} // Table
