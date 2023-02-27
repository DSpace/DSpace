import { autoserialize, inheritSerialization } from 'cerialize';
import { typedObject } from '../cache/builders/build-decorators';
import { DSpaceObject } from './dspace-object.model';
import { LICENSE } from './license.resource-type';

@typedObject
@inheritSerialization(DSpaceObject)
export class License extends DSpaceObject {
  static type = LICENSE;

  /**
   * Is the license custom?
   */
  @autoserialize
  custom: boolean;

  /**
   * The text of the license
   */
  @autoserialize
  text: string;
}
