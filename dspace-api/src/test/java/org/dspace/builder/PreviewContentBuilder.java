/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.PreviewContent;
import org.dspace.content.service.PreviewContentService;
import org.dspace.core.Context;

public class PreviewContentBuilder extends AbstractBuilder<PreviewContent, PreviewContentService> {

    private PreviewContent previewContent;

    protected PreviewContentBuilder(Context context) {
        super(context);
    }

    public static PreviewContentBuilder createPreviewContent(final Context context, Bitstream bitstream, String name,
                                                             String content, boolean isDirectory, String size,
                                                             Map<String, PreviewContent> subPreviewContents) {
        PreviewContentBuilder builder = new PreviewContentBuilder(context);
        return builder.create(context, bitstream, name, content, isDirectory, size, subPreviewContents);
    }

    private PreviewContentBuilder create(final Context context, Bitstream bitstream, String name, String content,
                                         boolean isDirectory, String size,
                                         Map<String, PreviewContent> subPreviewContents) {
        this.context = context;
        try {
            previewContent = previewContentService.create(context, bitstream, name, content,
                    isDirectory, size, subPreviewContents);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public static void deletePreviewContent(Integer id) throws Exception {
        if (Objects.isNull(id)) {
            return;
        }
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            PreviewContent previewContent = previewContentService.find(c, id);

            if (previewContent != null) {
                previewContentService.delete(c, previewContent);
            }
            c.complete();
        }
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            previewContent = c.reloadEntity(previewContent);
            delete(c, previewContent);
            c.complete();
            indexingService.commit();
        }
    }

    @Override
    public PreviewContent build() throws SQLException, AuthorizeException {
        try {
            context.dispatchEvents();
            indexingService.commit();
            return previewContent;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @Override
    public void delete(Context c, PreviewContent dso) throws Exception {
        if (dso != null) {
            getService().delete(c, dso);
        }
    }

    @Override
    protected PreviewContentService getService() {
        return previewContentService;
    }
}
