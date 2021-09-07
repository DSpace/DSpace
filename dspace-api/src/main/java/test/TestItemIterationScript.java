package test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.hibernate.ScrollableResults;

/**
 * Created by kristof on 07/09/2021
 */
public class TestItemIterationScript {
    /* Log4j logger */
    private static final Logger log = Logger.getLogger(TestItemIterationScript.class);

    private ItemService itemService;

    public static void main(String[] args) throws Exception {
        TestItemIterationScript script = new TestItemIterationScript();
        script.setup();
        script.run();
    }

    public void setup() {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
    }

    public void run() throws Exception {
        System.out.println("Starting item iteration...");
        Context context = new Context(Context.Mode.READ_ONLY);
        try (FileWriter writer = new FileWriter("/Users/kristof/Documents/test_item_iteration.txt")) {
            ScrollableResults items = this.itemService.findAllReadOnly(context);
            int i = 0;
            while (items.next()) {
                Item item = (Item) items.get(0);
                writer.write(item.getHandle() + "\n");
                i++;
                if (i%1000 == 0) {
                    System.out.println("Processed " + i + " items");
                    context.clearDatabaseCache();
                }
            }
            System.out.println("Finished item iteration");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.out.println(e.getMessage());
        } finally {
            context.complete();
        }
    }
}
