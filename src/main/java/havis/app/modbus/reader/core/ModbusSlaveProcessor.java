package havis.app.modbus.reader.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.app.modbus.reader.api.Field;
import havis.app.modbus.reader.api.FieldValue;
import havis.app.modbus.reader.api.Module;
import havis.app.modbus.reader.api.ModuleException;
import havis.util.modbus.ModbusMapping;
import havis.util.modbus.UInt16Array;
import havis.util.modbus.UInt8Array;

class ModbusSlaveProcessor implements SlaveProcessor {

	private static final Logger log = Logger.getLogger(ModbusSlaveProcessor.class.getName());

	private Path dfltFieldsPropsFilePath;
	private Path fieldsPropsFilePath;
	private int openCloseTimeout;
	private Slave slave;
	private Module module;
	private Map<Field, List<FieldValue>> initialFieldValues;
	private Lock lock = new ReentrantLock();
	private ModbusMapping mapping;
	private Lock mappingLock = new ReentrantLock();
	private int connectionCounter = 0;

	ModbusSlaveProcessor(Path configBaseDirPath, Path stateBaseDirPath, int openCloseTimeout, Slave slave,
			Module module) throws ModbusSlaveException {
		dfltFieldsPropsFilePath = configBaseDirPath.resolve("dfltFields.properties");
		fieldsPropsFilePath = stateBaseDirPath.resolve("fields.properties").toAbsolutePath();
		this.openCloseTimeout = openCloseTimeout;
		this.slave = slave;
		this.module = module;
	}

