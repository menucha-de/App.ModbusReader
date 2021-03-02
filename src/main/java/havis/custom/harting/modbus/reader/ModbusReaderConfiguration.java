package havis.custom.harting.modbus.reader;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.custom.harting.modbus.reader.core.FieldProperties;
import havis.custom.harting.modbus.reader.core.FieldProperties.FieldType;
import havis.custom.harting.modbus.reader.core.ModbusSlaveException;
import havis.custom.harting.modbus.reader.core.SlaveProcessor;
import havis.custom.harting.modbus.reader.rest.data.DeviceInfo;
import havis.custom.harting.modbus.reader.rest.data.RuntimeConfiguration;
import havis.custom.harting.modbus.reader.rest.data.RuntimeRegisterItem;
import havis.custom.harting.modbus.reader.rest.data.Type;
import havis.custom.harting.modbus.reader.rf.RfConstants.RfFieldType;
import havis.util.modbus.ModbusBase;
import havis.util.modbus.ModbusMapping;
import havis.util.modbus.UInt16Array;

public class ModbusReaderConfiguration {

	private static final Logger log = Logger.getLogger(ModbusReaderConfiguration.class.getName());
	private static final String DESCRIPTIONS_PATH = "havis-modbus-reader/descriptions.properties";

	private static final RfFieldType[] DEVICE_INFO_FIELDS = new RfFieldType[] { RfFieldType.VENDOR_NAME, RfFieldType.PRODUCT_CODE,
			RfFieldType.MAJOR_MINOR_REVISION, RfFieldType.SERIAL_NUMBER, RfFieldType.HARDWARE_REVISION, RfFieldType.BASE_FIRMWARE };

	private static final RfFieldType[] RUNTIME_CONFIG_FIELDS = new RfFieldType[] { RfFieldType.TAGS_IN_FIELD,
			RfFieldType.MEMORY_SELECTOR, RfFieldType.EPC_LENGTH, RfFieldType.TID_LENGTH, RfFieldType.USER_LENGTH,
			RfFieldType.SELECTION_MASK_COUNT, RfFieldType.SELECTION_MASK_MAX_LENGTH, RfFieldType.CUSTOM_COMMAND_MAX_LENGTH };

	private static final RfFieldType[] RUNTIME_FIELDS = new RfFieldType[] { RfFieldType.TAG_COUNT, RfFieldType.LAST_ERROR,
			RfFieldType.ACCESS_PASSWORD, RfFieldType.ANTENNA_MASK };

	private static final RfFieldType[] SELECTION_MASK_FIELDS = new RfFieldType[] { RfFieldType.SELECTION_MASK_BANK,
			RfFieldType.SELECTION_MASK_LENGTH, RfFieldType.SELECTION_MASK_OFFSET, RfFieldType.SELECTION_MASK };

	private Properties descriptions;
	private SlaveProcessor slaveProcessor;
	private ModbusMapping mapping;

	/**
	 * Loads field descriptions from classpath
	 * 
	 * @param slaveProcessor
	 *            {@link SlaveProcessor} Interface
	 */
	public ModbusReaderConfiguration(SlaveProcessor slaveProcessor) {
		this.slaveProcessor = slaveProcessor;
		descriptions = new Properties();
		try {
			descriptions.load(getClass().getClassLoader().getResourceAsStream(DESCRIPTIONS_PATH));
		} catch (IOException e) {
			log.log(Level.FINE, "Field descriptions not found", e);
		}
	}

