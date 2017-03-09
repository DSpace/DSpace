package edu.tamu.metadatatreebrowser;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class MetadataTreeService {

	private static MetadataTreeService metadataTreeService;
	private Map<String,MetadataTreeEntry> metadataTrees = new HashMap<String,MetadataTreeEntry>();
	
	private MetadataTreeService() {
	}
	
	static protected MetadataTreeService getInstance() {
		if (metadataTreeService == null) {
			metadataTreeService = new MetadataTreeService();
		}
		return metadataTreeService;
	}
	
	private MetadataTreeNode fetchBrowseTree(Context context, DSpaceObject dso) {
		try {
			return MetadataTreeNode.generateBrowseTree(context, dso);
		} catch (SQLException | AuthorizeException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected MetadataTreeNode getFullTree(Context context, DSpaceObject dso) {
		String handle = dso.getHandle();
		MetadataTreeEntry treeEntry = null;  
		if (metadataTrees.containsKey(handle)) {
			treeEntry = metadataTrees.get(handle);  
			if (treeEntry.isExpired()) {
				treeEntry.setTree(fetchBrowseTree(context, dso));
				treeEntry.resetAge();
			}
		} else {
			metadataTrees.put(handle,new MetadataTreeEntry(fetchBrowseTree(context, dso)));
			treeEntry = metadataTrees.get(handle);
		}
		
		return treeEntry.getTree();
	}
	
	private class MetadataTreeEntry {
		private Long age;
		private Long lifetime = 480L;
		private MetadataTreeNode tree;

		public MetadataTreeEntry(MetadataTreeNode tree,Long lifetime) {
			setTree(tree);
			resetAge();
			if (lifetime != null) {
				setLifetime(lifetime);
			}
		}
		
		public MetadataTreeEntry(MetadataTreeNode tree) {
			this(tree,null);
		}

		public Long getAge() {
			return age;
		}

		private void setAge(Long age) {
			this.age = age;
		}

		private Long getLifetime() {
			return lifetime;
		}

		private void setLifetime(Long lifetime) {
			this.lifetime = lifetime;
		}

		public MetadataTreeNode getTree() {
			return tree;
		}

		public void setTree(MetadataTreeNode tree) {
			this.tree = tree;
		}
		
		public void resetAge() {
			setAge(System.currentTimeMillis() / 1000L);
		}
		
		public boolean isExpired() {
			return ((System.currentTimeMillis() / 1000L) > (getAge()+(getLifetime()*60)));
		}
	}
}
