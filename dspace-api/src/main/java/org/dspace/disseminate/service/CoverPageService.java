package org.dspace.disseminate.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Renders a coverpage for an Item.
 */
public interface CoverPageService {
    /**
     * Render a PDF coverpage for the given Item. The implementation may use the context and
     * any relevant meta data from the Item to populuate dynamic content in the rendered page.
     *
     * @param context the current context
     * @param item the current item
     * @return a PDDocument containing the rendered coverpage.
     * The caller is responsible to close the PDDocument after use!
     */
    PDDocument renderCoverDocument(Context context, Item item);
}