	/**
	 * Opens TCP connection, initilizes {@link SlaveProcessor} and acquires the
	 * field mapping. Must be called before reading/writing.
	 * 
	 * @throws ModbusReaderException
	 */
	private void connect() throws ModbusReaderException {
		try {
			slaveProcessor.connect();
			mapping = slaveProcessor.acquireMapping();
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Closes TCP connection and releases the field mapping. Must be called
	 * after reading/writing.
	 * 
	 * @throws ModbusReaderException
	 */
	private void disconnect() throws ModbusReaderException {
		try {
			slaveProcessor.releaseMapping();
			slaveProcessor.disconnect();
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Read a String value from address specified by fp.
	 * 
	 * @param fp
	 *            {@link FieldProperties} to use for reading
	 * @return Field value
	 * @throws ModbusReaderException
	 */
	private String getString(FieldProperties fp) throws ModbusReaderException {
		try {
			UInt16Array registers = null;
			try {
				slaveProcessor.read((short) ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS, fp.getAddress(),
						fp.getAddressQuantity(), mapping);
				registers = UInt16Array.frompointer(mapping.getTabInputRegisters());
				String[] value = getStringValues(registers, mapping.getNbInputRegisters(), fp.getAddress(),
						fp.getAddressQuantity(), StandardCharsets.UTF_8, "");
				return value[0];
			} finally {
				if (registers != null) {
					registers.delete();
				}
			}
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Read String value from Modbus register
	 * 
	 * @param srcRegisters
	 * @param srcRegistersSize
	 * @param address
	 * @param addressQuantity
	 * @param encoding
	 * @param description
	 * @return
	 * @throws ModbusSlaveException
	 */
	private String[] getStringValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
			Charset encoding, String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}

		int length = addressQuantity * 2;
		byte[] bytes = new byte[length];
		for (int index = 0; index < addressQuantity; ++index) {
			int pos = index * 2;
			int key = address + index;
			short shortValue = (short) srcRegisters.getitem(key);
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": " + String.format("0x%04X", shortValue));
			}

			if (!addIfValid(bytes, pos, (byte) (shortValue >> 8))) {
				length = pos;
				break;
			}
			if (!addIfValid(bytes, pos + 1, (byte) shortValue)) {
				length = pos + 1;
				break;
			}
		}
		String stringValue = new String(bytes, 0, length, StandardCharsets.UTF_8);
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  -> " + stringValue);
		}
		return new String[] { stringValue };
	}

	private boolean addIfValid(byte[] bytes, int index, byte b) {
		if (b != 0x00) {
			bytes[index] = b;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Write unsigned short value to address specified by
	 * {@link FieldProperties} fp.
	 * 
	 * @param fp
	 * @param value
	 * @throws ModbusReaderException
	 */
	private void setUShort(FieldProperties fp, int value) throws ModbusReaderException {
		try {
			UInt16Array registers = null;
			try {
				registers = UInt16Array.frompointer(mapping.getTabRegisters());
				setUShortValues(registers, mapping.getNbRegisters(), fp.getAddress(), fp.getAddressQuantity(),
						new int[] { value }, "");
				mapping = slaveProcessor.write((short) ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS, fp.getAddress(),
						fp.getAddressQuantity(), new Date(), mapping);
			} finally {
				if (registers != null) {
					registers.delete();
				}
			}
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Write unsigned short value to Modbus register
	 * 
	 * @param destRegisters
	 * @param destRegistersSize
	 * @param address
	 * @param addressQuantity
	 * @param values
	 * @param description
	 * @throws ModbusSlaveException
	 */
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

	private byte[] shorts2bytes(short[] value) {
		byte[] ret = new byte[value.length * 2];
		for (int i = 0; i < value.length; i++) {
			ret[i * 2] = (byte) (value[i] >> 8);
			ret[i * 2 + 1] = (byte) value[i];
		}
		return ret;
	}

	/**
	 * Read unsigned short value from address specified by
	 * {@link FieldProperties} fp.
	 * 
	 * @param fp
	 *            {@link FieldProperties}
	 * @return unsigned short value
	 * @throws ModbusReaderException
	 */
	private BigInteger getBigInteger(FieldProperties fp) throws ModbusReaderException {
		try {
			UInt16Array registers = null;
			try {
				slaveProcessor.read((short) ModbusBase.MODBUS_FC_READ_INPUT_REGISTERS, fp.getAddress(),
						fp.getAddressQuantity(), mapping);
				registers = UInt16Array.frompointer(mapping.getTabInputRegisters());
				short[] value = getShortValues(registers, mapping.getNbInputRegisters(), fp.getAddress(),
						fp.getAddressQuantity(), "");
				return new BigInteger(shorts2bytes(value));
			} finally {
				if (registers != null) {
					registers.delete();
				}
			}
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Read unsigned short value from address specified by
	 * {@link FieldProperties} fp.
	 * 
	 * @param fp
	 *            {@link FieldProperties}
	 * @return unsigned short value
	 * @throws ModbusReaderException
	 */
	private int getUShort(FieldProperties fp) throws ModbusReaderException {
		try {
			UInt16Array registers = null;
			try {
				slaveProcessor.read((short) ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS, fp.getAddress(),
						fp.getAddressQuantity(), mapping);
				registers = UInt16Array.frompointer(mapping.getTabRegisters());
				int[] value = getUShortValues(registers, mapping.getNbRegisters(), fp.getAddress(),
						fp.getAddressQuantity(), "");
				return value[0];
			} finally {
				if (registers != null) {
					registers.delete();
				}
			}
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Read unsigned short value from Modbus register
	 * 
	 * @param srcRegisters
	 * @param srcRegistersSize
	 * @param address
	 * @param addressQuantity
	 * @param description
	 * @return unsigned short value
	 * @throws ModbusSlaveException
	 */
	private int[] getUShortValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
			String description) throws ModbusSlaveException {
		if (address + addressQuantity > srcRegistersSize) {
			throw new ModbusSlaveException(description + ": Invalid address: " + srcRegistersSize);
		}
		int[] ret = new int[addressQuantity];
		// for each value
		for (int i = 0; i < addressQuantity; i++) {
			// get value from registers
			int key = address + i;
			int intValue = srcRegisters.getitem(key) & 0xFFFF;
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Read " + description + " " + key + ": " + intValue);
			}
			ret[i] = intValue;
		}
		return ret;
	}

	/**
	 * Read short value from address specified by {@link FieldProperties} fp.
	 * 
	 * @param fp
	 *            {@link FieldProperties}
	 * @param value
	 * @throws ModbusReaderException
	 */
	private void setShort(FieldProperties fp, short value) throws ModbusReaderException {
		try {
			UInt16Array registers = null;
			try {
				registers = UInt16Array.frompointer(mapping.getTabRegisters());
				setShortValues(registers, mapping.getNbRegisters(), fp.getAddress(), fp.getAddressQuantity(),
						new short[] { value }, "");
				mapping = slaveProcessor.write((short) ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS, fp.getAddress(),
						fp.getAddressQuantity(), new Date(), mapping);
			} finally {
				if (registers != null) {
					registers.delete();
				}
			}
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Write short value to Modbus register
	 * 
	 * @param destRegisters
	 * @param destRegistersSize
	 * @param address
	 * @param addressQuantity
	 * @param values
	 * @param description
	 * @throws ModbusSlaveException
	 */
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

	/**
	 * Read short value from address specified by {@link FieldProperties} fp.
	 * 
	 * @param fp
	 *            FieldProperties
	 * @return short value
	 * @throws ModbusReaderException
	 */
	private short getShort(FieldProperties fp) throws ModbusReaderException {
		try {
			UInt16Array registers = null;
			try {
				slaveProcessor.read((short) ModbusBase.MODBUS_FC_READ_HOLDING_REGISTERS, fp.getAddress(),
						fp.getAddressQuantity(), mapping);
				registers = UInt16Array.frompointer(mapping.getTabRegisters());
				short[] value = getShortValues(registers, mapping.getNbRegisters(), fp.getAddress(),
						fp.getAddressQuantity(), "");
				return value[0];
			} finally {
				if (registers != null) {
					registers.delete();
				}
			}
		} catch (ModbusSlaveException e) {
			throw new ModbusReaderException(e);
		}
	}

	/**
	 * Read short value from Modbus register
	 * 
	 * @param srcRegisters
	 * @param srcRegistersSize
	 * @param address
	 * @param addressQuantity
	 * @param description
	 * @return short value
	 * @throws ModbusSlaveException
	 */
	private short[] getShortValues(UInt16Array srcRegisters, int srcRegistersSize, int address, int addressQuantity,
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
		return ret;
	}

	/**
	 * Converts FieldType from {@link SlaveProcessor} API to REST-friendly
	 * {@link Type}
	 * 
	 * @param fieldType
	 * @return Type
	 */
	private Type fromFieldType(FieldType fieldType) {
		Type result = null;
		if (fieldType == FieldType.INPUT_REGISTERS)
			result = Type.INPUT;
		if (fieldType == FieldType.HOLDING_REGISTERS) {
			result = Type.HOLDING;
		}
		return result;
	}

	/**
	 * Creates a {@link RuntimeRegisterItem} for RfField field and adds it to
	 * list.
	 * 
	 * @param start
	 *            {@link FieldProperties} to start fro,
	 * @param field
	 *            {@link RfFieldType} to get the properties for
	 * @param list
	 *            {@link List}&lt;{@link RuntimeRegisterItem}&gt; to add the
	 *            item
	 * @return {@link FieldProperties} Can be used as the start field for the
	 *         next search.
	 */
	private FieldProperties readFieldProp(FieldProperties start, RfFieldType field, List<RuntimeRegisterItem> list) {
		FieldProperties result = slaveProcessor.getFieldProperties(start, null, field.getField());
		list.add(runtimeItem(result));
		return result;
	}

	/**
	 * Create {@link RuntimeRegisterItem} from {@link FieldProperties}
	 * 
	 * @param fp
	 *            {@link FieldProperties}
	 * @return {@link RuntimeRegisterItem}
	 */
	private RuntimeRegisterItem runtimeItem(FieldProperties fp) {
		return new RuntimeRegisterItem(String.format("0x%04X", fp.getAddress()), String.valueOf(fp.getAddress()), fp.getAddressQuantity(),
				fromFieldType(fp.getType()), descriptions.getProperty(fp.getField().toString()));
	}

	/**
	 * Get a list of {@link FieldProperties} from an {@link RfFieldType} array
	 * 
	 * @param fields
	 * @return List of {@link FieldProperties}
	 */
	private List<FieldProperties> getFieldProperties(RfFieldType[] fields) {
		List<FieldProperties> result = new ArrayList<>(fields.length);
		FieldProperties fp = null;
		for (RfFieldType field : fields) {
			fp = slaveProcessor.getFieldProperties(fp, null, field.getField());
			result.add(fp);
		}
		return result;
	}

	/**
	 * Get {@link DeviceInfo}
	 * 
	 * @return {@link DeviceInfo}
	 * @throws ModbusReaderException
	 */
	public DeviceInfo getDeviceInfo() throws ModbusReaderException {
		log.log(Level.FINE, "Reading device info");
		connect();
		try {
			List<FieldProperties> deviceInfoFields = getFieldProperties(DEVICE_INFO_FIELDS);
			DeviceInfo result = new DeviceInfo();
			result.setVendorName(getString(deviceInfoFields.get(0)));
			result.setProductCode(getString(deviceInfoFields.get(1)));
			result.setMajorMinorRevision(getString(deviceInfoFields.get(2)));
			result.setSerialNumber(getBigInteger(deviceInfoFields.get(3)).toString());
			result.setHardwareRevision(getString(deviceInfoFields.get(4)));
			result.setBaseFirmware(getString(deviceInfoFields.get(5)));
			return result;
		} finally {
			disconnect();
		}
	}

	/**
	 * Get {@link RuntimeConfiguration}
	 * 
	 * @return {@link RuntimeConfiguration}
	 * @throws ModbusReaderException
	 */
	public RuntimeConfiguration getRuntimeConfiguration() throws ModbusReaderException {
		log.log(Level.FINE, "Reading runtime configuration");
		connect();
		try {
			List<FieldProperties> runtimeConfigFields = getFieldProperties(RUNTIME_CONFIG_FIELDS);
			RuntimeConfiguration rc = new RuntimeConfiguration();
			rc.setTagsInField(getUShort(runtimeConfigFields.get(0)));
			rc.setMemorySelector(getShort(runtimeConfigFields.get(1)));
			rc.setEpcLength(getUShort(runtimeConfigFields.get(2)));
			rc.setTidLength(getUShort(runtimeConfigFields.get(3)));
			rc.setUserLength(getUShort(runtimeConfigFields.get(4)));
			rc.setSelectionMaskCount(getUShort(runtimeConfigFields.get(5)));
			rc.setSelectionMaskMaxLength(getUShort(runtimeConfigFields.get(6)));
			rc.setCustomOperationMaxLength(getUShort(runtimeConfigFields.get(7)));
			return rc;
		} finally {
			disconnect();
		}
	}

	/**
	 * Set {@link RuntimeConfiguration}
	 * 
	 * @param rc
	 * @throws ModbusReaderException
	 */
	public void setRuntimeConfiguration(RuntimeConfiguration rc) throws ModbusReaderException {
		log.log(Level.FINE, "Writing runtime configuration");
		connect();
		try {
			List<FieldProperties> runtimeConfigFields = getFieldProperties(RUNTIME_CONFIG_FIELDS);
			setUShort(runtimeConfigFields.get(0), rc.getTagsInField());
			setShort(runtimeConfigFields.get(1), rc.getMemorySelector());
			setUShort(runtimeConfigFields.get(2), rc.getEpcLength());
			setUShort(runtimeConfigFields.get(3), rc.getTidLength());
			setUShort(runtimeConfigFields.get(4), rc.getUserLength());
			setUShort(runtimeConfigFields.get(5), rc.getSelectionMaskCount());
			setUShort(runtimeConfigFields.get(6), rc.getSelectionMaskMaxLength());
			setUShort(runtimeConfigFields.get(7), rc.getCustomOperationMaxLength());
		} finally {
			disconnect();
		}
	}

	/**
	 * Get Runtime Register
	 * 
	 * @return List of {@link RuntimeRegisterItem}
	 * @throws ModbusReaderException
	 */
	public List<RuntimeRegisterItem> getRuntime() throws ModbusReaderException {
		log.log(Level.FINE, "Reading runtime register");
		connect();
		try {
			RuntimeConfiguration rc = getRuntimeConfiguration();
			List<RuntimeRegisterItem> result = new ArrayList<>();

			FieldProperties fp = null;

			// Runtime Register
			for (RfFieldType field : RUNTIME_FIELDS) {
				fp = readFieldProp(fp, field, result);
			}

			// Selection Mask Register
			for (int i = 0; i < rc.getSelectionMaskCount(); ++i) {
				for (RfFieldType field : SELECTION_MASK_FIELDS) {
					fp = readFieldProp(fp, field, result);
				}
			}

			// Runtime Tag Register
			Map<RfFieldType, Boolean> fields = new LinkedHashMap<>();
			fields.put(RfFieldType.LOCK_OPERATION, true);
			fields.put(RfFieldType.KILL_OPERATION, true);
			fields.put(RfFieldType.KILL_PWD, rc.isIncludeKillPwd());
			fields.put(RfFieldType.ACCESS_PWD, rc.isIncludeAccessPwd());
			fields.put(RfFieldType.CRC, rc.isIncludeCRC());
			fields.put(RfFieldType.PC, rc.isIncludePC());
			fields.put(RfFieldType.EPC, rc.getEpcLength() > 0);
			fields.put(RfFieldType.XPC, rc.isIncludeXPC());
			fields.put(RfFieldType.TID_BANK, rc.getTidLength() > 0);
			fields.put(RfFieldType.USER_BANK, rc.getUserLength() > 0);
			fields.put(RfFieldType.CUSTOM_COMMAND_LENGTH, rc.getCustomOperationMaxLength() > 0);
			fields.put(RfFieldType.CUSTOM_COMMAND_DATA, rc.getCustomOperationMaxLength() > 0);

			for (int i = 0; i < rc.getTagsInField(); ++i) {
				for (Map.Entry<RfFieldType, Boolean> entry : fields.entrySet()) {
					if (entry.getValue()) {
						fp = readFieldProp(fp, entry.getKey(), result);
					}
				}
			}

			return result;
		} finally {
			disconnect();
		}
	}

	public String getRuntimeExport() throws ModbusReaderException {
		List<RuntimeRegisterItem> rt = getRuntime();
		StringBuilder result = new StringBuilder();
		result.append("Register address (HEX)\tRegister address (DEC)\tLength (WORD)\tRegister type\tDescription\n");
		for (RuntimeRegisterItem r : rt) {
			result.append(r.getAddressHex() + "\t");
			result.append(r.getAddressDez() + "\t");
			result.append(r.getLength() + "\t");
			result.append(r.getType() + "\t");
			result.append(r.getDescription() + "\n");
		}
		return result.toString();
	}
}
