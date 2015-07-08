package uk.ac.edina.datashare.utils;

/**
 * User license enum.
 */
public enum UserLicense
{
    UNKNOWN(-1),  NO_LICENSE(1), CREATIVE_COMMONS(2), OPEN_DATA_COMMONS(3), ODC_ATTRIBUTION(4), CREATIVE_COMMONS_BY(5);
    
    final int value;

    UserLicense(int i)
    {
        value = i;
    }
    
    /**
     * Convert integer to UserLicense
     * @param i
     * @return
     */
    public static UserLicense convert(int i)
    {
        UserLicense license = UserLicense.UNKNOWN;
        
        switch(i)
        {
            case 1:
            {
                license = UserLicense.NO_LICENSE;
                break;
            }
            case 2:
            {
                license = UserLicense.CREATIVE_COMMONS;
                break;
            }
            case 3:
            {
                license = UserLicense.OPEN_DATA_COMMONS;
                break;
            }
            case 4:
            {
                license = UserLicense.ODC_ATTRIBUTION;
                break;
            }
            case 5:
            {
                license = UserLicense.CREATIVE_COMMONS_BY;
                break;                
            }
        }
        
        return license;
    }
}
