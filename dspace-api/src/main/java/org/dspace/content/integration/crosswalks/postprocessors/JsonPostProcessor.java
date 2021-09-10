/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.postprocessors;

import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

/**
 * Consumer implementation to post process all the lines of the generated json.
 * It is used to remove the commas present before an } or a ] character.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class JsonPostProcessor implements Consumer<List<String>> {

    @Override
    public void accept(List<String> lines) {

        ListIterator<String> iterator = lines.listIterator();

        while (iterator.hasNext()) {
            String current = iterator.next();
            String next = cleanUpString(getNextValue(iterator));
            if ((next.startsWith("}") || next.startsWith("]")) && cleanUpString(current).endsWith(",")) {
                removeLastComma(iterator, current);
            }
        }
    }

    private String getNextValue(ListIterator<String> iterator) {
        if (!iterator.hasNext()) {
            return "";
        }

        String nextValue = iterator.next();
        iterator.previous();

        return nextValue;
    }

    private void removeLastComma(ListIterator<String> iterator, String current) {
        iterator.previous();
        iterator.set(current.substring(0, current.length() - 1));
    }

    private String cleanUpString(String str) {
        return str.replace("\r", "").replace("\n", "").replace("\t", "").trim();
    }

}
