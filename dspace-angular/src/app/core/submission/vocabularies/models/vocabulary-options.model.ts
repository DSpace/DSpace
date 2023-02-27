/**
 * Representing vocabulary properties
 */
export class VocabularyOptions {

  /**
   * The name of the vocabulary
   */
  name: string;

  /**
   * A boolean representing if value is closely related to a vocabulary entry or not
   */
  closed: boolean;

  constructor(name: string,
              closed: boolean = false) {
    this.name = name;
    this.closed = closed;
  }
}
