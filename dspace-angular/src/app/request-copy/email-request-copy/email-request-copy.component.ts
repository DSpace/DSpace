import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RequestCopyEmail } from './request-copy-email.model';
import { Location } from '@angular/common';

@Component({
  selector: 'ds-email-request-copy',
  styleUrls: ['./email-request-copy.component.scss'],
  templateUrl: './email-request-copy.component.html'
})
/**
 * A form component for an email to send back to the user requesting an item
 */
export class EmailRequestCopyComponent {
  /**
   * Event emitter for sending the email
   */
  @Output() send: EventEmitter<RequestCopyEmail> = new EventEmitter<RequestCopyEmail>();

  /**
   * The subject of the email
   */
  @Input() subject: string;

  /**
   * The contents of the email
   */
  @Input() message: string;

  constructor(protected location: Location) {
  }

  /**
   * Submit the email
   */
  submit() {
    this.send.emit(new RequestCopyEmail(this.subject, this.message));
  }

  /**
   * Return to the previous page
   */
  return() {
    this.location.back();
  }
}
