/**
 * Represents a search filter
 */

export class SearchFilter {
  key: string;
  values: string[];
  operator: string;

  constructor(key: string, values: string[], operator?: string) {
    this.key = key;
    this.values = values;
    this.operator = operator;
  }
}
