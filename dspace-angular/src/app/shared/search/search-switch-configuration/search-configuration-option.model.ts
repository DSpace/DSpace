/**
 * Represents a search configuration select option
 */
import { Context } from '../../../core/shared/context.model';

export interface SearchConfigurationOption {

  /**
   * The select option value
   */
  value: string;

  /**
   * The select option label
   */
  label: string;

  /**
   * The search context to use with the configuration
   */
  context: Context;
}
