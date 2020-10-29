/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.integration.crosswalks.FileNameDisseminator;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to export items in the given format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemExport extends DSpaceRunnable<ItemExportScriptConfiguration<ItemExport>> {

    private ItemService itemService;


    private UUID itemUuid;

    private String fileName;

    private String exportFormat;

    private Context context;

    @Override
    public void setup() throws ParseException {

        this.itemService = ContentServiceFactory.getInstance().getItemService();

        this.itemUuid = UUIDUtils.fromString(commandLine.getOptionValue('i'));
        this.exportFormat = commandLine.getOptionValue('f');
        this.fileName = commandLine.getOptionValue('n');
    }

    @Override
    @SuppressWarnings("unchecked")
    public ItemExportScriptConfiguration<ItemExport> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("item-export", ItemExportScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {

        context = new Context();
        assignCurrentUserInContext();

        if (exportFormat == null) {
            throw new IllegalArgumentException("The export format must be provided");
        }

        StreamDisseminationCrosswalk streamDisseminationCrosswalk = getCrosswalkByType(exportFormat);
        if (streamDisseminationCrosswalk == null) {
            throw new IllegalArgumentException("No dissemination configured for format " + exportFormat);
        }

        if (itemUuid == null) {
            throw new IllegalArgumentException("A valid item uuid should be provided");
        }

        Item item = itemService.find(context, itemUuid);
        if (item == null) {
            throw new IllegalArgumentException("No item found by id " + itemUuid);
        }

        boolean canDisseminate = streamDisseminationCrosswalk.canDisseminate(context, item);
        if (!canDisseminate) {
            throw new IllegalArgumentException("The item cannot be disseminated by the dissemination " + exportFormat);
        }

        try {
            performExport(item, streamDisseminationCrosswalk);
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }

    }

    private void performExport(Item item, StreamDisseminationCrosswalk streamDisseminationCrosswalk) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamDisseminationCrosswalk.disseminate(context, item, out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        String name = getFileName(streamDisseminationCrosswalk);
        handler.writeFilestream(context, name, in, streamDisseminationCrosswalk.getMIMEType());
        handler.logInfo("Item exported successfully into file named " + name);
    }

    private String getFileName(StreamDisseminationCrosswalk streamDisseminationCrosswalk) {
        if (StringUtils.isNotBlank(fileName)) {
            return fileName;
        }

        if (streamDisseminationCrosswalk instanceof FileNameDisseminator) {
            return ((FileNameDisseminator) streamDisseminationCrosswalk).getFileName();
        } else {
            return "export-result";
        }

    }

    private void assignCurrentUserInContext() throws SQLException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private StreamDisseminationCrosswalk getCrosswalkByType(String type) {
        return new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class).getByType(type);
    }

}
