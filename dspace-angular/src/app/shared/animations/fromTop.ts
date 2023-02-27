import { animate, state, style, transition, trigger } from '@angular/animations';

export const fromTopInState = state('fromTopIn', style({opacity: 1, transform: 'translateY(0)'}));
export const fromTopEnter = transition('* => fromTopIn', [
  style({opacity: 0, transform: 'translateY(-5%)'}),
  animate('400ms ease-in-out')
]);

export const fromTopOutState = state('fromTopOut', style({opacity: 0, transform: 'translateY(5%)'}));
export const fromTopLeave = transition('fromTopIn => fromTopOut', [
  style({opacity: 1, transform: 'translateY(0)'}),
  animate('300ms ease-in-out')
]);

export const fromTopIn = trigger('fromTopIn', [
  fromTopEnter
]);

export const fromTopOut = trigger('fromTopOut', [
  fromTopLeave
]);

export const fromTopInOut = trigger('fromTopInOut', [
  fromTopEnter,
  fromTopLeave
]);
