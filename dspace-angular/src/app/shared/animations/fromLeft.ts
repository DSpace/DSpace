import { animate, state, style, transition, trigger } from '@angular/animations';

export const fromLeftInState = state('fromLeftIn', style({opacity: 1, transform: 'translateX(0)'}));
export const fromLeftEnter = transition('* => fromLeftIn', [
  style({opacity: 0, transform: 'translateX(-5%)'}),
  animate('400ms ease-in-out')
]);

export const fromLeftOutState = state('fromLeftOut', style({opacity: 0, transform: 'translateX(5%)'}));
export const fromLeftLeave = transition('fromLeftIn => fromLeftOut', [
  style({opacity: 1, transform: 'translateX(0)'}),
  animate('300ms ease-in-out')
]);

export const fromLeftIn = trigger('fromLeftIn', [
  fromLeftEnter
]);

export const fromLeftOut = trigger('fromLeftOut', [
  fromLeftLeave
]);

export const fromLeftInOut = trigger('fromLeftInOut', [
  fromLeftEnter,
  fromLeftLeave
]);
