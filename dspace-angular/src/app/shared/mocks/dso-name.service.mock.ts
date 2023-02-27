import { DSpaceObject } from '../../core/shared/dspace-object.model';

export const UNDEFINED_NAME = 'Undefined';

export class DSONameServiceMock {
  public getName(dso: DSpaceObject) {
    return UNDEFINED_NAME;
  }

  public getHitHighlights(object: any, dso: DSpaceObject) {
    if (object.hitHighlights && object.hitHighlights['dc.title']) {
      return object.hitHighlights['dc.title'][0].value;
    } else if (object.hitHighlights && object.hitHighlights['organization.legalName']) {
      return object.hitHighlights['organization.legalName'][0].value;
    } else if (object.hitHighlights && (object.hitHighlights['person.familyName'] || object.hitHighlights['person.givenName'])) {
      if (object.hitHighlights['person.familyName'] && object.hitHighlights['person.givenName']) {
        return `${object.hitHighlights['person.familyName'][0].value}, ${object.hitHighlights['person.givenName'][0].value}`;
      }
      if (object.hitHighlights['person.familyName']) {
        return `${object.hitHighlights['person.familyName'][0].value}`;
      }
      if (object.hitHighlights['person.givenName']) {
        return `${object.hitHighlights['person.givenName'][0].value}`;
      }
    }
    return UNDEFINED_NAME;
  }
}
