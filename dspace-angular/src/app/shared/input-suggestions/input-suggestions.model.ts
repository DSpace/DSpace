/**
 * Interface representing a single suggestion for the input suggestions component
 */
export interface InputSuggestion {
  /**
   * The displayed value of the suggestion
   */
  displayValue: string;

  /**
   * The search query that can be used with filter suggestion.
   * It contains the value within the operator :
   *  - value,equals
   *  - value,authority
   */
  query?: string;

  /**
   * The actual value of the suggestion
   */
  value: string;
}
