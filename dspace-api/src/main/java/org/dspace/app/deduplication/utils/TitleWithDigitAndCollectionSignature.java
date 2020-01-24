/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.deduplication.utils;

import java.sql.SQLException;
import java.util.Locale;

import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;

public class TitleWithDigitAndCollectionSignature extends MD5ValueSignature {

	@Override
	protected String normalize(/* BrowsableDSpaceObject */DSpaceObject item, Context context, String value) {
		if (value != null) {
			String temp = null;
			if (item != null) {
				/* BrowsableDSpaceObject */DSpaceObject parent = null;
				try {
					parent = ContentServiceFactory.getInstance().getDSpaceObjectService(item).getParentObject(context,
							item);
					// parent = item.getParentObject();
				} catch (Exception e) {
					log.error(e.getMessage());
				}

				if (parent != null) {
					temp = parent.getName();
				}
			}
			String norm = Normalizer.normalize(value, Normalizer.NFD);
			CharsetDetector cd = new CharsetDetector();
			cd.setText(value.getBytes());
			CharsetMatch detect = cd.detect();
			if (detect != null && detect.getLanguage() != null) {
				norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase(new Locale(detect.getLanguage()));
			} else {
				norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase();
			}
			if (temp != null) {
				return temp + " " + norm;
			}
			return norm;
		} else {
			return "item:" + item.getID();
		}
	}

}
