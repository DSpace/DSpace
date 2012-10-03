package org.dspace.dataonemn;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;

import nu.xom.Document;
import nu.xom.Serializer;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.DateFormat;

public class SysMetaManager extends AbstractObjectManager {

	public SysMetaManager(Context aContext, String aCollection,
			String aSolrServer) {
		super(aContext, aCollection, aSolrServer);
	}

	public void getObjectMetadata(String aID, OutputStream aOutputStream)
			throws NotFoundException, SolrServerException, SQLException,
			IOException {
		Serializer serializer = new Serializer(aOutputStream);
		SystemMetadata sysMeta = new SystemMetadata(aID);
		Item item = getDSpaceItem(aID);
		EPerson ePerson = item.getSubmitter();
		String epID = Integer.toString(ePerson.getID());
		String epEmail = ePerson.getEmail();
		String id = parseIDFormat(aID)[0];
		String format = parseIDFormat(aID)[1];

		Date date = item.getLastModified();
		String lastMod = DateFormat.getDateTimeInstance().format(date);
		sysMeta.setLastModified(lastMod);

		if (format.equals("dap")) {
			sysMeta.setObjectFormat(DRYAD_NAMESPACE);
		}
		else {
			sysMeta.setObjectFormat(format);
		}

		sysMeta.setSubmitter(epID);
		sysMeta.setRightsHolder(epEmail); // or, could be same value as epID

		// Add relationship between science data and data metadata
		if (format.equals("dap")) {
			Bitstream bitstream = getOrigBitstream(item, "*");
			String name = bitstream.getName();
			int extIndex = name.lastIndexOf(".");
			String relFormat = "*"; // if we don't have an extension
			
			if (extIndex != -1) {
				relFormat = name.substring(extIndex + 1);
			}
			
			sysMeta.setDescribes(id + "/" + relFormat);
		}
		else {
			sysMeta.setDescribedBy(id + "/dap");
		}
		
		// DataONE will think about checksums, etc., for science metadata
		if (!format.equals("dap")) {
			Bitstream bitstream = getOrigBitstream(item, format);
			String checksum = bitstream.getChecksum();
			String algorithm = bitstream.getChecksumAlgorithm();

			sysMeta.setChecksum(algorithm, checksum);
			sysMeta.setSize(bitstream.getSize());
		}
		else {
			String[] checksumDetails = getObjectChecksum(id, format);
			sysMeta.setChecksum(checksumDetails[1], checksumDetails[0]);
		}
		
		sysMeta.setAuthoritative(MN_NODE_NAME);
		sysMeta.setOrigin(MN_NODE_NAME);

		serializer.write(new Document(sysMeta));
		serializer.flush();
		aOutputStream.close();
	}
}