	@Override
	public void connect() throws ModbusSlaveException {
		lock.lock();
		try {
			if (connectionCounter == 0) {
				try {
					module.open(openCloseTimeout);
				} catch (ModuleException e) {
					throw new ModbusSlaveException("Cannot open module", e);
				}
				try {
					if (initialFieldValues == null) {
						Path path = Files.isRegularFile(fieldsPropsFilePath) ? fieldsPropsFilePath
								: dfltFieldsPropsFilePath;
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, "Reading field values from " + path);
						}
						initialFieldValues = new FieldSerializer().read(path);
					}
					// set field values to module
					for (Entry<Field, List<FieldValue>> fieldValueEntry : initialFieldValues.entrySet()) {
						Field field = fieldValueEntry.getKey();
						List<FieldValue> values = fieldValueEntry.getValue();
						for (int i = 0; i < values.size(); i++) {
							module.setFieldValue(field, i /* fieldGroupIndex */, values.get(i));
						}
					}
				} catch (Exception e) {
					try {
						module.close(openCloseTimeout);
					} catch (ModuleException e1) {
						log.log(Level.SEVERE, "Cannot close module", e1);
					}
					throw new ModbusSlaveException("Cannot initialize configuration fields", e);
				}

				createMapping();
			}
			connectionCounter++;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void disconnect() throws ModbusSlaveException {
		lock.lock();
		try {
			switch (connectionCounter) {
			case 0:
				return;
			case 1:
				try {
					Map<Field, List<FieldValue>> fieldValues = new HashMap<>();
					// get all fields
					FieldProperties fieldProps = module.getFieldProperties(null /* startFieldInfo */,
							null /* address */, null /* field */);
					Field[] fields = fieldProps.getScannedFields();
					fieldProps = null;
					// for each field
					for (Field field : fields) {
						// get field properties
						fieldProps = module.getFieldProperties(fieldProps /* startFieldInfo */, null /* address */,
								field);
						// if field is a configuration field
						if (fieldProps.isConfigField()) {
							List<FieldValue> values = fieldValues.get(field);
							if (values == null) {
								values = new ArrayList<>();
								fieldValues.put(field, values);
							}
							// get field value from module and add it to list
							values.add(module.getFieldValue(field, fieldValues.size() /* fieldGroupIndex */));
						}
					}
					// if field values have been changed
					if (!initialFieldValues.equals(fieldValues)) {
						if (log.isLoggable(Level.INFO)) {
							log.log(Level.INFO, "Writing field values to " + fieldsPropsFilePath);
						}
						new FieldSerializer().write(fieldValues, fieldsPropsFilePath);
						initialFieldValues = fieldValues;
					}
				} catch (Exception e) {
					throw new ModbusSlaveException("Cannot store configuration fields", e);
				} finally {
					slave.destroyMapping(mapping);
					mapping = null;
					try {
						module.close(openCloseTimeout);
					} catch (ModuleException e) {
						throw new ModbusSlaveException("Cannot close module", e);
					}
				}
			}
			connectionCounter--;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public FieldProperties getFieldProperties(FieldProperties startFieldProps, Integer selectAddress,
			Field selectField) {
		lock.lock();
		try {
			return module.getFieldProperties(startFieldProps, selectAddress, selectField);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public ModbusMapping acquireMapping() {
		mappingLock.lock();
		lock.lock();
		try {
			return mapping;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void releaseMapping() {
		mappingLock.unlock();
	}

	@Override
	public void read(short functionCode, int address, int addressQuantity, ModbusMapping mapping)
			throws ModbusSlaveException {
		lock.lock();
		try {
			// get field properties for first address
			FieldProperties fieldPropsStart = module.getFieldProperties(null /* startFieldInfo */, address,
					null /* field */);
			// if address does not exist
			if (fieldPropsStart.getField() == null) {
				return;
			}
			try {
				Field[] scannedFields;
				// if multiple addresses
				if (addressQuantity > 1) {
					// get field properties of last address
					FieldProperties fieldPropsEnd = module.getFieldProperties(fieldPropsStart /* startFieldInfo */,
							address + addressQuantity - 1, null /* field */);
					scannedFields = fieldPropsEnd.getScannedFields();
				} else {
					scannedFields = new Field[] { fieldPropsStart.getField() };
				}
				// get values of scanned fields from module and write them to
				// the mapping
				setFieldValues(fieldPropsStart, scannedFields, mapping);
			} catch (ModuleException e) {
				throw new ModbusSlaveException("Cannot read values for address " + address, e);
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public ModbusMapping write(short functionCode, int address, int addressQuantity, Date timeStamp,
			ModbusMapping mapping) throws ModbusSlaveException {
		lock.lock();
		try {
			// get field properties for first address
			FieldProperties fieldPropsStart = module.getFieldProperties(null /* startFieldInfo */, address,
					null /* field */);
			// if address does not exist
			if (fieldPropsStart.getField() == null) {
				return mapping;
			}
			Field[] scannedFields;
			try {
				// if multiple addresses
				if (addressQuantity > 1) {
					// get field properties of last address
					FieldProperties fieldPropsEnd = module.getFieldProperties(fieldPropsStart /* startFieldInfo */,
							address + addressQuantity - 1, null /* field */);
					scannedFields = fieldPropsEnd.getScannedFields();
				} else {
					scannedFields = new Field[] { fieldPropsStart.getField() };
				}
				// get values of scanned fields from mapping and write them to
				// the module
				getFieldValues(fieldPropsStart, scannedFields, mapping);
			} catch (ModuleException e) {
				throw new ModbusSlaveException("Cannot write values of address " + address, e);
			}
			fieldPropsStart = null;
			// for each scanned field
			for (Field scannedField : scannedFields) {
				// get field properties
				fieldPropsStart = module.getFieldProperties(fieldPropsStart /* startFieldInfo */, null /* address */,
						scannedField);
				// if field is a config field
				if (fieldPropsStart.isConfigField()) {
					// create a new mapping
					createMapping();
					return this.mapping;
				}
			}
			return mapping;
		} finally {
			lock.unlock();
		}
	}

	private void createMapping() {
		// expand fields
		module.expandFields();
		// create mapping
		FieldProperties fieldProps = module.getFieldProperties(null /* startFieldInfo */, null /* address */,
				null /* field */);
		if (mapping != null) {
			slave.destroyMapping(mapping);
		}
		mapping = slave.createMapping(fieldProps.getAddress() /* coils */, fieldProps.getAddress() /* discreteInputs */,
				fieldProps.getAddress() /* holdingRegisters */, fieldProps.getAddress()/* inputRegisters */);
	}

	/**
	 * Sets field values from the module to a mapping.
	 * 
	 * @param fieldPropsStart
	 * @param fields
	 * @param mapping
	 * @throws ModbusSlaveException
	 * @throws ModuleException
	 */
	private void setFieldValues(FieldProperties fieldPropsStart, Field[] fields, ModbusMapping mapping)
			throws ModbusSlaveException, ModuleException {
		FieldProperties fieldProps = fieldPropsStart;
		// for each field
		for (Field field : fields) {
			// get field properties
			fieldProps = module.getFieldProperties(fieldProps, null /* address */, field);
			// get field value from module
			FieldValue value = module.getFieldValue(field, fieldProps.getFieldGroupIndex());
			if (value != null && value.getDataType() != fieldProps.getDataType()) {
				throw new ModbusSlaveException("Invalid data type for field " + fieldProps.getField() + " at address "
						+ fieldProps.getAddress() + ": " + value.getDataType() + " (expected "
						+ fieldProps.getDataType() + ")");
			}
			UInt8Array tabBits = null;
			UInt16Array tabRegisters = null;
			try {
				// set field value to mapping
				switch (fieldProps.getType()) {
				case COILS:
					tabBits = UInt8Array.frompointer(mapping.getTabBits());
					setFieldValue(tabBits, mapping.getNbBits(), fieldProps, value, "coil");
					break;
				case DISCRETE_INPUTS:
					tabBits = UInt8Array.frompointer(mapping.getTabInputBits());
					setFieldValue(tabBits, mapping.getNbInputBits(), fieldProps, value, "discrete input");
					break;
				case HOLDING_REGISTERS:
					tabRegisters = UInt16Array.frompointer(mapping.getTabRegisters());
					setFieldValue(tabRegisters, mapping.getNbRegisters(), fieldProps, value, "holding register");
					break;
				case INPUT_REGISTERS:
					tabRegisters = UInt16Array.frompointer(mapping.getTabInputRegisters());
					setFieldValue(tabRegisters, mapping.getNbInputRegisters(), fieldProps, value, "input register");
					break;
				}
			} finally {
				if (tabBits != null) {
					tabBits.delete();
				} else if (tabRegisters != null) {
					tabRegisters.delete();
				}
			}
		}
	}

	/**
	 * Gets field values from a mapping and sets them to the module.
	 * 
	 * @param fieldPropsStart
	 * @param fields
	 * @param mapping
	 * @throws ModbusSlaveException
	 * @throws ModuleException
	 */
	private void getFieldValues(FieldProperties fieldPropsStart, Field[] fields, ModbusMapping mapping)
			throws ModbusSlaveException, ModuleException {
		FieldProperties fieldProps = fieldPropsStart;
		// for each field
		for (Field field : fields) {
			// get field properties
			fieldProps = module.getFieldProperties(fieldProps, null /* address */, field);
			UInt8Array tabBits = null;
			UInt16Array tabRegisters = null;
			try {
				FieldValue value = null;
				// get field value from mapping
				switch (fieldProps.getType()) {
				case COILS:
					tabBits = UInt8Array.frompointer(mapping.getTabBits());
					value = getFieldValue(tabBits, mapping.getNbBits(), fieldProps, "coil");
					break;
				case DISCRETE_INPUTS:
					tabBits = UInt8Array.frompointer(mapping.getTabInputBits());
					value = getFieldValue(tabBits, mapping.getNbInputBits(), fieldProps, "discrete input");
					break;
				case HOLDING_REGISTERS:
					tabRegisters = UInt16Array.frompointer(mapping.getTabRegisters());
					value = getFieldValue(tabRegisters, mapping.getNbRegisters(), fieldProps, "holding register");
					break;
				case INPUT_REGISTERS:
					tabRegisters = UInt16Array.frompointer(mapping.getTabInputRegisters());
					value = getFieldValue(tabRegisters, mapping.getNbInputRegisters(), fieldProps, "input register");
					break;
				}
				// set field value to module
				module.setFieldValue(field, fieldProps.getFieldGroupIndex(), value);
			} finally {
				if (tabBits != null) {
					tabBits.delete();
				} else if (tabRegisters != null) {
					tabRegisters.delete();
				}
			}
		}
	}

	private void setFieldValue(UInt8Array destBits, int destBitsSize, FieldProperties fieldProps, FieldValue fieldValue,
			String description) throws ModbusSlaveException {
		if (fieldValue == null) {
			setBoolValues(destBits, destBitsSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					(boolean[]) null /* value */, description);
			return;
		}
		switch (fieldValue.getDataType()) {
		case BOOLEAN:
			setBoolValues(destBits, destBitsSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					fieldValue.getBooleanValue(), description);
			break;
		default:
			throw new ModbusSlaveException("Unknown data type for bits at address " + fieldProps.getAddress() + ": "
					+ fieldValue.getDataType() + " (supported: BOOLEAN)");
		}
	}

	private FieldValue getFieldValue(UInt8Array destBits, int destBitsSize, FieldProperties fieldProps,
			String description) throws ModbusSlaveException {
		switch (fieldProps.getDataType()) {
		case BOOLEAN:
			return getBoolValues(destBits, destBitsSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					description);
		default:
			throw new ModbusSlaveException("Unknown data type for bits at address " + fieldProps.getAddress() + ": "
					+ fieldProps.getDataType() + " (supported: BOOLEAN)");
		}
	}

	private void setFieldValue(UInt16Array destRegisters, int destRegistersSize, FieldProperties fieldProps,
			FieldValue fieldValue, String description) throws ModbusSlaveException {
		if (fieldValue == null) {
			setByteValues(destRegisters, destRegistersSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					(byte[]) null /* value */, description);
			return;
		}
		switch (fieldValue.getDataType()) {
		case BYTE:
			setByteValues(destRegisters, destRegistersSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					fieldValue.getByteValue(), description);
			break;
		case SHORT:
			setShortValues(destRegisters, destRegistersSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					fieldValue.getShortValue(), description);
			break;
		case USHORT:
			setUShortValues(destRegisters, destRegistersSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					fieldValue.getUShortValue(), description);
			break;
		case FLOAT:
			setFloatValues(destRegisters, destRegistersSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					fieldValue.getFloatValue(), description);
			break;
		case STRING:
			setStringValues(destRegisters, destRegistersSize, fieldProps.getAddress(), fieldProps.getAddressQuantity(),
					fieldValue.getStringValue(), StandardCharsets.UTF_8, description);
			break;
		default:
			throw new ModbusSlaveException("Unknown data type for registers at address " + fieldProps.getAddress()
					+ ": " + fieldValue.getDataType() + " (supported: BYTE, SHORT, USHORT, FLOAT, STRING)");
		}
	}

	private FieldValue getFieldValue(UInt16Array destRegisters, int destRegistersSize, FieldProperties fieldProps,
			String description) throws ModbusSlaveException {
		switch (fieldProps.getDataType()) {
		case BYTE:
			return getByteValues(destRegisters, destRegistersSize, fieldProps.getAddress(),
					fieldProps.getAddressQuantity(), description);
		case SHORT:
			return getShortValues(destRegisters, destRegistersSize, fieldProps.getAddress(),
					fieldProps.getAddressQuantity(), description);
		case USHORT:
			return getUShortValues(destRegisters, destRegistersSize, fieldProps.getAddress(),
					fieldProps.getAddressQuantity(), description);
		case FLOAT:
			return getFloatValues(destRegisters, destRegistersSize, fieldProps.getAddress(),
					fieldProps.getAddressQuantity(), description);
		case STRING:
			return getStringValues(destRegisters, destRegistersSize, fieldProps.getAddress(),
					fieldProps.getAddressQuantity(), StandardCharsets.UTF_8, description);
		default:
			throw new ModbusSlaveException("Unknown data type for registers at address " + fieldProps.getAddress()
					+ ": " + fieldProps.getDataType() + " (supported: BYTE, SHORT, USHORT, FLOAT, STRING)");
		}
	}

	private void setBoolValues(UInt8Array destBits, int destBitsSize, int address, int addressQuantity,
			boolean[] values, String description) throws ModbusSlaveException {
		if (address + addressQuantity > destBitsSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destBitsSize);
		}
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			short shortValue = (values != null && i < values.length && values[i]) ? (short) 1 : (short) 0;
			// set value to registers
			int key = address + i;
			destBits.setitem(key, shortValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": 0x" + String.format("%02X", shortValue));
			}
		}
	}

	private FieldValue getBoolValues(UInt8Array srcBits, int srcBitsSize, int address, int addressQuantity,
			String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcBitsSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcBitsSize);
		}
		boolean[] ret = new boolean[addressQuantity];
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = srcBits.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read" + description + " " + key + ": 0x" + String.format("%02X", shortValue));
			}
			ret[i] = (shortValue == 1);
		}
		return new FieldValue(ret);
	}

	private void setByteValues(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity,
			byte[] values, String description) throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		for (int i = 0; i < addressQuantity * 2; i += 2) {
			byte b1 = (values != null && i < values.length) ? values[i] : 0;
			byte b2 = (values != null && i + 1 < values.length) ? values[i + 1] : 0;
			int intValue = (b1 << 8 | b2 & 0x00FF) & 0xFFFF;
			// set value to registers
			int key = address + i / 2;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
			}
		}
	}

