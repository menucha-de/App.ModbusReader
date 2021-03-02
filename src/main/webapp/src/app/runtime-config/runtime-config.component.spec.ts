import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RuntimeConfigComponent } from './runtime-config.component';

describe('RuntimeConfigComponent', () => {
  let component: RuntimeConfigComponent;
  let fixture: ComponentFixture<RuntimeConfigComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RuntimeConfigComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RuntimeConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
