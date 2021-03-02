package havis.custom.harting.modbus.reader.rf;

import havis.custom.harting.modbus.reader.api.Field;
import havis.custom.harting.modbus.reader.api.FieldValue;
import havis.custom.harting.modbus.reader.api.Module;
import havis.custom.harting.modbus.reader.api.ModuleException;
import havis.custom.harting.modbus.reader.common.ServiceFactory;
import havis.custom.harting.modbus.reader.core.FieldProperties;
import havis.custom.harting.modbus.reader.rf.RfConstants.RfFieldType;
import havis.device.rf.RFDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RfModule implements Module {
	private static final Logger log = Logger.getLogger(RfModule.class.getName());

	private class RfFieldProperties extends FieldProperties {
		private Integer expandedFieldIndex;

		private RfFieldProperties(int address, Field[] scannedFields) {
			super(address, scannedFields);
		}
	}

	private final ServiceFactory<RFDevice> rfDeviceServiceFactory;
	private RfConnector rfConnector;
	private List<RfFieldType> expandedFields = new ArrayList<>();
	private List<Integer> fieldGroupIndices = new ArrayList<>();
	private final Map<RfFieldType, FieldValue> fieldValues = new HashMap<>();

	public RfModule(ServiceFactory<RFDevice> rfDeviceServiceFactory) {
		this.rfDeviceServiceFactory = rfDeviceServiceFactory;
	}

	@Override
	public void open(int timeout) throws ModuleException {
		rfConnector = new RfConnector(rfDeviceServiceFactory);
		rfConnector.open(timeout);
	}

	@Override
	public void close(int timeout) throws ModuleException {
		rfConnector.close();
	}

	@Override
	public void expandFields() {
		expandedFields.clear();
		fieldGroupIndices.clear();
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Expanded fields:");
		}
		// field -> group index
		Map<RfFieldType, Integer> groupIndices = new HashMap<>();
		int blockStartIndex = 0;
		int blockCount = 0;
		FieldProperties fieldProps = null;
		for (int fieldIndex = 0; fieldIndex < RfConstants.FIELDS.length; fieldIndex++) {
			RfFieldType field = RfConstants.FIELDS[fieldIndex];
			int fieldCount = getFieldCount(field);
			// get field address quantity BEFORE field index is changed
			int fieldAddressQuantity = getFieldAddressQuantity(field);
			// processing of a block must be started/finished even if the
			// first/last field does is not used or its address quantity is 0
			switch (field) {
			case SELECTION_MASK_BANK: // -> SELECTION_MASK
			case LOCK_OPERATION: // -> CUSTOM_COMMAND_DATA
				blockStartIndex = fieldIndex;
				if (blockCount <= 0) {
					blockCount = fieldCount;
				}
				break;
			case SELECTION_MASK:
			case CUSTOM_COMMAND_DATA:
				blockCount--;
				// if a further block must be created
				if (blockCount > 0) {
					// continue with first field of block
					fieldIndex = blockStartIndex - 1;
				}
				break;
			default:
			}
			if (fieldCount > 0 && fieldAddressQuantity > 0) {
				expandedFields.add(field);
				Integer groupIndex = null;
				if (fieldCount > 1) {
					groupIndex = groupIndices.get(field);
					if (groupIndex == null) {
						groupIndex = 0;
					}
					fieldGroupIndices.add(groupIndex);
					groupIndices.put(field, groupIndex + 1);
				} else {
					fieldGroupIndices.add(0);
				}
				if (log.isLoggable(Level.FINE)) {
					fieldProps = getFieldProperties(fieldProps, null /* selectAddress */, field.getField(), false /* enableLogging */);
					log.log(Level.FINE, " " + fieldProps.getAddress() + " -> " + field + (fieldCount > 1 ? "[" + groupIndex + "]" : ""));
					if (fieldIndex + 1 == RfConstants.FIELDS.length) {
						log.log(Level.FINE, " " + (fieldProps.getAddress() + fieldProps.getAddressQuantity()));
					}
				}
			}
		}
	}

	/**
	 * Determines field properties. The properties of the first field which applies
	 * to one of the conditions is returned. If no field is found the address after
	 * the last field and all scanned fields are returned.
	 * 
	 * @param rt
	 * @param startFieldProps
	 * @param selectAddress
	 * @param selectField
	 * @return
	 */
	@Override
	public FieldProperties getFieldProperties(FieldProperties startFieldProps, Integer selectAddress, Field selectField) {
		return getFieldProperties(startFieldProps, selectAddress, selectField, true /* enableLogging */);
	}

	@Override
	public FieldValue getFieldValue(Field field, int fieldGroupIndex) throws ModuleException {
		RfField f;
		if (field instanceof RfField)
			f = (RfField) field;
		else
			f = new RfField(field);

		FieldValue ret = rfConnector.getFieldValue(f, fieldGroupIndex);
		if (ret == null) {
			ret = fieldValues.get(f.getType());
		}
		return ret;
	}

	@Override
	public void setFieldValue(Field field, int fieldGroupIndex, FieldValue value) throws ModuleException {
		RfField f;
		if (field instanceof RfField)
			f = (RfField) field;
		else
			f = new RfField(field);

		rfConnector.setFieldValue(f, fieldGroupIndex, value);
		switch (f.getType()) {
		case TAGS_IN_FIELD:
		case MEMORY_SELECTOR:
		case EPC_LENGTH:
		case TID_LENGTH:
		case USER_LENGTH:
		case SELECTION_MASK_COUNT:
		case SELECTION_MASK_MAX_LENGTH:
		case CUSTOM_COMMAND_MAX_LENGTH:
			fieldValues.put(f.getType(), value);
			break;
		default:
		}
	}

	private FieldProperties getFieldProperties(FieldProperties startFieldProps, Integer selectAddress, Field selectField, boolean enableLogging) {
		RfFieldType selectRfField = RfFieldType.get(selectField);

		int address = 0;
		int startExpandedFieldIndex = 0;
		if (startFieldProps != null) {
			address = startFieldProps.getAddress();
			startExpandedFieldIndex = ((RfFieldProperties) startFieldProps).expandedFieldIndex;
		}
		int startAddress = address;
		for (int i = startExpandedFieldIndex; i < expandedFields.size(); i++) {
			RfFieldType field = expandedFields.get(i);
			// field length
			int fieldAddressQuantity = getFieldAddressQuantity(field);

			if (selectField != null && field == selectRfField || selectAddress != null && selectAddress.intValue() >= address && selectAddress.intValue() < address + fieldAddressQuantity) {

				// if the address points to current field
				if (enableLogging && log.isLoggable(Level.FINE)) {
					if (selectAddress != null) {
						log.log(Level.FINE, "Found " + selectAddress + " -> " + field);
					} else {
						log.log(Level.FINE, "Found " + selectRfField + " -> " + address);
					}
				}
				if (enableLogging && log.isLoggable(Level.FINER)) {
					RfFieldType startField = expandedFields.get(startExpandedFieldIndex);
					if (selectAddress != null) {
						log.log(Level.FINER, "  started with " + startAddress + " -> " + startField);
					} else {
						log.log(Level.FINER, "  started with " + startField + " -> " + startAddress);
					}
				}

				Field current = field.getField();
				if (selectAddress != null) {
					// if the field is addressed, enrich with offset and length
					int offset;
					int length;
					if (startFieldProps != null && selectAddress != null) {
						// this is an end address
						int lengthFromZero = (selectAddress.intValue() - address) + 1;
						int otherOffset = ((RfField) startFieldProps.getField()).getOffset();
						if (startFieldProps.getField().equals(field.getField())) {
							// in the same field, use other offset
							offset = otherOffset;
						} else {
							// another field, start from 0
							offset = 0;
						}
						length = lengthFromZero - offset;
					} else {
						length = 0; // we don't know
						// this is a start address
						offset = selectAddress.intValue() - address;
					}
					current = new RfField(current, offset, length);
				}

				Field[] scannedFields = new Field[i - startExpandedFieldIndex + 1];
				for (int j = 0; j < scannedFields.length; j++) {
					Field f = expandedFields.get(j + startExpandedFieldIndex).getField();
					if (f.equals(current))
						f = current;
					else if (startFieldProps != null && f.equals(startFieldProps.getField()))
						f = startFieldProps.getField();

					scannedFields[j] = f;
				}

				RfFieldProperties ret = new RfFieldProperties(address, scannedFields);
				ret.expandedFieldIndex = i;
				ret.setField(current);
				ret.setAddressQuantity(fieldAddressQuantity);
				ret.setType(RfConstants.FIELD_PROPERTIES.get(field).type);
				ret.setDataType(RfConstants.FIELD_PROPERTIES.get(field).dataType);
				ret.setFieldGroupIndex(fieldGroupIndices.get(i));
				ret.setConfigField(RfConstants.FIELD_PROPERTIES.get(field).isConfigField);
				return ret;
			}
			address += fieldAddressQuantity;
		}
		Field[] scannedFields = new Field[expandedFields.size() - startExpandedFieldIndex];
		for (int j = 0; j < scannedFields.length; j++) {
			Field f = expandedFields.get(j + startExpandedFieldIndex).getField();
			if (startFieldProps != null && f.equals(startFieldProps.getField()))
				f = startFieldProps.getField();

			scannedFields[j] = f;
		}
		return new RfFieldProperties(address, scannedFields);
	}

	private int getFieldAddressQuantity(RfFieldType field) {
		switch (field) {
		case SELECTION_MASK:
			return fieldValues.get(RfFieldType.SELECTION_MASK_MAX_LENGTH).getUShortValue()[0];
		case EPC:
			return fieldValues.get(RfFieldType.EPC_LENGTH).getUShortValue()[0];
		case TID_BANK:
			return fieldValues.get(RfFieldType.TID_LENGTH).getUShortValue()[0];
		case USER_BANK:
			return fieldValues.get(RfFieldType.USER_LENGTH).getUShortValue()[0];
		case CUSTOM_COMMAND_DATA:
			return fieldValues.get(RfFieldType.CUSTOM_COMMAND_MAX_LENGTH).getUShortValue()[0];
		default:
			return RfConstants.FIELD_PROPERTIES.get(field).addressQuantity;
		}
	}

	private int getFieldCount(RfFieldType field) {
		switch (field) {
		case SELECTION_MASK_BANK:
		case SELECTION_MASK_LENGTH:
		case SELECTION_MASK_OFFSET:
		case SELECTION_MASK:
			return fieldValues.get(RfFieldType.SELECTION_MASK_COUNT).getUShortValue()[0];
		case LOCK_OPERATION:
		case KILL_OPERATION:
		case KILL_PWD:
		case ACCESS_PWD:
		case CRC:
		case PC:
		case EPC:
		case XPC:
		case TID_BANK:
		case USER_BANK:
		case CUSTOM_COMMAND_LENGTH:
		case CUSTOM_COMMAND_DATA:
			int count = fieldValues.get(RfFieldType.TAGS_IN_FIELD).getUShortValue()[0];
			if (count == 0) {
				return 0;
			}
			short memorySelector = fieldValues.get(RfFieldType.MEMORY_SELECTOR).getShortValue()[0];
			switch (field) {
			case XPC:
				if ((memorySelector & 16) != 16) {
					return 0;
				}
				break;
			case PC:
				if ((memorySelector & 8) != 8) {
					return 0;
				}
				break;
			case CRC:
				if ((memorySelector & 4) != 4) {
					return 0;
				}
				break;
			case ACCESS_PWD:
				if ((memorySelector & 2) != 2) {
					return 0;
				}
				break;
			case KILL_PWD:
				if ((memorySelector & 1) != 1) {
					return 0;
				}
			default:
			}
			return count;
		default:
			return 1;
		}
	}
}
