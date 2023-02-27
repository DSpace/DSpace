import { autoserialize, inheritSerialization } from 'cerialize';
import { typedObject } from '../../cache/builders/build-decorators';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { ResourceType } from '../../shared/resource-type';
import { HALResource } from '../../shared/hal-resource.model';
import { USAGE_REPORT } from './usage-report.resource-type';
import { HALLink } from '../../shared/hal-link.model';
import { deserialize, autoserializeAs } from 'cerialize';

/**
 * A usage report.
 */
@typedObject
@inheritSerialization(HALResource)
export class UsageReport extends HALResource {

  static type = USAGE_REPORT;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  @autoserialize
  id: string;

  @autoserializeAs('report-type')
  reportType: string;

  @autoserialize
  points: Point[];

  @deserialize
  _links: {
    self: HALLink;
  };
}

/**
 * A statistics data point.
 */
export interface Point {
  id: string;
  label: string;
  type: string;
  values: {
    views: number;
  }[];
}
