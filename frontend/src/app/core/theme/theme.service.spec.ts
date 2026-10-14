import {TestBed} from '@angular/core/testing';
import {ThemeService} from './theme.service';

describe('ThemeService', () => {
  function mockSystemPreference(dark: boolean) {
    vi.stubGlobal('matchMedia', (query: string) => ({matches: dark && query.includes('dark')}));
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
  });

  afterEach(() => vi.unstubAllGlobals());

  it('follows the system preference initially', () => {
    mockSystemPreference(true);
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('prefers a stored choice over the system preference', () => {
    mockSystemPreference(true);
    localStorage.setItem('theme', 'light');
    expect(TestBed.inject(ThemeService).theme()).toBe('light');
  });

  it('toggles, applies and remembers the theme', () => {
    mockSystemPreference(false);
    const service = TestBed.inject(ThemeService);
    service.toggle();
    expect(service.theme()).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem('theme')).toBe('dark');
    service.toggle();
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});
