/**
 * An interface to represent an access condition.
 */
export class SherpaPoliciesDetailsObject {

  /**
   * The sherpa policies error
   */
  error: boolean;

  /**
   * The sherpa policies journal details
   */
  journals: Journal[];

  /**
   * The sherpa policies message
   */
  message: string;

  /**
   * The sherpa policies metadata
   */
  metadata: Metadata;

}


export interface Metadata {
  id: number;
  uri: string;
  dateCreated: string;
  dateModified: string;
  inDOAJ: boolean;
  publiclyVisible: boolean;
}


export interface Journal {
  titles: string[];
  url: string;
  issns: string[];
  romeoPub: string;
  zetoPub: string;
  inDOAJ: boolean;
  publisher: Publisher;
  publishers: Publisher[];
  policies: Policy[];
}

export interface Publisher {
  name: string;
  relationshipType: string;
  country: string;
  uri: string;
  identifier: string;
  paidAccessDescription: string;
  paidAccessUrl: string;
  publicationCount: number;
}

export interface Policy {
  id: number;
  openAccessPermitted: boolean;
  uri: string;
  internalMoniker: string;
  permittedVersions: PermittedVersions[];
  urls: any;
  publicationCount: number;
  preArchiving: string;
  postArchiving: string;
  pubArchiving: string;
  openAccessProhibited: boolean;
}

export interface PermittedVersions {
  articleVersion: string;
  option: number;
  conditions: string[];
  prerequisites: string[];
  locations: string[];
  licenses: string[];
  embargo: Embargo;
}

export interface Embargo {
  units: any;
  amount: any;
}
