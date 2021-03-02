import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RuntimeRegisterComponent } from './runtime-register.component';

describe('RuntimeRegisterComponent', () => {
  let component: RuntimeRegisterComponent;
  let fixture: ComponentFixture<RuntimeRegisterComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RuntimeRegisterComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RuntimeRegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
