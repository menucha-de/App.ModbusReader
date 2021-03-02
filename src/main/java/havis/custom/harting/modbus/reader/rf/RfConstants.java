package havis.custom.harting.modbus.reader.rf;

import java.util.HashMap;
import java.util.Map;

import havis.custom.harting.modbus.reader.api.Field;
import havis.custom.harting.modbus.reader.core.FieldProperties.FieldDataType;
import havis.custom.harting.modbus.reader.core.FieldProperties.FieldType;
import havis.custom.harting.modbus.reader.core.SlaveProcessor;

public class RfConstants {

	/**
	 * The field identifiers can be used for interface {@link SlaveProcessor}.
	 */
	public enum RfFieldType {
		// device info
		VENDOR_NAME(0), //
		PRODUCT_CODE(1), //
		MAJOR_MINOR_REVISION(2), //
		SERIAL_NUMBER(3), //
		HARDWARE_REVISION(4), //
		BASE_FIRMWARE(5), //
		// device config
		COMMUNICATION_STANDARD(10), //
		NUMBER_OF_ANTENNAS(11), //
		ANTENNA_ONE_CONNECTED(12), //
		ANTENNA_TWO_CONNECTED(13), //
		ANTENNA_ONE_TRANSMIT_POWER(14), //
		ANTENNA_TWO_TRANSMIT_POWER(15), //
		// runtime config
		TAGS_IN_FIELD(20), //
		MEMORY_SELECTOR(21), //
		EPC_LENGTH(22), //
		TID_LENGTH(23), //
		USER_LENGTH(24), //
		SELECTION_MASK_COUNT(25), //
		SELECTION_MASK_MAX_LENGTH(26), //
		CUSTOM_COMMAND_MAX_LENGTH(27), //
		TAG_COUNT(28), //
		LAST_ERROR(29), //
		ACCESS_PASSWORD(30), //
		ANTENNA_MASK(31), //
		// runtime selection mask
		SELECTION_MASK_BANK(40), //
		SELECTION_MASK_LENGTH(41), //
		SELECTION_MASK_OFFSET(42), //
		SELECTION_MASK(43), //
		// runtime tag
		LOCK_OPERATION(50), //
		KILL_OPERATION(51), //
		KILL_PWD(52), //
		ACCESS_PWD(53), //
		CRC(54), //
		PC(55), //
		EPC(56), //
		XPC(57), //
		TID_BANK(58), //
		USER_BANK(59), //
		CUSTOM_COMMAND_LENGTH(60), //
		CUSTOM_COMMAND_DATA(61);

		private Field field;

		private RfFieldType(int field) {
			this.field = new Field(field);
		}

		public Field getField() {
			return field;
		}

		public static RfFieldType get(Field field) {
			if (field == null) {
				return null;
			}
			for (RfFieldType f : RfFieldType.values()) {
				if (field.equals(f.getField())) {
					return f;
				}
			}
			return null;
		}
	}

	enum RfErrorCode {
		NONE(0), //
		TAGS_IN_FIELD_EXCEEDED(0x0201), //
		TAG_MEMORY_OVERRUN(0x0301), //
		TAG_MEMORY_LOCKED(0x0302), //
		INSUFFICIENT_POWER(0x0303), //
		NON_SPECIFIC_TAG_ERROR(0x0304), //
		NO_RESPONSE_FROM_TAG(0x0305), //
		NON_SPECIFIC_READER_ERROR(0x0306), //
		INCORRECT_PASSWORD(0x0307), //
		ZERO_KILL_PASSWORD(0x0308);

		private int value;

