/**
 * A single link in the _links section of a {@link HALResource}
 */
export class HALLink {

  /**
   * The url of the {@link HALLink}'s target
   */
  href: string;

  /**
   * The name of the {@link HALLink}
   */
  name?: string;

  /**
   * A boolean indicating whether the href contains a template.
   *
   * e.g. if href is "http://haltalk.herokuapp.com/docs/{rel}"
   * {rel} would be the template
   */
  templated?: boolean;
}
