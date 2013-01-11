package org.dspace.orm.entity.content;

public enum DSpaceObjectType {
	BITSTREAM(0),
	BUNDLE(1),
	ITEM(2),
	COLLECTION(3),
	COMMUNITY(4),
	SITE(5),
	GROUP(6),
	EPERSON(7),
	WORKSPACE_ITEM(8),
	BITSTREAM_FORMAT(9),
	WORKFLOW_ITEM(10),
	VERSION_ITEM(11),
	HANDLE(12),
	RESOURCE_POLICY(13),
	REGISTRATION_DATA(14),
	HARVESTED_ITEM(15),
	HARVESTED_COLLECTION(16),
	METADATA(17),
	FILE_EXTENSION(18);
	
	
	private final int id;

	
	DSpaceObjectType(int id) { this.id = id; }
	
	public int getId () {
		return id;
	}
	
	public static DSpaceObjectType getById(int id)
    {
		DSpaceObjectType type = null;

        for (DSpaceObjectType bonusTypeTemp : DSpaceObjectType.values())
        {
            if(id == bonusTypeTemp.id)
            {
                type = bonusTypeTemp;
                break;
            }
        }

        return type;
    }
}
