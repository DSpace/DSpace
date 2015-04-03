/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.dspace.IOUtils;

/**
 * 
 */
public class ControlPanelBackupTab extends AbstractControlPanelTab {

	private Request request = null;
	private static Logger log = Logger.getLogger(ControlPanelBackupTab.class);

	public void add_output(Division div, String message) throws WingException {
		String lines[] = message.split("\n");
		int i = 0;
		for (; i < lines.length; i++) {
			String line = lines[i];
			div.addPara(line);
		}
	}

	@Override
	public void addBody(Map objectModel, Division div) throws WingException {

		request = ObjectModelHelper.getRequest(objectModel);

		String sources_dir = ConfigurationManager.getProperty("lr", "lr.dspace.source.dir");
		String backup_config_dir = sources_dir + "/config";
		String backup_config_file = backup_config_dir
				+ "/_substituted/backup2l.conf.substituted";

		List info = div.addList("backup-config");
		info.setHead("Backup Configuration");
		addBackupFileSection(info, backup_config_file);

		Division available = div.addDivision("available-backups");
		available.setHead("Available Backups");

		String message = null;
		message = IOUtils.run(new File(sources_dir), new String[] { "backup2l",
				"-c", backup_config_file, "-s" });

		if (message != null) {
			if (message.contains("ERROR: Backup volume is locked.")) {
				add_output(available, message);
			} else {
				String lines[] = message.split("\n");
				int i = 0;
				for (; i < lines.length; i++) {
					String line = lines[i];
					available.addPara(line);
					if (line.trim().equals("======="))
						break;
				}

				int rowCount = 0;
				int colCount = 0;

				for (int j = i; j < lines.length; j++) {
					String line = lines[j];
					if (line.trim().startsWith("all"))
						rowCount++;
				}

				i += 2;
				if (i >= lines.length) {
					add_output(available, message);
				} else {
					colCount = lines[i].trim().split("[ \\|]+").length;

					Table backups = available.addTable("backups", rowCount + 1,
							colCount);

					Row r = backups.addRow(Cell.ROLE_HEADER);

					int indexTillRead = i + rowCount + 2;

					for (; i < indexTillRead; i++) {
						if (lines[i].trim().startsWith("---"))
							continue;
						String cols[] = lines[i].split("[ \\|]+");
						for (String col : cols) {
							r.addCellContent(col);
						}
						r = backups.addRow(Cell.ROLE_DATA);
					}

					for (; i < lines.length; i++) {
						available.addPara(lines[i]);
					}
				}
			}
		}

	}

	private void addBackupFileSection(List info, String filename)
			throws WingException {
		Properties source_config = new Properties();
		try {
			source_config.load(new FileReader(filename));

			info.addLabel("SRCLIST");
			info.addItem(source_config.getProperty("SRCLIST"));

			info.addLabel("BACKUP_DIR");
			info.addItem(source_config.getProperty("BACKUP_DIR"));

			info.addLabel("MAX_LEVEL");
			info.addItem(source_config.getProperty("MAX_LEVEL"));

			info.addLabel("MAX_PER_LEVEL");
			info.addItem(source_config.getProperty("MAX_PER_LEVEL"));

			info.addLabel("MAX_FULL");
			info.addItem(source_config.getProperty("MAX_FULL"));

			info.addLabel("GENERATIONS");
			info.addItem(source_config.getProperty("GENERATIONS"));

		} catch (FileNotFoundException e) {
			info.addItem(filename + " file not found!", "alert alert-error");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}


