import { Option } from './submission-cc-license.model';

/**
 * An interface to represent the submission's creative commons license section data.
 */
export interface WorkspaceitemSectionCcLicenseObject {
  ccLicense?: {
    id: string;
    fields: {
      [fieldId: string]: Option;
    }
  };
  uri?: string;
  accepted?: boolean;
}
