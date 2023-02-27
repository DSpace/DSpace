/**
 * A class representing an email to send back to the user requesting an item
 */
export class RequestCopyEmail {
  constructor(public subject: string,
              public message: string) {
  }
}
