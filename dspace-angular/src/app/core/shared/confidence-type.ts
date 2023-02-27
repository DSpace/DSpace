export enum ConfidenceType {
  /**
   * This authority value has been confirmed as accurate by an
   * interactive user or authoritative policy
   */
  CF_ACCEPTED = 600,

  /**
   * Value is singular and valid but has not been seen and accepted
   * by a human, so its provenance is uncertain.
   */
  CF_UNCERTAIN = 500,

  /**
   * There are multiple matching authority values of equal validity.
   */
  CF_AMBIGUOUS = 400,

  /**
   * There are no matching answers from the authority.
   */
  CF_NOTFOUND = 300,

  /**
   * The authority encountered an internal failure - this preserves a
   * record in the metadata of why there is no value.
   */
  CF_FAILED = 200,

  /**
   * The authority recommends this submission be rejected.
   */
  CF_REJECTED = 100,

  /**
   * No reasonable confidence value is available
   */
  CF_NOVALUE = 0,

  /**
   * Value has not been set (DB default).
   */
  CF_UNSET = -1
}
