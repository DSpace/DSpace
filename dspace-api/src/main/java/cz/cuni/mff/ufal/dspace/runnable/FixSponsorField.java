package cz.cuni.mff.ufal.dspace.runnable;

import org.dspace.app.util.DCInput;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import java.io.Console;
import java.sql.SQLException;

/**
 * Created by okosarko on 28.12.15.
 *
 * The structured string stored in local.sponsor is expected to have 5 fields, add empty ones if this is not true.
 */
public class FixSponsorField {

    private static final int EXPECTED_FIELD_COUNT = 5;

    public static void fixItem(Item item) throws SQLException, AuthorizeException {
        Metadatum[] sponsors = item.getMetadataByMetadataString("local.sponsor");
        if(sponsors != null && sponsors.length > 0) {
            item.clearMetadata("local","sponsor", Item.ANY, Item.ANY);
            for (Metadatum dval : sponsors) {
                String val = dval.value;
                int seenFieldCount = val.split(DCInput.ComplexDefinition.SEPARATOR, -1).length;
                if (seenFieldCount != EXPECTED_FIELD_COUNT) {
                    for (int i = EXPECTED_FIELD_COUNT - seenFieldCount; i > 0; i--) {
                        val += DCInput.ComplexDefinition.SEPARATOR;
                    }
                    dval.value = val;
                }
                item.addMetadatum(dval);
            }
            item.update();
        }
    }

    public static void main(String[] args) throws SQLException, AuthorizeException {
        Context context = new Context();
        Console console = System.console();
        EPerson eperson = null;
        do{
            String email = console.readLine("Enter site administrators email:");
            eperson = EPerson.findByEmail(context,email);
        }while (eperson == null);
        context.setCurrentUser(eperson);
        ItemIterator ii = Item.findAll(context);
        while(ii.hasNext()){
            Item item = ii.next();
            try{
                fixItem(item);
            } catch (SQLException | AuthorizeException e) {
                context.abort();
                System.err.print(String.format("Error on item %s. Quitting.", item.getID()));
                e.printStackTrace();
                break;
            }
        }
        context.complete();
    }
}
