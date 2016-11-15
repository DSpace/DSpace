/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.mediafilter.MediaFilterManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;

@Distributive
public class MediaFilterCurationTask extends AbstractCurationTask {

	protected Logger log = Logger.getLogger(MediaFilterCurationTask.class);

	@Override
	public void init(Curator curator, String taskId) throws IOException {
		super.init(curator, taskId);
	}
	
	@Override
	public int perform(DSpaceObject dso) throws IOException {
		distribute(dso);
        return Curator.CURATE_SUCCESS;
	}
	
	@Override
    protected void performItem(Item item) throws SQLException, IOException
    {
		Context context = Curator.curationContext();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    PrintStream ps = new PrintStream(baos);
		try {
			PrintStream out = System.out;
			MediaFilterManager.init(null);
			System.setOut(ps);
			MediaFilterManager.applyFiltersItem(context, item);
			System.out.flush();
			System.setOut(out);
		} catch (Exception e) {
			setResult(e.getMessage());
			report(e.getMessage());
			throw new RuntimeException(e.getMessage(), e);
		}
		context.commit();
		setResult(baos.toString());
		report(baos.toString());
		ps.close();
	}
}
