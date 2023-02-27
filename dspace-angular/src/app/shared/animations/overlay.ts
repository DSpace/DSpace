import { animate, state, transition, trigger, style } from '@angular/animations';

export const overlay = trigger('overlay', [

  state('show', style({ opacity: 0.5 })),

  state('hide', style({ opacity: 0 })),

  transition('show <=> hide', animate(250))
]);