	private FieldValue getByteValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
			String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		byte[] ret = new byte[addressQuantity * 2];
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = (short) srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
			}
			ret[i * 2] = (byte) (shortValue >> 8);
			ret[i * 2 + 1] = (byte) shortValue;
		}
		return new FieldValue(ret);
	}

	private void setShortValues(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity,
			short[] values, String description) throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			short v = (values != null && i < values.length) ? values[i] : 0;
			int intValue = v & 0xFFFF;
			// set value to registers
			int key = address + i;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
			}
		}
	}

	private FieldValue getShortValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
			String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		short[] ret = new short[addressQuantity];
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = (short) srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
			}
			ret[i] = shortValue;
		}
		return new FieldValue(ret);
	}

	private void setUShortValues(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity,
			int[] values, String description) throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			int intValue = (values != null && i < values.length) ? values[i] : 0;
			// set value to registers
			int key = address + i;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": " + intValue);
			}
		}
	}

	private FieldValue getUShortValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
			String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		int[] ret = new int[addressQuantity];
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			int intValue = srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": " + intValue);
			}
			ret[i] = intValue;
		}
		return new FieldValue(ret);
	}

	private void setFloatValues(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity,
			float[] values, String description) throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		UInt16Array floatRegisters = new UInt16Array(2);
		try {
			// for each value
			for (int i = 0; i < addressQuantity / 2; i++) {
				float floatValue = (values != null && i < values.length) ? values[i] : 0;
				// set float value to register array
				slave.setFloat(floatValue, floatRegisters);
				// for each register value
				for (int j = 0; j < 2; j++) {
					int intValue = floatRegisters.getitem(j);
					// set value to registers
					int key = address + i * 2 + j;
					destRegisters.setitem(key, intValue);
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
								"Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
					}
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, " -> " + floatValue);
				}
			}
		} finally {
			floatRegisters.delete();
		}
	}

	private FieldValue getFloatValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
			String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		float[] ret = new float[addressQuantity];
		UInt16Array floatRegisters = new UInt16Array(2);
		try {
			// for each float value
			for (int i = 0; i < addressQuantity / 2; i++) {
				// for each integer value
				for (int j = 0; j < 2; j++) {
					int key = address + i * 2 + j;
					short shortValue = (short) srcRegisters.getitem(key);
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE,
								"Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
					}
					floatRegisters.setitem(j, shortValue);
				}
				// set float value to register array
				float floatValue = slave.getFloat(floatRegisters);
				ret[i] = floatValue;
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "  -> " + floatValue);
				}
			}
		} finally {
			floatRegisters.delete();
		}
		return new FieldValue(ret);
	}

	private void setStringValues(UInt16Array destRegisters, int destRegistersSize, int address, int addressQuantity,
			String[] values, Charset encoding, String description) throws ModbusSlaveException {
		if (address + addressQuantity > destRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + destRegistersSize);
		}
		// convert string array to byte array
		byte[] bytes = null;
		String stringValue = null;
		if (values != null) {
			StringBuilder strB = new StringBuilder();
			for (String value : values) {
				if (value != null) {
					strB.append(value);
				}
			}
			stringValue = strB.toString();
			bytes = stringValue.getBytes(encoding);
		}
		// for each value
		for (int i = 0; i < addressQuantity * 2; i += 2) {
			byte b1 = (bytes != null && i < bytes.length) ? bytes[i] : 0;
			byte b2 = (bytes != null && i + 1 < bytes.length) ? bytes[i + 1] : 0;
			int intValue = (b1 << 8 | b2 & 0x00FF) & 0xFFFF;
			// set value to registers
			int key = address + i / 2;
			destRegisters.setitem(key, intValue);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Wrote " + description + " " + key + ": 0x" + String.format("%04X", intValue));
			}
		}
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  -> " + stringValue);
		}
	}

	private FieldValue getStringValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
			Charset encoding, String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}

		List<Byte> byteList = new ArrayList<>();
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			short shortValue = (short) srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": 0x" + String.format("%04X", shortValue));
			}
			byteList.add((byte) (shortValue >> 8));
			byteList.add((byte) shortValue);
		}
		byte[] bytes = new byte[byteList.size()];
		for (int i = 0; i < byteList.size(); i++) {
			bytes[i] = byteList.get(i);
		}
		String stringValue = new String(bytes, StandardCharsets.UTF_8);
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  -> " + stringValue);
		}
		return new FieldValue(new String[] { stringValue });
	}

}
