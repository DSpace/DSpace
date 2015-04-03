/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.checks;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.checks.ImportantLogs;
import cz.cuni.mff.ufal.dspace.IOUtils;
import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.AbstractControlPanelTab;
import cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.HtmlHelper;

/**
 * Add a section that shows last user logins.
 */
public class LoggingControlCheck extends AbstractControlPanelTab {

	// constants
	static private final String[] mandatory_log_names = {
												"authentication.log.",
												"cocoon.log.",
												"dspace.log.",
												"utilities.log",
									};

	private static String log_dir = null;
	static {
		try {
			log_dir = ConfigurationManager.getProperty("log.dir");
		} catch (Exception e) {
		}
	}

	private Request request = null;
	protected HtmlHelper html = null;

	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		div = div.addDivision( this.getClass().getSimpleName(), "control_check  well well-light" );

		Request request = ObjectModelHelper.getRequest(objectModel);
		html = new HtmlHelper(div, web_link);

		String[] log_names = list_log_files(IOUtils.log_with_date("", null));
		boolean has_option = false;

		// select other files
		String[] all_latest_files = IOUtils.list_dates();
		html.file_chooser(all_latest_files);

		String option = request.getParameter("extra");

		if (option != null) {
			has_option = true;
			List<String> tmp = new ArrayList<String>();
			for (String f : list_log_files(option)) {
				if (f.contains(option)) {
					tmp.add(f);
				}
			}
			log_names = tmp.toArray(new String[tmp.size()]);
		}

		for (int i = 0; i < log_names.length; ++i) {
			String input_file_base = log_names[i];
			String input_file = absolute_log(input_file_base);

			BufferedReader safe_reader = null;
			try {
				safe_reader = IOUtils.safe_reader(input_file);
			} catch (EOFException e) {
				html.table("warnings");
				html.table_header(new String[] { String.format(
						"File: [%s] Warning: [%s]", input_file_base,
						e.toString()), }, HtmlHelper.header_class.WARNING);
				continue;
			} catch (Exception e) {
				html.failed();
				html.exception(e.toString(), null);
				continue;
			}

			// output warnings
			ImportantLogs logs = new ImportantLogs(safe_reader,
					IOUtils.get_date_from_log_file(input_file));
			if (0 < logs.warnings().size()) {
				for (String warning : logs.warnings()) {
					html.warning(warning);
					html.failed();
				}
			}

			try {
				boolean problem_found = (logs._lines.size() != 0);
				// output info
				if (!problem_found) {
					html.header(String.format(
							"File: [%s] Warnings/Errors: [%d]",
							input_file_base, logs._lines.size()), HtmlHelper
							.cls(problem_found ? HtmlHelper.header_class.NOT_OK
									: HtmlHelper.header_class.OK));
				} else {

					html.failed();
					//html.header1("File: " + input_file);
					html.table("exceptions", true);
					html.table_header(
							new String[] {
									String.format(
											"File: [%s] Warnings/Errors: [%d]",
											input_file_base, logs._lines.size()),
									null },
							problem_found ? HtmlHelper.header_class.NOT_OK
									: HtmlHelper.header_class.OK);

					html.table_add(new String[] { "Exception at line:",
							"Content" });
					for (String exc : logs._lines) {
						try {
							String[] vals = exc.split(":", 2);
							html.table_add(vals);
						} catch (Exception ex) {
							continue;
						}
					}
				}

			} catch (Exception e) {
				html.failed();
				return;
			}
			html.header1(" ");

		} // for

		if (!has_option) {
			for (String mln : mandatory_log_names) {
				boolean found = false;
				for (String ln : log_names) {
					if (ln.startsWith(mln)) {
						found = true;
						break;
					}
				}
				if (!found) {
					html.failed();
					html.table("exceptions");
					html.table_header(new String[] {
							"File [" + mln + "] not found", null },
							HtmlHelper.header_class.NOT_OK);
				}
			} // for
		}

	}

	static public String[] list_latest_log_files(final String must_contain) {
		return list_log_files(IOUtils.log_with_date(must_contain, "yyyy-MM"));
	}

	static public String absolute_log(String log_file) {
		return new File(log_dir, log_file).toString();
	}

	static public String[] list_log_files(final String must_contain) {
		return IOUtils.list_files(log_dir, must_contain);
	}

}


