import { Config } from './config.interface';

/**
 * Config that determines how the dropdown list of years are created for browse-by-date components
 */
export interface HomeConfig extends Config {
  recentSubmissions: {
    /**
   * The number of item showing in recent submission components
   */
    pageSize: number;

    /**
     * sort record of recent submission
     */
    sortField: string;
  }

  topLevelCommunityList: {
    pageSize: number;
  };
}
