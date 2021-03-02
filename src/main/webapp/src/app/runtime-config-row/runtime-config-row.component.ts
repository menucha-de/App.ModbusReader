import { Component, OnInit, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-runtime-config-row',
  templateUrl: './runtime-config-row.component.html',
  styleUrls: ['./runtime-config-row.component.scss']
})
export class RuntimeConfigRowComponent implements OnInit {

  @Input() length: number;
  @Input() label: string;
  @Output() lengthChange = new EventEmitter<number>();
  enabled: boolean;
  backupLength: number;

  values = Array(256 + 5).fill(1).map((x, i) => (i < 255 ? (i + 1) : 2 ** (i - 255 + 8)));
  constructor() { }

  ngOnInit() {
    this.enabled = this.length > 0;
    this.backupLength = this.length > 0 ? this.length : 1;
  }

  changeValue(value: number) {
    this.lengthChange.emit(value);
  }
}
