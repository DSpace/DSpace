import { Directive, ViewContainerRef } from '@angular/core';

@Directive({
  selector: '[dsListableObject]',
})
/**
 * Directive used as a hook to know where to inject the dynamic listable object component
 */
export class ListableObjectDirective {
  constructor(public viewContainerRef: ViewContainerRef) { }
}
