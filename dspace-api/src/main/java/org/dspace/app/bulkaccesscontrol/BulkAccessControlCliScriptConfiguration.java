/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol;

import java.io.InputStream;

import org.apache.commons.cli.Options;

/**
 * Extension of {@link BulkAccessControlScriptConfiguration} for CLI.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class BulkAccessControlCliScriptConfiguration<T extends BulkAccessControlCli>
    extends BulkAccessControlScriptConfiguration<T> {

    @Override
    public Options getOptions() {
        Options options = new Options();

        options.addOption("u", "uuid", true, "target uuids of communities/collections/items");
        options.getOption("u").setType(String.class);
        options.getOption("u").setRequired(true);

        options.addOption("f", "file", true, "source json file");
        options.getOption("f").setType(InputStream.class);
        options.getOption("f").setRequired(true);

        options.addOption("e", "eperson", true, "email of EPerson used to perform actions");
        options.getOption("e").setRequired(true);

        options.addOption("h", "help", false, "help");

        return options;
    }
}
