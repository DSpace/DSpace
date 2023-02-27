import { autoserialize, deserialize, inheritSerialization } from 'cerialize';
import { SectionsType } from '../../../submission/sections/sections-type';
import { typedObject } from '../../cache/builders/build-decorators';
import { HALLink } from '../../shared/hal-link.model';
import { ConfigObject } from './config.model';
import { SUBMISSION_SECTION_TYPE } from './config-type';

/**
 * An interface that define section visibility and its properties.
 */
export interface SubmissionSectionVisibility {
  main: any;
  other: any;
}

@typedObject
@inheritSerialization(ConfigObject)
export class SubmissionSectionModel extends ConfigObject {
  static type = SUBMISSION_SECTION_TYPE;

  /**
   * The header for this section
   */
  @autoserialize
  header: string;

  /**
   * A boolean representing if this submission section is the mandatory or not
   */
  @autoserialize
  mandatory: boolean;

  /**
   * A string representing the kind of section object
   */
  @autoserialize
  sectionType: SectionsType;

  /**
   * The [SubmissionSectionVisibility] object for this section
   */
  @autoserialize
  visibility: SubmissionSectionVisibility;

  /**
   * The {@link HALLink}s for this SubmissionSectionModel
   */
  @deserialize
  _links: {
    self: HALLink;
    config: HALLink;
  };

}
