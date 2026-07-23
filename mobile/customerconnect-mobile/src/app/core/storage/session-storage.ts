import { Injectable, InjectionToken } from '@angular/core';

export interface SessionStoragePort {
  read<T>(key: string): T | null;
  write<T>(key: string, value: T): void;
  remove(key: string): void;
}

export const SESSION_STORAGE = new InjectionToken<SessionStoragePort>('SESSION_STORAGE');

@Injectable()
export class WebSessionStorage implements SessionStoragePort {
  // Browser fallback. Replace this adapter with a native secure-storage plugin for device builds.
  read<T>(key: string): T | null {
    try {
      const value = sessionStorage.getItem(key);
      return value ? (JSON.parse(value) as T) : null;
    } catch {
      return null;
    }
  }
  write<T>(key: string, value: T): void {
    sessionStorage.setItem(key, JSON.stringify(value));
  }
  remove(key: string): void {
    sessionStorage.removeItem(key);
  }
}
