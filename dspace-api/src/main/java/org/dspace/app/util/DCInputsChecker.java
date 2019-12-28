/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

import java.util.List;

/**
 * Tool for testing {@link DCInputsReader}.
 * Writes out the names of forms and fields.
 *
 * @author mhwood
 */
public class DCInputsChecker {
    /**
     * Just run it:  {@code bin/dsrun org.dspace.app.util.DCInputsChecker}
     * @param argv not used.
     */
    public static void main(String[] argv) {
        DCInputsReader reader = null;
        List<DCInputSet> forms = null;
        try {
            reader = new DCInputsReader();
            forms = reader.getAllInputs(reader.countInputs(), 0);
        } catch (DCInputsReaderException ex) {
            System.err.format("Constructor threw DCInputsReaderException:  %s%n",
                    ex.getMessage());
            System.exit(1);
        }

        // Enumerate forms, pages and fields.
        for (DCInputSet form : forms) {
            System.out.format("Form %s has fields:%n", form.getFormName());
            int pageN = 0;
            for (DCInput[] page : form.getFields()) {
                System.out.format("  Page %d%n", pageN++);
                for (DCInput field : page) {
                    System.out.format("    Field %s%n", field.getFieldName());
                }
            }
        }

        // Get around "must be final" + "might not be initialized" + try/catch
        final DCInputsReader stupidCopyOfReader = reader;

        // Enumerate pair lists
        System.out.println();
        System.out.println("Pair lists:");

        reader.getPairsNameIterator()
                .forEachRemaining((String name) -> {
                    System.out.format("  Pair name:  %s%n", name);
                    List<String> pairs = stupidCopyOfReader.getPairs(name);
                    for (int pairN = 0; pairN < pairs.size(); pairN += 2) {
                        System.out.format("    %s = %s%n",
                                pairs.get(pairN), pairs.get(pairN + 1));
                    }
                });
    }

    /** Utility class.  Don't instantiate. */
    private DCInputsChecker() { }
}
