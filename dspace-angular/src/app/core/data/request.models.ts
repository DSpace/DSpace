/* eslint-disable max-classes-per-file */
import { GenericConstructor } from '../shared/generic-constructor';
import { ResponseParsingService } from './parsing.service';
import { EndpointMapResponseParsingService } from './endpoint-map-response-parsing.service';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import {
  SubmissionResponseParsingService
} from '../submission/submission-response-parsing.service';
import { RestRequestMethod } from './rest-request-method';
import { TaskResponseParsingService } from '../tasks/task-response-parsing.service';
import { ContentSourceResponseParsingService } from './content-source-response-parsing.service';
import { RestRequestWithResponseParser } from './rest-request-with-response-parser.model';
import { DspaceRestResponseParsingService } from './dspace-rest-response-parsing.service';
import { FindListOptions } from './find-list-options.model';


// uuid and handle requests have separate endpoints
export enum IdentifierType {
  UUID = 'uuid',
  HANDLE = 'handle'
}

class DSpaceRestRequest extends RestRequestWithResponseParser {
  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return DspaceRestResponseParsingService;
  }
}

export class GetRequest extends DSpaceRestRequest {
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  ) {
    super(uuid, href, RestRequestMethod.GET, body, options);
  }
}

export class PostRequest extends DSpaceRestRequest {
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  ) {
    super(uuid, href, RestRequestMethod.POST, body);
  }
}

/**
 * Request representing a multipart post request
 */
export class MultipartPostRequest extends DSpaceRestRequest {
  public isMultipart = true;
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  )  {
    super(uuid, href, RestRequestMethod.POST, body);
  }
}

export class PutRequest extends DSpaceRestRequest {
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  ) {
    super(uuid, href, RestRequestMethod.PUT, body);
  }
}

export class DeleteRequest extends DSpaceRestRequest {
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  ) {
    super(uuid, href, RestRequestMethod.DELETE, body);
  }
}

export class OptionsRequest extends DSpaceRestRequest {
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  ) {
    super(uuid, href, RestRequestMethod.OPTIONS, body);
  }
}

export class HeadRequest extends DSpaceRestRequest {
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  ) {
    super(uuid, href, RestRequestMethod.HEAD, body);
  }
}

export class PatchRequest extends DSpaceRestRequest {
  constructor(
    public uuid: string,
    public href: string,
    public body?: any,
    public options?: HttpOptions
  ) {
    super(uuid, href, RestRequestMethod.PATCH, body);
  }
}

export class FindListRequest extends GetRequest {
  constructor(
    uuid: string,
    href: string,
    public body?: FindListOptions,
  ) {
    super(uuid, href);
  }
}

export class EndpointMapRequest extends GetRequest {
  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return EndpointMapResponseParsingService;
  }
}

/**
 * Class representing a submission HTTP GET request object
 */
export class SubmissionRequest extends GetRequest {
  constructor(uuid: string, href: string) {
    super(uuid, href);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return SubmissionResponseParsingService;
  }
}

/**
 * Class representing a submission HTTP DELETE request object
 */
export class SubmissionDeleteRequest extends DeleteRequest {
  constructor(public uuid: string,
              public href: string) {
    super(uuid, href);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return SubmissionResponseParsingService;
  }
}

/**
 * Class representing a submission HTTP PATCH request object
 */
export class SubmissionPatchRequest extends PatchRequest {
  constructor(public uuid: string,
              public href: string,
              public body?: any) {
    super(uuid, href, body);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return SubmissionResponseParsingService;
  }
}

/**
 * Class representing a submission HTTP POST request object
 */
export class SubmissionPostRequest extends PostRequest {
  constructor(public uuid: string,
              public href: string,
              public body?: any,
              public options?: HttpOptions) {
    super(uuid, href, body, options);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return SubmissionResponseParsingService;
  }
}

export class CreateRequest extends PostRequest {
  constructor(uuid: string, href: string, public body?: any, public options?: HttpOptions) {
    super(uuid, href, body, options);
  }
}

export class ContentSourceRequest extends GetRequest {
  constructor(uuid: string, href: string) {
    super(uuid, href);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return ContentSourceResponseParsingService;
  }
}

export class UpdateContentSourceRequest extends PutRequest {
  constructor(uuid: string, href: string, public body?: any, public options?: HttpOptions) {
    super(uuid, href, body, options);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return ContentSourceResponseParsingService;
  }
}

/**
 * Request to delete an object based on its identifier
 */
export class DeleteByIDRequest extends DeleteRequest {
  constructor(
    uuid: string,
    href: string,
    public resourceID: string
  ) {
    super(uuid, href);
  }
}

export class TaskPostRequest extends PostRequest {
  constructor(uuid: string, href: string, public body?: any, public options?: HttpOptions) {
    super(uuid, href, body, options);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return TaskResponseParsingService;
  }
}

export class TaskDeleteRequest extends DeleteRequest {
  constructor(uuid: string, href: string, public body?: any, public options?: HttpOptions) {
    super(uuid, href, body, options);
  }

  getResponseParser(): GenericConstructor<ResponseParsingService> {
    return TaskResponseParsingService;
  }
}

export class MyDSpaceRequest extends GetRequest {
  public responseMsToLive = 10 * 1000;
}
