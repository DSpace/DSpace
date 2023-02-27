import { inheritSerialization } from 'cerialize';
import { typedObject } from '../cache/builders/build-decorators';
import { DSpaceObject } from './dspace-object.model';
import { SITE } from './site.resource-type';

/**
 * Model class for the Site object
 */
@typedObject
@inheritSerialization(DSpaceObject)
export class Site extends DSpaceObject {

  static type = SITE;

}
