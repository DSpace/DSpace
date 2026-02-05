/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * Created by: Andrew Wood
 * Date: 03 Jun 2020
 */
public class PackagerFileService {

    private static org.apache.logging.log4j.Logger log =
            org.apache.logging.log4j.LogManager.getLogger(PackagerFileService.class);

    private PackageParameters params = null;
    private boolean keepExist = false;
    private boolean restore = false;
    private boolean forceReplace = false;
    private DSpaceObjectService dSpaceObjectService;
    private HandleService handleService;
    private Map<PackagerIngestAction, String> actionMap = new HashMap<>();

    public PackagerFileService(@Nullable PackageParameters pkgParams) {
        if (pkgParams != null) {
            this.params = pkgParams;
            restore = pkgParams.restoreModeEnabled();
            forceReplace = pkgParams.replaceModeEnabled();
            keepExist = pkgParams.keepExistingModeEnabled();
        }
        actionMap.put(PackagerIngestAction.CREATE, "Create");
        actionMap.put(PackagerIngestAction.FAIL_RESTORE_IN_DSPACE, "Cannot Restore, Object Exist");
        actionMap.put(PackagerIngestAction.REPLACE_IN_DSPACE, "Replace");
        actionMap.put(PackagerIngestAction.REPLACE_NOT_IN_DSPACE, "Cannot Replace, Not in DSpace");
        actionMap.put(PackagerIngestAction.SKIP_RELATED_FILE_NOT_FOUND, "Related file not found");
        actionMap.put(PackagerIngestAction.SKIP_RESTORE_IN_DSPACE, "Cannot Restore, Object Exist");
        actionMap.put(PackagerIngestAction.RESTORE_NOT_IN_DSPACE, "Restore");

    }

    public List<FileNode> getFileNodeTree(Context context, String sourceFilesPath, String scope) throws SQLException {
        return getFileNodes(context, sourceFilesPath, parseScope(scope), new HashSet<>());
    }

    public List<String> getPathsInTree(Context context, String sourceFilePath, String scopeString) throws SQLException {
        List<String> filesInTree = new ArrayList<>();
        List<FileNode> fileNodes = getFileNodeTree(context, sourceFilePath, scopeString);
        if (!fileNodes.isEmpty()) {
            fileNodes.get(0).getTreePaths(filesInTree);
        }
        return filesInTree;
    }

    public Map<String, Boolean> parseScope(String scopeString) {
        Map<String, Boolean> scope = new HashMap<>();
        for (String part : scopeString.split(",")) {
            String[] pair = part.split(":");
            String relName = pair[0];
            boolean recursive = pair.length == 2 && pair[1].toLowerCase().startsWith("r");
            scope.put(relName, recursive);
        }
        return scope;
    }

    private String initUUID(String sourceFile) {
        String uuid = null;
        METSManifest manifest = getManifestData(sourceFile);
        String[] id = manifest.getMets().getAttribute("ID").getValue().split("ID-");
        if (id.length > 1) {
            uuid = id[1];
        }
        return uuid;
    }

    private int initType(String sourceFile) {
        int type = 2;
        METSManifest manifest = getManifestData(sourceFile);
        String[] typeAttr = manifest.getMets().getAttribute("TYPE").getValue().split(" ");
        if (typeAttr.length > 1) {
            String typeString = typeAttr[1];
            type = Constants.getTypeID(typeString);
        }
        return type;
    }

    public List<FileNode> getFileNodes(Context context, String sourceFilePath,
                                       Map<String, Boolean> scope, Set<String> filesInTree) throws SQLException {
        List<FileNode> fileNodes = new ArrayList<>();
        filesInTree.add(sourceFilePath);
        FileNode fileNode = new FileNode(getSourceFileHandle(sourceFilePath),
                sourceFilePath, getRels(context, sourceFilePath, scope, filesInTree),
                initUUID(sourceFilePath), initType(sourceFilePath));
        getAction(context, fileNode);
        fileNodes.add(fileNode);
        return fileNodes;
    }

