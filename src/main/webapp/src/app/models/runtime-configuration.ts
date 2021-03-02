export interface RuntimeConfigurationIntf {
  tagsInField?: number;
  memorySelector?: number;
  epcLength?: number;
  tidLength?: number;
  userLength?: number;
  selectionMaskCount?: number;
  selectionMaskMaxLength?: number;
  customOperationMaxLength?: number;
  includeAccessPwd?: boolean;
  includeCRC?: boolean;
  includeKillPwd?: boolean;
  includePC?: boolean;
  includeXPC?: boolean;
}

export class RuntimeConfiguration implements RuntimeConfigurationIntf {
  private static readonly KILL_PWD = 0b1;
  private static readonly ACCESS_PWD = 0b10;
  private static readonly CRC = 0b100;
  private static readonly PC = 0b1000;
  private static readonly XPC = 0b10000;

  tagsInField?: number;
  memorySelector?: number;
  epcLength?: number;
  tidLength?: number;
  userLength?: number;
  selectionMaskCount?: number;
  selectionMaskMaxLength?: number;
  customOperationMaxLength?: number;

  constructor(
    source: RuntimeConfigurationIntf,
  ) {
    Object.assign(this, source);
  }
  private setMemValue(include: boolean, value: number) {
    if (include) {
      this.memorySelector |= value;
    } else {
      this.memorySelector &= ~value;
    }
  }

  private getMemValue(value: number) {
    return (this.memorySelector & value) > 0;
  }

  get includeXPC() {
    return this.getMemValue(RuntimeConfiguration.XPC);
  }

  set includeXPC(includeXPC: boolean) {
    this.setMemValue(includeXPC, RuntimeConfiguration.XPC);
  }

  get includePC() {
    return this.getMemValue(RuntimeConfiguration.PC);
  }

  set includePC(includePC: boolean) {
    this.setMemValue(includePC, RuntimeConfiguration.PC);
  }

  get includeCRC() {
    return this.getMemValue(RuntimeConfiguration.CRC);
  }

  set includeCRC(includeCRC: boolean) {
    this.setMemValue(includeCRC, RuntimeConfiguration.CRC);
  }

  get includeAccessPwd() {
    return this.getMemValue(RuntimeConfiguration.ACCESS_PWD);
  }

  set includeAccessPwd(includeAccessPwd: boolean) {
    this.setMemValue(includeAccessPwd, RuntimeConfiguration.ACCESS_PWD);
  }

  get includeKillPwd() {
    return this.getMemValue(RuntimeConfiguration.KILL_PWD);
  }

  set includeKillPwd(includeKillPwd: boolean) {
    this.setMemValue(includeKillPwd, RuntimeConfiguration.KILL_PWD);
  }
}
