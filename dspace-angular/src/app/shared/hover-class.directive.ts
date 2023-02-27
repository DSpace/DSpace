import { Directive, ElementRef, HostListener, Input } from '@angular/core';

@Directive({
  selector: '[dsHoverClass]'
})
/**
 * A directive adding a class to an element when hovered over
 */
export class HoverClassDirective {
  /**
   * The name of the class to add on hover
   */
  @Input('dsHoverClass') hoverClass: string;

  constructor(public elementRef: ElementRef) { }

  /**
   * On mouse enter, add the class to the element's class list
   */
  @HostListener('mouseenter') onMouseEnter() {
    this.elementRef.nativeElement.classList.add(this.hoverClass);
  }

  /**
   * On mouse leave, remove the class from the element's class list
   */
  @HostListener('mouseleave') onMouseLeave() {
    this.elementRef.nativeElement.classList.remove(this.hoverClass);
  }

}