    // TODO: 6/3/20  feed in packager params to comply with various ingest types
    public METSManifest getManifestData(String sourceFile) {
        METSManifest manifest = null;
        try {
            File pkgFile = new File(sourceFile);
            boolean validate = false;
            //Assume zip
            boolean manifestOnly = false;
            if (params != null) {
                validate = params.getBooleanProperty("validate", false);
                manifestOnly = params.getBooleanProperty("manifestOnly", false);
            }

            // parsed out METS Manifest from the file.
            if (manifestOnly) {
                // parse the bare METS manifest and sanity-check it.
                //dspaceSIP value comes from METSIngest
                try (InputStream is = new FileInputStream(pkgFile)) {
                    manifest = METSManifest.create(is, validate, "dspaceSIP");
                }
            } else {
                try (ZipFile zip = new ZipFile(pkgFile)) {
                    // Retrieve the manifest file entry (named mets.xml)
                    ZipEntry manifestEntry = zip.getEntry(METSManifest.MANIFEST_FILE);

                    if (manifestEntry != null) {
                        // parse the manifest and sanity-check it.
                        //dspaceAIP comes from AIPingester
                        manifest = METSManifest.create(zip.getInputStream(manifestEntry),
                                validate, "dspaceAIP");
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            log.error(fnfe.getMessage(), fnfe);
        } catch (IOException ioe) {
            log.error(ioe.getMessage(), ioe);
        } catch (MetadataValidationException mve) {
            log.error(mve.getMessage(), mve);
        }
        return manifest;
    }

    //Extract handle from parent Mets node
    public String getSourceFileHandle(String sourceFile) {
        METSManifest metsManifest = getManifestData(sourceFile);
        String handle = metsManifest.getMets().getAttribute("OBJID").getValue();
        if (handle.split(":").length < 2) {
            throw new RuntimeException("Source file is malformed! Missing handle!");
        } else {
            handle = handle.split(":")[1];
        }
        return handle;
    }

    public Map<String, List<FileNode>> getRels(Context context, String sourceFilePath, Map<String, Boolean> scope,
                                               Set<String> filesInTree) throws SQLException {
        Map<String, List<FileNode>> rels = new HashMap<>();
        Element relsParentElement = getRelsStrucMap(getManifestData(sourceFilePath));
        List<Element> relsElements = new ArrayList<>();
        if (relsParentElement != null) {
            relsElements = relsParentElement.getChildren();
        }
        for (Element relElement : relsElements) {
            String handle;
            String path;
            String uuid = null;
            String relName = relElement.getAttributeValue("ID").split("_")[1];
            int type;
            if (scope.containsKey(relName) || scope.containsKey("*")) {
                // we care about this relationship
                for (Object relElementChildren : relElement.getChildren()) {
                    //The children of relElementChildrenElement are that items MPTR elements
                    Element relElementChildrenElement = (Element) relElementChildren;
                    handle = getMPTRData(relElementChildrenElement.getChildren(), "HANDLE");
                    path = setRelPath(sourceFilePath,
                            getMPTRData(relElementChildrenElement.getChildren(), "URL"));
                    uuid = getMPTRData(relElementChildrenElement.getChildren(), "URN").split(":")[2];
                    List<FileNode> relatedItems = rels.get(relName);
                    type = initType(sourceFilePath);
                    if (relatedItems == null) {
                        relatedItems = new ArrayList<>();
                        rels.put(relName, relatedItems);
                    }
                    Map<String, Boolean> childScope;
                    if (filesInTree.contains(path)) {
                        childScope = Map.of();
                    } else {
                        // if the child isn't in the tree yet, include in-scope rels
                        childScope = new HashMap<>(scope);
                        // ..but exclude the current relName if it's non-recursive
                        boolean recursive = false;
                        if (scope.containsKey("*")) { // default to the recursive setting for *, if specified
                            recursive = scope.get("*");
                            if (!recursive) {
                                childScope.remove("*"); // don't go deeper by default if
                                // non-recursive * is specified
                            }
                        }
                        if (scope.containsKey(relName)) { // if exact relName is specified, prefer its recursive setting
                            recursive = scope.get(relName);
                            if (!recursive) {
                                childScope.remove(relName); // don't go deeper for this relName
                                // if given as non-recursive
                            }
                        }
                    }
                    //Iterate through children
                    if (!filesInTree.contains(path)) {
                        filesInTree.add(path);
                        FileNode fileNode;
                        if (new File(path).exists()) {
                            fileNode = new FileNode(handle, path,
                                    getRels(context, path, childScope, filesInTree), uuid, type);
                        } else {
                            fileNode = new FileNode(handle, path, Map.of(), uuid, type);
                            fileNode.setAction(PackagerIngestAction.SKIP_RELATED_FILE_NOT_FOUND);
                        }
                        if (fileNode.action == null) {
                            getAction(context, fileNode);
                        }
                        relatedItems.add(fileNode);
                    }
                }
            }
        }
        return rels;
    }

    //grab specified MPTR data from rel node
    public String getMPTRData(List<Element> mptrs, String loctype) {
        for (Element mptr : mptrs) {
            if (mptr.getAttribute("LOCTYPE").getValue().equals(loctype)) {
                //For some god forsaken reason mptr.getAttribute("href")
                // returns null so I guess I'll loop over all of them
                for (Object attribute : mptr.getAttributes()) {
                    Attribute attrAttribute = (Attribute) attribute;
                    if (attrAttribute.getName().equalsIgnoreCase("href")) {
                        return attrAttribute.getValue();
                    }
                }
            }
        }
        return null;
    }

    //Set child file path with respect to sourceFile path
    public String setRelPath(String sourceFilePath, String childFileName) {
        int lastUnderscore = sourceFilePath.lastIndexOf("_") + 1;
        return sourceFilePath.substring(0, lastUnderscore) + childFileName;
    }

    //Get the top level rel strucMap
    public Element getRelsStrucMap(METSManifest metsManifest) {
        List<Element> children = metsManifest.getMets().getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getAttribute("ID") != null && children.get(i)
                    .getAttribute("ID").getValue().equals("rels")) {
                return children.get(i);
            }
        }
        return null;
    }

    public void getAction(Context context, FileNode fileNode) throws SQLException {
        if (fileNode.action == null) {
            DSpaceObject fileNodeObject = null;
            if (fileNode.uuid != null && !fileNode.uuid.equals("")) {
                dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(fileNode.type);
                fileNodeObject = dSpaceObjectService.find(context, UUID.fromString(fileNode.uuid));
            } else if (fileNode.handle != null) {
                handleService = HandleServiceFactory.getInstance().getHandleService();
                fileNodeObject = handleService.resolveToObject(context, fileNode.handle);
            }
            if (fileNodeObject == null) {
                if (restore && forceReplace) {
                    fileNode.action = PackagerIngestAction.REPLACE_NOT_IN_DSPACE;
                } else if (restore || restore && keepExist || restore && forceReplace) {
                    fileNode.action = PackagerIngestAction.RESTORE_NOT_IN_DSPACE;
                }
            } else {
                if (restore) {
                    fileNode.action =  PackagerIngestAction.FAIL_RESTORE_IN_DSPACE;
                }
                if (restore && forceReplace) {
                    fileNode.action = PackagerIngestAction.REPLACE_IN_DSPACE;
                }
                if (restore && keepExist) {
                    fileNode.action =  PackagerIngestAction.SKIP_RESTORE_IN_DSPACE;
                }
            }
            if (!restore && !forceReplace && !keepExist) {
                fileNode.action = PackagerIngestAction.CREATE;
            }
        }
    }

    public class FileNode {
        public String handle;
        public String path;
        public String uuid = "";
        public PackagerIngestAction action;
        int type;
        Map<String, List<FileNode>> rels;

        public FileNode(String handle, String path, Map<String, List<FileNode>> rels, String uuid, int type) {
            this.handle = handle;
            this.path = path;
            this.rels = rels;
            this.type = type;
            if (uuid != null) {
                this.uuid = uuid;
            }
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public void setAction(PackagerIngestAction action) {
            this.action = action;
        }

        public void print(PrintStream printStream) {
            print(printStream, "");
        }

        public void getRelMapInit(Map<String, List<String>> relMap) {
            getRelMap(relMap);
        }

        public void print(PrintStream printStream, String prefix) {
            printStream.println(prefix + path + "  " + uuid + "  (" + actionMap.get(action) + ")");
            for (String relName : rels.keySet()) {
                System.out.println(prefix + "  " + relName);
                for (FileNode child : rels.get(relName)) {
                    child.print(printStream, prefix + "    ");
                }
            }
        }

        public void getRelMap(Map<String, List<String>> relMap) {
            for (String relName : rels.keySet()) {
                List<String> uuidList = new ArrayList<>();
                for (FileNode child : rels.get(relName)) {
                    uuidList.add(child.uuid);
                    child.getRelMapInit(relMap);
                }
                relMap.put(relName, uuidList);
            }
        }

        public Map<String, Map<String,List<String>>> getPathToRelMap() {
            Map<String, Map<String,List<String>>> pathToRelMap = new HashMap<>();
            for (String relation : rels.keySet()) {
                Map<String, List<String>> relPath = new HashMap<>();
                List<String> paths = new ArrayList<>();
                for (FileNode childNode : rels.get(relation)) {
                    paths.add(childNode.path);
                }
                relPath.put(relation, paths);
                pathToRelMap.put(path, relPath);
            }
            return pathToRelMap;
        }

        public void getTreePaths(List<String> filePaths) {
            filePaths.add(path);
            for (String relName : rels.keySet()) {
                for (FileNode child : rels.get(relName)) {
                    child.getTreePaths(filePaths);
                }
            }
        }
    }

    public enum PackagerIngestAction {
        SKIP_RESTORE_IN_DSPACE, // if -r -k and is in dspace, we skip it
        SKIP_RELATED_FILE_NOT_FOUND, // if referenced file is not found, we skip it
        CREATE,  // if -s (ingest)
        FAIL_RESTORE_IN_DSPACE,  // if -r and was in dspace
        RESTORE_NOT_IN_DSPACE,  // if -r (or -r -k) (or -r -f) and wasn't in dspace
        REPLACE_IN_DSPACE,  // if -r -f and was in dspace
        REPLACE_NOT_IN_DSPACE;  // if -r -f and was not in dspace
    }
}
