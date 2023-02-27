import { animate, state, style, transition, trigger } from '@angular/animations';

export const fadeInState = state('fadeIn', style({opacity: 1}));
export const fadeInEnter =  transition('* => fadeIn', [
  style({ opacity: 0 }),
  animate(300, style({ opacity: 1 }))
]);
const fadeEnter =  transition(':enter', [
  style({ opacity: 0 }),
  animate(300, style({ opacity: 1 }))
]);

export const fadeOutState = state('fadeOut', style({opacity: 0}));
export const fadeOutLeave = transition('fadeIn => fadeOut', [
  style({ opacity: 1 }),
  animate(400, style({ opacity: 0 }))
]);
const fadeLeave =  transition(':leave', [
  style({ opacity: 0 }),
  animate(300, style({ opacity: 1 }))
]);

export const fadeIn = trigger('fadeIn', [
  fadeEnter
]);

export const fadeOut = trigger('fadeOut', [
  fadeLeave
]);

export const fadeInOut = trigger('fadeInOut', [
  fadeEnter,
  fadeLeave
]);
