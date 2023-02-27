import { Directive, AfterViewInit, ElementRef, Input } from '@angular/core';
import { isNotEmpty } from '../empty.util';

/**
 * Directive to set focus on an element when it is rendered
 */
@Directive({
  selector: '[dsAutoFocus]'
})
export class AutoFocusDirective implements AfterViewInit {

  /**
   * Optional input to specify which element in a component should get the focus
   * If left empty, the component itself will get the focus
   */
  @Input() autoFocusSelector: string = undefined;

  constructor(private el: ElementRef) {
  }

  ngAfterViewInit() {
    if (isNotEmpty(this.autoFocusSelector)) {
      return this.el.nativeElement.querySelector(this.autoFocusSelector).focus();
    } else {
      return this.el.nativeElement.focus();
    }
  }
}
