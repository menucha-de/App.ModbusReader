package havis.app.modbus.reader.core;

import havis.util.modbus.ModbusMapping;
import havis.util.modbus.UInt16Array;

interface Slave {

	ModbusMapping createMapping(int coils, int discreteInputs, int holdingRegisters, int inputRegisters);

	void destroyMapping(ModbusMapping mapping);

	void setFloat(float value, UInt16Array destRegisters);

	float getFloat(UInt16Array destRegisters);
}
