import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter, Router} from '@angular/router';
import {signal} from '@angular/core';
import {FederationDirectory} from '../../core/federation/federation-directory';
import {FederationTarget} from '../../core/federation/federation-target';
import {Instances} from './instances';

describe('Instances', () => {
  const directory = {
    instances: signal([
      {id: 'local', instanceName: 'Local', available: true, deployments: [], error: ''},
      {id: 'office', instanceName: 'Office', available: true, deployments: [{name: 'api', state: 'RUNNING', message: 'ready'}], error: ''},
      {id: 'garage', instanceName: 'garage', available: false, deployments: [], error: 'Unable to reach this instance.'}
    ]),
    loading: signal(false),
    error: signal(false),
    refresh: vi.fn()
  };

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideRouter([]), {provide: FederationDirectory, useValue: directory}, FederationTarget]});
    directory.refresh.mockClear();
  });

  it('shows instance status and deployment information', () => {
    const fixture: ComponentFixture<Instances> = TestBed.createComponent(Instances);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Office');
    expect(fixture.nativeElement.textContent).toContain('api');
    expect(fixture.nativeElement.textContent).toContain('RUNNING');
    expect(fixture.nativeElement.textContent).toContain('Unable to reach this instance.');
  });

  it('opens the dashboard for the chosen instance', () => {
    const fixture: ComponentFixture<Instances> = TestBed.createComponent(Instances);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    const button = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('li'))[1]
      .querySelector('button') as HTMLButtonElement;
    button.click();
    expect(navigate).toHaveBeenCalledWith(['/'], {queryParams: {instance: 'office'}});
  });
});