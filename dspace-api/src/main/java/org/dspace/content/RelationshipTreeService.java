/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipTreeService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipService relationshipService;

    public Node getTree(Context context, Item item, String scopeString) {
        Node node = null;
        getNode(context, item, parseScope(scopeString), new HashSet<>(), -1, node);
        return node;
    }

    public Set<UUID> getItemsInTree(Context context, Item item, String scopeString, boolean includeRoot) {
        Set<UUID> itemsInTree = new HashSet<>();
        Node node = null;
        getNode(context, item, parseScope(scopeString), itemsInTree, -1, node);
        if (!includeRoot) {
            itemsInTree.remove(item.getID());
        }
        return itemsInTree;
    }

    private Map<String, Boolean> parseScope(String scopeString) {
        Map<String, Boolean> scope = new HashMap<>();
        for (String part : scopeString.split(",")) {
            String[] pair = part.split(":");
            String relName = pair[0];
            boolean recursive = pair.length == 2 && pair[1].toLowerCase().startsWith("r");
            scope.put(relName, recursive);
        }
        return scope;
    }

    private void getNode(Context context, Item item, Map<String, Boolean> scope,
                         Set<UUID> itemsInTree, int place, Node node) {
        try {
            if (item.getHandle() == null) {
                //Not archived don't add
            } else {
                itemsInTree.add(item.getID());
                node = new Node(item.getID(), getFirstMetadataValue(item, "relationship", "type"),
                        getFirstMetadataValue(item, "dc", "title"),
                        place, getRels(context, item, scope, itemsInTree));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, SortedSet<Node>> getRels(Context context, Item item, Map<String, Boolean> scope,
                                                 Set<UUID> itemsInTree)
            throws SQLException {
        Map<String, SortedSet<Node>> rels = new HashMap<>();
        for (Relationship rel : relationshipService.findByItem(context, item)) {
            String relName;
            Item childItem;
            int childPlace;
            if (rel.getLeftItem().getID().equals(item.getID())) { // this item is on the left
                relName = rel.getRelationshipType().getRightwardType();
                childItem = rel.getRightItem();
                childPlace = rel.getLeftPlace();
            } else { // this item is on the right
                relName = rel.getRelationshipType().getLeftwardType();
                childItem = rel.getLeftItem();
                childPlace = rel.getRightPlace();
            }
            if (scope.containsKey(relName) || scope.containsKey("*")) {
                // we care about this relationship
                SortedSet<Node> relatedItems = rels.get(relName);
                if (relatedItems == null) {
                    relatedItems = new TreeSet<>(Comparator.comparingInt(node -> node.place));
                    rels.put(relName, relatedItems);
                }
                Map<String, Boolean> childScope;
                if (itemsInTree.contains(childItem.getID())) {
                    childScope = Map.of();
                } else {
                    // if the child isn't in the tree yet, include in-scope rels
                    childScope = new HashMap<>(scope);
                    // ..but exclude the current relName if it's non-recursive
                    boolean recursive = false;
                    if (scope.containsKey("*")) { // default to the recursive setting for *, if specified
                        recursive = scope.get("*");
                        if (!recursive) {
                            childScope.remove("*"); // don't go deeper by default if non-recursive * is specified
                        }
                    }
                    if (scope.containsKey(relName)) { // if exact relName is specified, prefer its recursive setting
                        recursive = scope.get(relName);
                        if (!recursive) {
                            childScope.remove(relName); // don't go deeper for this relName if given as non-recursive
                        }
                    }
                }
                Node node = null;
                getNode(context, childItem, childScope, itemsInTree, childPlace, node);
                if (node != null) {
                    relatedItems.add(node);
                }
            }
        }
        return rels;
    }

    private String getFirstMetadataValue(Item item, String schema, String element) {
        String value = itemService.getMetadataFirstValue(item, schema, element, null, Item.ANY);
        return value != null ? value : "";
    }

    public class Node {
        UUID id;
        String entityType;
        String title;
        int place;
        Map<String, SortedSet<Node>> rels;

        Node(UUID id, String entityType, String title, int place, Map<String, SortedSet<Node>> rels) {
            this.id = id;
            this.entityType = entityType;
            this.title = title;
            this.place = place;
            this.rels = rels;
        }

        public void print(PrintStream printStream) {
            print(printStream, "");
        }

        private void print(PrintStream printStream, String prefix) {
            String placeString = "";
            if (place >= 0) {
                placeString = "[" + place + "] ";
            }
            printStream.println(prefix + placeString + id + " (" + entityType + ") \"" + title + "\"");
            for (String relName : rels.keySet()) {
                System.out.println(prefix + "  " + relName);
                for (Node child : rels.get(relName)) {
                    child.print(printStream, prefix + "    ");
                }
            }
        }
    }
}

