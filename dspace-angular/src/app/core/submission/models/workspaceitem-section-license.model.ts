
/**
 * An interface to represent submission's license section data.
 */
export interface WorkspaceitemSectionLicenseObject {
  /**
   * The license url
   */
  url: string;

  /**
   * The acceptance date of the license
   */
  acceptanceDate: string;

  /**
   * A boolean representing if license has been granted
   */
  granted: boolean;
}
