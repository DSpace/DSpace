import { Directive, EventEmitter, HostListener, Output } from '@angular/core';

@Directive({
  selector: '[dsDragClick]'
})

/**
 * Directive for preventing drag events being misinterpret as clicks
 * The difference is made using the time the mouse button is pushed down
 */
export class DragClickDirective {
  /**
   * The start time of the mouse down event in milliseconds
   */
  private start;

  /**
   * Emits a click event when the click is perceived as an actual click and not a drag
   */
  @Output() actualClick = new EventEmitter();

  /**
   * When the mouse button is pushed down, register the start time
   * @param event Mouse down event
   */
  @HostListener('mousedown', ['$event'])
  mousedownEvent(event) {
    this.start = new Date();
  }

  /**
   * When the mouse button is let go of, check how long if it was down for
   * If the mouse button was down for more than 250ms, don't emit a click event
   * @param event Mouse down event
   */
  @HostListener('mouseup', ['$event'])
  mouseupEvent(event) {
    const end: any = new Date();
    const clickTime = end - this.start;
    if (clickTime < 250) {
      this.actualClick.emit(event);
    }
  }
}
