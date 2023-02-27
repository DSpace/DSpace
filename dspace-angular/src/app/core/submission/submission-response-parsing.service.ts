import { Injectable } from '@angular/core';
import { deepClone } from 'fast-json-patch';
import { DSOResponseParsingService } from '../data/dso-response-parsing.service';

import { ResponseParsingService } from '../data/parsing.service';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { ParsedResponse } from '../cache/response.models';
import { isEmpty, isNotEmpty, isNotNull } from '../../shared/empty.util';
import { ConfigObject } from '../config/models/config.model';
import { BaseResponseParsingService } from '../data/base-response-parsing.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { FormFieldMetadataValueObject } from '../../shared/form/builder/models/form-field-metadata-value.model';
import { SubmissionObject } from './models/submission-object.model';
import { WorkflowItem } from './models/workflowitem.model';
import { WorkspaceItem } from './models/workspaceitem.model';
import { RestRequest } from '../data/rest-request.model';

/**
 * Export a function to check if object has same properties of FormFieldMetadataValueObject
 *
 * @param obj
 */
export function isServerFormValue(obj: any): boolean {
  return (typeof obj === 'object'
    && obj.hasOwnProperty('value')
    && obj.hasOwnProperty('language')
    && obj.hasOwnProperty('authority')
    && obj.hasOwnProperty('confidence'));
}

/**
 * Export a function to normalize sections object of the server response
 *
 * @param obj
 * @param objIndex
 */
export function normalizeSectionData(obj: any, objIndex?: number) {
  let result: any = obj;
  if (isNotNull(obj)) {
    // If is an Instance of FormFieldMetadataValueObject normalize it
    if (typeof obj === 'object' && isServerFormValue(obj)) {
      // If authority property is set normalize as a FormFieldMetadataValueObject object
      /* NOTE: Data received from server could have authority property equal to null, but into form
         field's model is required a FormFieldMetadataValueObject object as field value, so instantiate it */
      result = new FormFieldMetadataValueObject(
        obj.value,
        obj.language,
        obj.authority,
        (obj.display || obj.value),
        obj.place || objIndex,
        obj.confidence,
        obj.otherInformation
      );
    } else if (Array.isArray(obj)) {
      result = [];
      obj.forEach((item, index) => {
        result[index] = normalizeSectionData(item, index);
      });
    } else if (typeof obj === 'object') {
      result = Object.create({});
      Object.keys(obj)
        .forEach((key) => {
          result[key] = normalizeSectionData(obj[key]);
        });
    }
  }
  return result;
}

/**
 * Provides methods to parse response for a submission request.
 */
@Injectable()
export class SubmissionResponseParsingService extends BaseResponseParsingService implements ResponseParsingService {

  protected toCache = false;

  /**
   * The submission assumes certain related HALResources will always be embedded.
   * It only works if the responseparser finds these embedded resources, and directly
   * attaches them to the requested object, instead of putting them in the cache and
   * treating them as separate objects. This boolean was added to allow us to disable
   * that behavior for the rest of the application, while keeping it for the submission.
   *
   * It should be removed after the submission has been refactored to treat embeds as
   * resources that may need to be retrieved separately.
   */
  protected shouldDirectlyAttachEmbeds = true;

  constructor(protected objectCache: ObjectCacheService,
              protected dsoParser: DSOResponseParsingService
  ) {
    super();
  }

  /**
   * Parses data from the workspaceitems/workflowitems endpoints
   *
   * @param {RestRequest} request
   * @param {RawRestResponse} data
   * @returns {RestResponse}
   */
  parse(request: RestRequest, data: RawRestResponse): ParsedResponse {
    this.dsoParser.parse(deepClone(request), deepClone(data));
    if (isNotEmpty(data.payload)
      && isNotEmpty(data.payload._links)
      && this.isSuccessStatus(data.statusCode)) {
      const dataDefinition = this.processResponse<SubmissionObject | ConfigObject>(data.payload, request);
      return new ParsedResponse(data.statusCode, undefined, { dataDefinition });
    } else if (isEmpty(data.payload) && this.isSuccessStatus(data.statusCode)) {
      return new ParsedResponse(data.statusCode);
    } else {
      throw new Error('Unexpected response from server');
    }
  }

  /**
   * Parses response and normalize it
   *
   * @param {RawRestResponse} data
   * @param {RestRequest} request
   * @returns {any[]}
   */
  protected processResponse<ObjectDomain>(data: any, request: RestRequest): any[] {
    const dataDefinition = this.process<ObjectDomain>(data, request);
    const definition = Array.of();
    const processedList = Array.isArray(dataDefinition) ? dataDefinition : Array.of(dataDefinition);

    processedList.forEach((item) => {

      // item = Object.assign({}, item);
      // In case data is an Instance of WorkspaceItem normalize field value of all the section of type form
      if (item instanceof WorkspaceItem
        || item instanceof WorkflowItem) {
        if (item.sections) {
          const precessedSection = Object.create({});
          // Iterate over all workspaceitem's sections
          Object.keys(item.sections)
            .forEach((sectionId) => {
              if (typeof item.sections[sectionId] === 'object' && (isNotEmpty(item.sections[sectionId]) &&
                // When Upload section is disabled, add to submission only if there are files
                (!item.sections[sectionId].hasOwnProperty('files') || isNotEmpty((item.sections[sectionId] as any).files)))) {

                const sectiondata = Object.create({});
                // Iterate over all sections property
                Object.keys(item.sections[sectionId])
                  .forEach((metdadataId) => {
                    const entry = item.sections[sectionId][metdadataId];
                    // If entry is not an array, for sure is not a section of type form
                    if (Array.isArray(entry)) {
                      sectiondata[metdadataId] = [];
                      entry.forEach((valueItem, index) => {
                        // Parse value and normalize it
                        const normValue = normalizeSectionData(valueItem, index);
                        if (isNotEmpty(normValue)) {
                          sectiondata[metdadataId].push(normValue);
                        }
                      });
                    } else {
                      sectiondata[metdadataId] = entry;
                    }
                  });
                precessedSection[sectionId] = sectiondata;
              }
            });
          item = Object.assign({}, item, { sections: precessedSection });
        }
      }
      definition.push(item);
    });

    return definition;
  }
}
