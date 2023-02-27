import { SectionsType } from '../sections/sections-type';
import { SectionVisibility } from './section-visibility.model';
import { WorkspaceitemSectionDataType } from '../../core/submission/models/workspaceitem-sections.model';
import { SubmissionSectionError } from './submission-section-error.model';

/**
 * An interface to represent section object state
 */
export interface SubmissionSectionObject {
  /**
   * The section header
   */
  header: string;

  /**
   * The section configuration url
   */
  config: string;

  /**
   * A boolean representing if this section is mandatory
   */
  mandatory: boolean;

  /**
   * The section type
   */
  sectionType: SectionsType;

  /**
   * The section visibility
   */
  visibility: SectionVisibility;

  /**
   * A boolean representing if this section is collapsed
   */
  collapsed: boolean;

  /**
   * A boolean representing if this section is enabled
   */
  enabled: boolean;

  /**
   * The list of the metadata ids of the section.
   */
  metadata: string[];

  /**
   * The section data object
   */
  data: WorkspaceitemSectionDataType;

  /**
   * The list of the section's errors to show. It contains the error list to display when section is not pristine
   */
  errorsToShow: SubmissionSectionError[];

  /**
   * The list of the section's errors detected by the server. They may not be shown yet if section is pristine
   */
  serverValidationErrors: SubmissionSectionError[];

  /**
   * A boolean representing if this section is loading
   */
  isLoading: boolean;

  /**
   * A boolean representing if this section is valid
   */
  isValid: boolean;

  /**
   * The formId related to this section
   */
  formId: string;
}