		private RfErrorCode(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	static final RfFieldType[] FIELDS = { //
			// device info
			RfFieldType.VENDOR_NAME, //
			RfFieldType.PRODUCT_CODE, //
			RfFieldType.MAJOR_MINOR_REVISION, //
			RfFieldType.SERIAL_NUMBER, //
			RfFieldType.HARDWARE_REVISION, //
			RfFieldType.BASE_FIRMWARE, //

			// device config
			RfFieldType.COMMUNICATION_STANDARD, //
			RfFieldType.NUMBER_OF_ANTENNAS, //
			RfFieldType.ANTENNA_ONE_CONNECTED, //
			RfFieldType.ANTENNA_TWO_CONNECTED, //
			RfFieldType.ANTENNA_ONE_TRANSMIT_POWER, //
			RfFieldType.ANTENNA_TWO_TRANSMIT_POWER, //

			// runtime config
			RfFieldType.TAGS_IN_FIELD, //
			RfFieldType.MEMORY_SELECTOR, //
			RfFieldType.EPC_LENGTH, //
			RfFieldType.TID_LENGTH, //
			RfFieldType.USER_LENGTH, //
			RfFieldType.SELECTION_MASK_COUNT, //
			RfFieldType.SELECTION_MASK_MAX_LENGTH, //
			RfFieldType.CUSTOM_COMMAND_MAX_LENGTH, //
			RfFieldType.TAG_COUNT, //
			RfFieldType.LAST_ERROR, //
			RfFieldType.ACCESS_PASSWORD, //
			RfFieldType.ANTENNA_MASK, //

			// runtime selection mask
			RfFieldType.SELECTION_MASK_BANK, //
			RfFieldType.SELECTION_MASK_LENGTH, //
			RfFieldType.SELECTION_MASK_OFFSET, //
			RfFieldType.SELECTION_MASK, //

			// runtime tag
			RfFieldType.LOCK_OPERATION, //
			RfFieldType.KILL_OPERATION, //
			RfFieldType.KILL_PWD, //
			RfFieldType.ACCESS_PWD, //
			RfFieldType.CRC, //
			RfFieldType.PC, //
			RfFieldType.EPC, //
			RfFieldType.XPC, //
			RfFieldType.TID_BANK, //
			RfFieldType.USER_BANK, //
			RfFieldType.CUSTOM_COMMAND_LENGTH, //
			RfFieldType.CUSTOM_COMMAND_DATA };

	static final class RfFieldProperties {
		public FieldType type;
		public FieldDataType dataType;
		public int addressQuantity;
		public boolean isConfigField;

		private RfFieldProperties(FieldType type, FieldDataType dataType, int addressQuantity, boolean isConfigField) {
			this.type = type;
			this.dataType = dataType;
			this.addressQuantity = addressQuantity;
			this.isConfigField = isConfigField;
		}
	}

	@SuppressWarnings("serial")
	static final Map<RfFieldType, RfFieldProperties> FIELD_PROPERTIES = new HashMap<RfFieldType, RfFieldProperties>() {
		{
			// device info
			put(RfFieldType.VENDOR_NAME, new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.STRING, 128, true));
			put(RfFieldType.PRODUCT_CODE,
					new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.STRING, 128, true));
			put(RfFieldType.MAJOR_MINOR_REVISION,
					new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.STRING, 128, true));
			put(RfFieldType.SERIAL_NUMBER, new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.SHORT, 4, false));
			put(RfFieldType.HARDWARE_REVISION,
					new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.STRING, 1, false));
			put(RfFieldType.BASE_FIRMWARE,
					new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.STRING, 3, false));

			// device config
			put(RfFieldType.COMMUNICATION_STANDARD,
					new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.NUMBER_OF_ANTENNAS,
					new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.ANTENNA_ONE_CONNECTED,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.ANTENNA_TWO_CONNECTED,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.ANTENNA_ONE_TRANSMIT_POWER,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.SHORT, 1, false));
			put(RfFieldType.ANTENNA_TWO_TRANSMIT_POWER,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.SHORT, 1, false));

			// runtime config
			put(RfFieldType.TAGS_IN_FIELD,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, true));
			put(RfFieldType.MEMORY_SELECTOR,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.SHORT, 1, true));
			put(RfFieldType.EPC_LENGTH, new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, true));
			put(RfFieldType.TID_LENGTH, new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, true));
			put(RfFieldType.USER_LENGTH, new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, true));
			put(RfFieldType.SELECTION_MASK_COUNT,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, true));
			put(RfFieldType.SELECTION_MASK_MAX_LENGTH,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, true));
			put(RfFieldType.CUSTOM_COMMAND_MAX_LENGTH,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, true));
			put(RfFieldType.TAG_COUNT, new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.LAST_ERROR, new RfFieldProperties(FieldType.INPUT_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.ACCESS_PASSWORD,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.SHORT, 2, false));
			put(RfFieldType.ANTENNA_MASK,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.SHORT, 1, false));

			// runtime selection mask
			put(RfFieldType.SELECTION_MASK_BANK,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.SELECTION_MASK_LENGTH,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.SELECTION_MASK_OFFSET,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.USHORT, 1, false));
			put(RfFieldType.SELECTION_MASK,
					new RfFieldProperties(FieldType.HOLDING_REGISTERS, FieldDataType.BYTE, 0, false));

			// runtime tag
			put(RfFieldType.LOCK_OPERATION, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.SHORT, 1, false));
			put(RfFieldType.KILL_OPERATION, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.SHORT, 2, false));
			put(RfFieldType.KILL_PWD, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.BYTE, 2, false));
			put(RfFieldType.ACCESS_PWD, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.BYTE, 2, false));
			put(RfFieldType.CRC, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.USHORT, 1, false));
			put(RfFieldType.PC, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.USHORT, 1, false));
			put(RfFieldType.EPC, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.BYTE, 0, false));
			put(RfFieldType.XPC, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.USHORT, 2, false));
			put(RfFieldType.TID_BANK, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.BYTE, 0, false));
			put(RfFieldType.USER_BANK, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.BYTE, 0, false));
			put(RfFieldType.CUSTOM_COMMAND_LENGTH, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.USHORT, 1, false));
			put(RfFieldType.CUSTOM_COMMAND_DATA, new RfFieldProperties(FieldType.HOLDING_REGISTERS, //
					FieldDataType.BYTE, 0, false));
		}
	};
}
