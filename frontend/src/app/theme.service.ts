import {Inject, Injectable} from '@angular/core';
import {DOCUMENT} from '@angular/common';

type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {

  private readonly STORAGE_KEY = 'theme';
  private currentTheme: Theme = 'light';

  constructor(@Inject(DOCUMENT) private document: Document) {
    const stored = (localStorage.getItem(this.STORAGE_KEY) as Theme | null);

    if (stored === 'light' || stored === 'dark') {
      this.currentTheme = stored;
    } else {
      const prefersDark = window.matchMedia &&
        window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.currentTheme = prefersDark ? 'dark' : 'light';
    }

    this.applyTheme(this.currentTheme);
  }

  get theme(): Theme {
    return this.currentTheme;
  }

  toggleTheme(): void {
    this.currentTheme = this.currentTheme === 'dark' ? 'light' : 'dark';
    localStorage.setItem(this.STORAGE_KEY, this.currentTheme);
    this.applyTheme(this.currentTheme);
  }

  private applyTheme(theme: Theme): void {
    const htmlEl = this.document.documentElement; // <html>
    if (theme === 'dark') {
      htmlEl.classList.add('dark');
    } else {
      htmlEl.classList.remove('dark');
    }
  }
}
