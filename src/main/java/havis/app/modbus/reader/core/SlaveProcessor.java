package havis.app.modbus.reader.core;

import java.util.Date;

import havis.app.modbus.reader.api.Field;
import havis.util.modbus.ModbusMapping;

/**
 * A slave processor is provided by {@link ModbusSlave#open()} and is used
 * internally by the slave for incoming connections and externally by the UI.
 * The slave processor is thread safe.
 */
public interface SlaveProcessor {
	/**
	 * Connects the processor to the backend. The connection must be closed with
	 * {@link #disconnect()}.
	 * 
	 * @throws ModbusSlaveException
	 */
	void connect() throws ModbusSlaveException;

	/**
	 * Closes the connection from the processor to the backend.
	 * 
	 * @throws ModbusSlaveException
	 */
	void disconnect() throws ModbusSlaveException;

	/**
	 * Determines field properties for a modbus address or a field identifier of
	 * the backend module (eg. {@link RfField#TAG_COUNT#getValue()}). A
	 * connection must have been created before with {@link #connect()}.
	 * 
	 * @param startFieldProps
	 * @param selectAddress
	 * @param selectField
	 * @return
	 */
	FieldProperties getFieldProperties(FieldProperties startFieldProps, Integer selectAddress, Field selectField);

	/**
	 * Acquires the mapping for reading or writing of values with
	 * {@link #read(short, int, int, ModbusMapping)} /
	 * {@link #write(short, int, int, Date, ModbusMapping)}. A connection must
	 * have been created before with {@link #connect()}. The mapping must be
	 * released with {@link #releaseMapping()}.
	 */
	ModbusMapping acquireMapping();

	/**
	 * Releases the mapping returned by {@link #acquireMapping()}.
	 */
	void releaseMapping();

	/**
	 * Reads values from the backend and sets them to the mapping.
	 * <p>
	 * The mapping must be acquired with {@link #acquireMapping()} and released
	 * with {@link #releaseMapping()}.
	 * </p>
	 * 
	 * @param functionCode
	 * @param address
	 * @param addressQuantity
	 * @param mapping
	 * @throws ModbusSlaveException
	 */
	void read(short functionCode, int address, int addressQuantity, ModbusMapping mapping) throws ModbusSlaveException;

	/**
	 * Writes values from the mapping to the backend.
	 * <p>
	 * The mapping must be acquired with {@link #acquireMapping()} and released
	 * with {@link #releaseMapping()}.
	 * </p>
	 * <p>
	 * The returned mapping must be used for further calls. It may be empty.
	 * </p>
	 * 
	 * @param functionCode
	 * @param address
	 * @param addressQuantity
	 * @param timeStamp
	 * @param mapping
	 * @return
	 * @throws ModbusSlaveException
	 */
	ModbusMapping write(short functionCode, int address, int addressQuantity, Date timeStamp, ModbusMapping mapping)
			throws ModbusSlaveException;
}
