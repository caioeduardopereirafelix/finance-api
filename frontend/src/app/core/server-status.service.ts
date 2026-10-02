import { Injectable, signal } from '@angular/core';

export const SLOW_RESPONSE_MS = 6000;

@Injectable({ providedIn: 'root' })
export class ServerStatusService {

  readonly waking = signal(false);

  private awake = false;

  watch(): ReturnType<typeof setTimeout> | undefined {
    if (this.awake) {
      return undefined;
    }
    return setTimeout(() => {
      if (!this.awake) {
        this.waking.set(true);
      }
    }, SLOW_RESPONSE_MS);
  }

  cancel(timer: ReturnType<typeof setTimeout> | undefined) {
    if (timer !== undefined) {
      clearTimeout(timer);
    }
  }

  markAwake() {
    this.awake = true;
    this.waking.set(false);
  }
}
