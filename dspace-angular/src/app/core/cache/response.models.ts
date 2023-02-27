/* eslint-disable max-classes-per-file */
import { PageInfo } from '../shared/page-info.model';
import { ConfigObject } from '../config/models/config.model';
import { DSpaceObject } from '../shared/dspace-object.model';
import { HALLink } from '../shared/hal-link.model';
import { UnCacheableObject } from '../shared/uncacheable-object.model';
import { RequestError } from '../data/request-error.model';

export class RestResponse {
  public toCache = true;
  public timeCompleted: number;

  constructor(
    public isSuccessful: boolean,
    public statusCode: number,
    public statusText: string
  ) {
  }
}

export class ParsedResponse extends RestResponse {
  constructor(statusCode: number, public link?: HALLink, public unCacheableObject?: UnCacheableObject) {
    super(true, statusCode, `${statusCode}`);
  }
}

export class DSOSuccessResponse extends RestResponse {
  constructor(
    public resourceSelfLinks: string[],
    public statusCode: number,
    public statusText: string,
    public pageInfo?: PageInfo
  ) {
    super(true, statusCode, statusText);
  }
}

export class EndpointMap {
  [linkPath: string]: HALLink
}

export class EndpointMapSuccessResponse extends RestResponse {
  constructor(
    public endpointMap: EndpointMap,
    public statusCode: number,
    public statusText: string
  ) {
    super(true, statusCode, statusText);
  }
}

export class ErrorResponse extends RestResponse {
  errorMessage: string;

  constructor(error: RequestError) {
    super(false, error.statusCode, error.statusText);
    console.error(error);
    this.errorMessage = error.message;
  }
}

export class ConfigSuccessResponse extends RestResponse {
  constructor(
    public configDefinition: ConfigObject,
    public statusCode: number,
    public statusText: string,
    public pageInfo?: PageInfo
  ) {
    super(true, statusCode, statusText);
  }
}

/**
 * A REST Response containing a token
 */
export class TokenResponse extends RestResponse {
  constructor(
    public token: string,
    public isSuccessful: boolean,
    public statusCode: number,
    public statusText: string
  ) {
    super(isSuccessful, statusCode, statusText);
  }
}

export class PostPatchSuccessResponse extends RestResponse {
  constructor(
    public dataDefinition: any,
    public statusCode: number,
    public statusText: string,
    public pageInfo?: PageInfo
  ) {
    super(true, statusCode, statusText);
  }
}

export class EpersonSuccessResponse extends RestResponse {
  constructor(
    public epersonDefinition: DSpaceObject[],
    public statusCode: number,
    public statusText: string,
    public pageInfo?: PageInfo
  ) {
    super(true, statusCode, statusText);
  }
}

export class MessageResponse extends RestResponse {
  public toCache = false;

  constructor(
    public statusCode: number,
    public statusText: string,
    public pageInfo?: PageInfo
  ) {
    super(true, statusCode, statusText);
  }
}

export class TaskResponse extends RestResponse {
  public toCache = false;

  constructor(
    public statusCode: number,
    public statusText: string,
    public pageInfo?: PageInfo
  ) {
    super(true, statusCode, statusText);
  }
}

export class FilteredDiscoveryQueryResponse extends RestResponse {
  constructor(
    public filterQuery: string,
    public statusCode: number,
    public statusText: string,
    public pageInfo?: PageInfo
  ) {
    super(true, statusCode, statusText);
  }
}
