import { Directive, ElementRef, Output, EventEmitter, HostListener } from '@angular/core';

@Directive({
  selector: '[dsClickOutside]'
})
/**
 * Directive to detect when the users clicks outside of the element the directive was put on
 */
export class ClickOutsideDirective {
  /**
   * Emits null when the user clicks outside of the element
   */
  @Output()
  public dsClickOutside = new EventEmitter();

  constructor(private _elementRef: ElementRef) {
  }

  @HostListener('document:click')
  public onClick() {
    const hostElement = this._elementRef.nativeElement;
    const focusElement = hostElement.ownerDocument.activeElement;
    const clickedInside = hostElement.contains(focusElement);
    if (!clickedInside) {
      this.dsClickOutside.emit(null);
    }
  }
}
