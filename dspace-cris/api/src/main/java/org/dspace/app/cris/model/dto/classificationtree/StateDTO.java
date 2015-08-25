package org.dspace.app.cris.model.dto.classificationtree;

public class StateDTO {
	
	private boolean opened = true;
	
	private boolean disabled = false;
	
	private boolean selected = false;

	public boolean isOpened() {
		return opened;
	}

	public void setOpened(boolean opened) {
		this.opened = opened;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
}
