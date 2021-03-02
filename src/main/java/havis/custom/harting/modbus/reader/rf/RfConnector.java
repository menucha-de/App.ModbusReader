package havis.custom.harting.modbus.reader.rf;

import havis.custom.harting.modbus.reader.api.FieldValue;
import havis.custom.harting.modbus.reader.api.ModuleException;
import havis.custom.harting.modbus.reader.common.ServiceFactory;
import havis.custom.harting.modbus.reader.rf.RfConstants.RfErrorCode;
import havis.custom.harting.modbus.reader.rf.RfConstants.RfFieldType;
import havis.device.rf.RFConsumer;
import havis.device.rf.RFDevice;
import havis.device.rf.capabilities.Capabilities;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.capabilities.DeviceCapabilities;
import havis.device.rf.capabilities.RegulatoryCapabilities;
import havis.device.rf.capabilities.TransmitPowerTableEntry;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.AntennaProperties;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.CustomOperation;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.TagOperation;
import havis.device.rf.tag.operation.WriteOperation;
import havis.device.rf.tag.result.CustomResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class RfConnector {
	private static final int MAX_WORDS_READ = 255;
	private static final int MAX_WORDS_WRITE = 122;

	private static final Logger log = Logger.getLogger(RfConnector.class.getName());

	class ExTagData {
		TagData td;
		int customCmdLength;
		byte[] customCmd;
	}

	class SelectionMask {
		int bank;
		int bitLength;
		int bitOffset;
		byte[] data;

		@Override
		public String toString() {
			return "SelectionMask [bank=" + bank + ", bitLength=" + bitLength + ", bitOffset=" + bitOffset + ", data="
					+ Arrays.toString(data) + "]";
		}
	}

	private ServiceFactory<RFDevice> rfDeviceServiceFactory;
	private RFDevice service;
	private String vendorName;
	private String productCode;
	private String majorMinorRevision;
	private int tagsInField;
	private int tidLength;
	private int selectionMaskCount;
	private List<SelectionMask> selectionMasks = new ArrayList<>();
	private short antennaMask;
	private int accessPassword;
	private RfConstants.RfErrorCode lastError;
	private List<TagData> tagData = new ArrayList<>();
	// tagData index -> extended tag data
	private Map<Integer, ExTagData> exTagData = new HashMap<>();

	RfConnector(ServiceFactory<RFDevice> rfDeviceServiceFactory) {
		this.rfDeviceServiceFactory = rfDeviceServiceFactory;
	}

	void open(int timeout) throws ModuleException {
		long start = System.currentTimeMillis();
		try {
			service = rfDeviceServiceFactory.getService("" /* host */, 0 /* port */, timeout);
		} catch (Exception e) {
			throw new ModuleException("Cannot get Rf device service within " + timeout + "ms", e);
		}
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Opening connection to Rf device");
		}
		int remainingTimeout = (int) (System.currentTimeMillis() - start);
		try {
			service.openConnection(new RFConsumer() {

				@Override
				public void keepAlive() {
				}

				@Override
				public List<TagOperation> getOperations(TagData arg0) {
					return null;
				}

				@Override
				public void connectionAttempted() {
				}
			}, remainingTimeout);
		} catch (Exception e) {
			throw new ModuleException("Cannot open connection to Rf device within " + remainingTimeout + "ms", e);
		}
		vendorName = "";
		productCode = "";
		majorMinorRevision = "";
		tagsInField = 0;
		tidLength = 0;
		selectionMaskCount = 0;
		selectionMasks.clear();
		antennaMask = 0;
		accessPassword = 0;
		lastError = RfConstants.RfErrorCode.NONE;
		tagData.clear();
		exTagData.clear();
	}

	void close() throws ModuleException {
		if (service != null) {
			try {
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Closing connection to Rf device");
				}
				service.closeConnection();
			} catch (ConnectionException e) {
				throw new ModuleException("Cannot close connection to Rf device", e);
			}
		}
		service = null;
	}

	FieldValue getFieldValue(RfField field, int fieldGroupIndex) throws ModuleException {
		RfConstants.RfErrorCode errorCode = RfConstants.RfErrorCode.NONE;
		try {
			switch (field.getType()) {
			// device info
			case VENDOR_NAME:
				return new FieldValue(new String[] { vendorName });
			case PRODUCT_CODE:
				return new FieldValue(new String[] { productCode });
			case MAJOR_MINOR_REVISION:
				return new FieldValue(new String[] { majorMinorRevision });
			case SERIAL_NUMBER:
				String strValue = getProperty("mica.device.serial_no", "0" /* dflt */);
				int addressQuantity = RfConstants.FIELD_PROPERTIES.get(RfFieldType.SERIAL_NUMBER).addressQuantity;
				return new FieldValue(bigInt2shorts(new BigInteger(strValue), addressQuantity));
			case HARDWARE_REVISION:
				return new FieldValue(new String[] { getProperty("mica.device.hw_revision", "" /* dflt */) });
			case BASE_FIRMWARE:
				return new FieldValue(new String[] { getProperty("mica.firmware.base_version", "" /* dflt */) });

			// device config
			case COMMUNICATION_STANDARD:
				RegulatoryCapabilities regulatoryCaps = (RegulatoryCapabilities) getCapabilities(
						CapabilityType.REGULATORY_CAPABILITIES);
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Received " + CapabilityType.REGULATORY_CAPABILITIES);
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "  communicationStandard=" + regulatoryCaps.getCommunicationStandard());
				}
				return new FieldValue(new int[] { regulatoryCaps.getCommunicationStandard() });
			case NUMBER_OF_ANTENNAS:
				DeviceCapabilities deviceCaps = (DeviceCapabilities) getCapabilities(
						CapabilityType.DEVICE_CAPABILITIES);
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Received " + CapabilityType.DEVICE_CAPABILITIES);
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "  numberOfAntennas=" + deviceCaps.getNumberOfAntennas());
				}
				return new FieldValue(new int[] { deviceCaps.getNumberOfAntennas() });
			case ANTENNA_ONE_CONNECTED:
				AntennaProperties antennaProps = (AntennaProperties) getConfiguration(
						ConfigurationType.ANTENNA_PROPERTIES, (short) 1 /* antennaId */);
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Received " + ConfigurationType.ANTENNA_PROPERTIES);
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "  isConnected=" + antennaProps.isConnected());
				}
				return new FieldValue(new int[] { antennaProps.isConnected() ? 1 : 0 });
			case ANTENNA_TWO_CONNECTED:
				antennaProps = (AntennaProperties) getConfiguration(ConfigurationType.ANTENNA_PROPERTIES,
						(short) 2 /* antennaId */);
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Received " + ConfigurationType.ANTENNA_PROPERTIES);
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "  isConnected=" + antennaProps.isConnected());
				}
				return new FieldValue(new int[] { antennaProps.isConnected() ? 1 : 0 });
			case ANTENNA_ONE_TRANSMIT_POWER:
				Short transmitPower = getTransmitPower((short) 1 /* antennaId */);
				return (transmitPower == null) ? null : new FieldValue(new short[] { transmitPower });
			case ANTENNA_TWO_TRANSMIT_POWER:
				transmitPower = getTransmitPower((short) 2 /* antennaId */);
				return (transmitPower == null) ? null : new FieldValue(new short[] { transmitPower });

			// runtime configuration
			case TAGS_IN_FIELD:
				return new FieldValue(new int[] { tagsInField });
			case MEMORY_SELECTOR:
			case EPC_LENGTH:
				// field sizes (see RfModule)
				return null;
			case TID_LENGTH:
				return new FieldValue(new int[] { tidLength });
			case USER_LENGTH:
				// field size (see RfModule)
				return null;
			case SELECTION_MASK_COUNT:
				return new FieldValue(new int[] { selectionMaskCount });
			case SELECTION_MASK_MAX_LENGTH:
			case CUSTOM_COMMAND_MAX_LENGTH:
				// field sizes (see RfModule)
				return null;
			default:
			}

			// runtime
			switch (field.getType()) {
			case SELECTION_MASK_BANK:
			case SELECTION_MASK_LENGTH:
			case SELECTION_MASK_OFFSET:
			case SELECTION_MASK:
				if (fieldGroupIndex >= selectionMaskCount) {
					throw new RfModuleException("Reading field " + field + ": Max. selection mask count exceeded: "
							+ (fieldGroupIndex + 1) + "/" + selectionMaskCount,
							RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR);
				}
			default:
			}

			switch (field.getType()) {
			case TAG_COUNT: // used field values: ANTENNA_MASK,
				// SELECTION_MASK_BANK, SELECTION_MASK_LENGTH,
				// SELECTION_MASK_OFFSET, SELECTION_MASK,
				// TAGS_IN_FIELD
				tagData = inventory(antennaMask, selectionMasks, tagsInField);
				exTagData.clear();
				// return tag count
				return new FieldValue(new int[] { tagData.size() });
			case LAST_ERROR:
				return new FieldValue(new int[] { lastError.getValue() });
			case ACCESS_PASSWORD:
				return new FieldValue(new short[] { (short) (accessPassword >> 16), (short) accessPassword });
			case ANTENNA_MASK:
				return new FieldValue(new short[] { antennaMask });
			case SELECTION_MASK_BANK:
				return new FieldValue(new int[] { selectionMasks.get(fieldGroupIndex).bank });
			case SELECTION_MASK_LENGTH:
				return new FieldValue(new int[] { selectionMasks.get(fieldGroupIndex).bitLength });
			case SELECTION_MASK_OFFSET:
				return new FieldValue(new int[] { selectionMasks.get(fieldGroupIndex).bitOffset });
			case SELECTION_MASK:
				return new FieldValue(selectionMasks.get(fieldGroupIndex).data);
			default:
			}

			if (fieldGroupIndex >= tagData.size()) {
				throw new RfModuleException("Reading field " + field + ": Max. tag count exceeded: "
						+ (fieldGroupIndex + 1) + "/" + tagData.size(),
						RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR);
			}
			ExTagData exTd = getExTagData(fieldGroupIndex);
			switch (field.getType()) {
			case LOCK_OPERATION:
			case KILL_OPERATION:
				// unsupported
				return null;
			case KILL_PWD: // used field values: TAG_COUNT, ACCESS_PASSWORD
				return new FieldValue(read(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 0 /* bank */, (short) 0 /* wordOffset */, (short) 2 /* wordCount */, accessPassword));
			case ACCESS_PWD: // dependencies: TAG_COUNT, ACCESS_PASSWORD
				return new FieldValue(read(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 0 /* bank */, (short) 2 /* wordOffset */, (short) 2 /* wordCount */, accessPassword));
			case CRC: // used field values: TAG_COUNT
				return new FieldValue(new int[] { DataTypeConverter.ushort(exTd.td.getCrc()) });
			case PC: // used field values: TAG_COUNT
				return new FieldValue(new int[] { DataTypeConverter.ushort(exTd.td.getPc()) });
			case EPC: // used field values: TAG_COUNT
				return new FieldValue(exTd.td.getEpc());
			case XPC: // used field values: TAG_COUNT
				int xpc = exTd.td.getXpc();
				return new FieldValue(new int[] { xpc >> 16, xpc & 0xFFFF });
			case TID_BANK: // used field values: TAG_COUNT, ACCESS_PASSWORD
				return new FieldValue(read(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 2 /* bank */, (short) 0 /* wordOffset */, (short) tidLength /* wordCount */, accessPassword));
			case USER_BANK: // used field values: TAG_COUNT, ACCESS_PASSWORD
				return new FieldValue(shiftData(read(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 3 /* bank */,
						(short) field.getOffset(), (short) field.getLength(), accessPassword), (short) field.getOffset(), (short) 0 /* we don't know the register length here */));
			case CUSTOM_COMMAND_LENGTH: // used field values: TAG_COUNT
				return new FieldValue(new int[] { exTd.customCmdLength });
			case CUSTOM_COMMAND_DATA: // used field values: TAG_COUNT
				return (exTd.customCmd == null) ? null : new FieldValue(exTd.customCmd);
			default:
			}
			return null;
		} catch (RfModuleException e) {
			errorCode = e.getErrorCode();
			throw e;
		} catch (ModuleException e) {
			errorCode = RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR;
			throw e;
		} catch (Exception e) {
			errorCode = RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR;
			throw new ModuleException("Failed to get field value: " + e.toString());
		} finally {
			lastError = errorCode;
		}
	}

	void setFieldValue(RfField field, int fieldGroupIndex, FieldValue value) throws ModuleException {
		if (value == null) {
			return;
		}
		RfErrorCode errorCode = RfErrorCode.NONE;
		try {
			switch (field.getType()) {
			// device info
			case VENDOR_NAME:
				if (value.getStringValue().length > 0) {
					vendorName = value.getStringValue()[0];
				}
				return;
			case PRODUCT_CODE:
				if (value.getStringValue().length > 0) {
					productCode = value.getStringValue()[0];
				}
				return;
			case MAJOR_MINOR_REVISION:
				if (value.getStringValue().length > 0) {
					majorMinorRevision = value.getStringValue()[0];
				}
				return;
			case SERIAL_NUMBER:
			case HARDWARE_REVISION:
			case BASE_FIRMWARE:
				// read only
				return;

			// device config
			case COMMUNICATION_STANDARD:
			case NUMBER_OF_ANTENNAS:
				// read only
				return;
			case ANTENNA_ONE_CONNECTED:
				if (value.getUShortValue().length > 0) {
					setAntennaProperties((short) 1 /* antennaId */, value.getUShortValue()[0] == 1);
				}
				return;
			case ANTENNA_TWO_CONNECTED:
				if (value.getUShortValue().length > 0) {
					setAntennaProperties((short) 2 /* antennaId */, value.getUShortValue()[0] == 1);
				}
				return;
			case ANTENNA_ONE_TRANSMIT_POWER:
				if (value.getShortValue().length > 0) {
					setTransmitPower((short) 1 /* antennaId */, value.getShortValue()[0]);
				}
				return;
			case ANTENNA_TWO_TRANSMIT_POWER:
				if (value.getShortValue().length > 0) {
					setTransmitPower((short) 2 /* antennaId */, value.getShortValue()[0]);
				}
				return;

			// runtime configuration
			case TAGS_IN_FIELD:
				if (value.getUShortValue().length > 0) {
					tagsInField = value.getUShortValue()[0];
				}
				return;
			case MEMORY_SELECTOR:
			case EPC_LENGTH:
				// field sizes (see RfModule)
				return;
			case TID_LENGTH:
				if (value.getUShortValue().length > 0) {
					tidLength = value.getUShortValue()[0];
				}
				return;
			case USER_LENGTH:
				// field size (see RfModule)
				return;
			case SELECTION_MASK_COUNT:
				if (value.getUShortValue().length > 0) {
					selectionMaskCount = value.getUShortValue()[0];
					selectionMasks.clear();
					for (int i = 0; i < selectionMaskCount; i++) {
						selectionMasks.add(new SelectionMask());
					}
				}
				return;
			case SELECTION_MASK_MAX_LENGTH:
			case CUSTOM_COMMAND_MAX_LENGTH:
				// field sizes (see RfModule)
				return;
			default:
			}

			// runtime
			switch (field.getType()) {
			case SELECTION_MASK_BANK:
			case SELECTION_MASK_LENGTH:
			case SELECTION_MASK_OFFSET:
			case SELECTION_MASK:
				if (fieldGroupIndex >= selectionMaskCount) {
					throw new RfModuleException("Reading field " + field + ": Max. selection mask count exceeded: "
							+ (fieldGroupIndex + 1) + "/" + selectionMaskCount,
							RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR);
				}
			default:
			}

			switch (field.getType()) {
			case LAST_ERROR:
				// read only
				return;
			case ACCESS_PASSWORD:
				short[] shortValue = value.getShortValue();
				if (shortValue.length > 1) {
					accessPassword = shortValue[0] << 16 | shortValue[1];
				}
				return;
			case ANTENNA_MASK:
				if (value.getShortValue().length > 0) {
					antennaMask = value.getShortValue()[0];
				}
				return;
			case SELECTION_MASK_BANK:
				if (value.getUShortValue().length > 0) {
					selectionMasks.get(fieldGroupIndex).bank = value.getUShortValue()[0];
				}
				return;
			case SELECTION_MASK_LENGTH:
				if (value.getUShortValue().length > 0) {
					selectionMasks.get(fieldGroupIndex).bitLength = value.getUShortValue()[0];
				}
				return;
			case SELECTION_MASK_OFFSET:
				if (value.getUShortValue().length > 0) {
					selectionMasks.get(fieldGroupIndex).bitOffset = value.getUShortValue()[0];
				}
				return;
			case SELECTION_MASK:
				if (value.getByteValue().length > 0) {
					selectionMasks.get(fieldGroupIndex).data = value.getByteValue();
				}
				return;
			default:
			}

			if (fieldGroupIndex >= tagData.size()) {
				throw new RfModuleException("Writing field " + field + ": Max. tag count exceeded: "
						+ (fieldGroupIndex + 1) + "/" + tagData.size(),
						RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR);
			}
			ExTagData exTd = getExTagData(fieldGroupIndex);
			switch (field.getType()) {
			case LOCK_OPERATION:
			case KILL_OPERATION:
				// unsupported
				return;
			case KILL_PWD: // used field values: TAG_COUNT, ACCESS_PASSWORD
				write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 0 /* bank */, (short) 0 /* wordOffset */, value.getByteValue(), accessPassword);
				return;
			case ACCESS_PWD: // used field values: TAG_COUNT, ACCESS_PASSWORD
				write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 0 /* bank */, (short) 2 /* wordOffset */, value.getByteValue(), accessPassword);
				return;
			case CRC: // used field values: TAG_COUNT, ACCESS_PASSWORD
				int[] ushortValue = value.getUShortValue();
				if (ushortValue.length > 0 && DataTypeConverter.ushort(exTd.td.getCrc()) != ushortValue[0]) {
					write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 1 /* bank */, (short) 0 /* wordOffset */,
							shorts2bytes(ushorts2shorts(ushortValue)), accessPassword);
					exTd.td.setCrc((short) ushortValue[0]);
				}
				return;
			case PC: // used field values: TAG_COUNT, ACCESS_PASSWORD
				ushortValue = value.getUShortValue();
				if (ushortValue.length > 0 && DataTypeConverter.ushort(exTd.td.getPc()) != ushortValue[0]) {
					write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 1 /* bank */, (short) 1 /* wordOffset */,
							shorts2bytes(ushorts2shorts(ushortValue)), accessPassword);
					exTd.td.setPc((short) ushortValue[0]);
				}
				return;
			case EPC: // used field values: TAG_COUNT, ACCESS_PASSWORD
				if (!Arrays.equals(exTd.td.getEpc(), value.getByteValue())) {
					write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 1 /* bank */, (short) 2 /* wordOffset */,
							value.getByteValue(), accessPassword);
					exTd.td.setEpc(value.getByteValue());
				}
				return;
			case XPC: // used field values: TAG_COUNT, ACCESS_PASSWORD
				ushortValue = value.getUShortValue();
				int xpc = exTd.td.getXpc();
				if (ushortValue.length > 1 && ((xpc >> 16) != ushortValue[0] || (xpc & 0xFFFF) != ushortValue[1])) {
					short epcLength = (short) (exTd.td.getPc() >> 11);
					write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 1 /* bank */,
							(short) (2 + epcLength) /* wordOffset */, shorts2bytes(ushorts2shorts(ushortValue)),
							accessPassword);
					exTd.td.setXpc(ushortValue[0] << 16 | ushortValue[1] & 0xFFFF);
				}
				return;
			case TID_BANK: // used field values: TAG_COUNT, ACCESS_PASSWORD
				write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 2 /* bank */, (short) 0 /* wordOffset */, value.getByteValue(), accessPassword);
				return;
			case USER_BANK: // used field values: TAG_COUNT, ACCESS_PASSWORD
				write(exTd.td.getAntennaID(), exTd.td.getEpc(), (short) 3 /* bank */, (short) field.getOffset(), shiftData(value.getByteValue(), (short) (field.getOffset() * -1), (short) field.getLength()), accessPassword);
				return;
			case CUSTOM_COMMAND_LENGTH:
				if (value.getUShortValue().length > 0) {
					exTd.customCmdLength = value.getUShortValue()[0];
				}
				return;
			case CUSTOM_COMMAND_DATA: // used field values: TAG_COUNT,
										// ACCESS_PASSWORD
				exTd.customCmdLength = 0;
				exTd.customCmd = null;
				exTd.customCmd = execCustomCmd(exTd.td.getAntennaID(), exTd.td.getEpc(), value.getByteValue(),
						(short) exTd.customCmdLength, accessPassword);
				exTd.customCmdLength = (exTd.customCmd == null) ? 0 : exTd.customCmd.length * 8;
				return;
			default:
			}
		} catch (RfModuleException e) {
			errorCode = e.getErrorCode();
			throw e;
		} catch (ModuleException e) {
			errorCode = RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR;
			throw e;
		} catch (Exception e) {
			errorCode = RfConstants.RfErrorCode.NON_SPECIFIC_READER_ERROR;
			throw new ModuleException("Failed to set field value: " + e.toString());
		} finally {
			lastError = errorCode;
		}
	}

	private ExTagData getExTagData(int fieldGroupIndex) {
		TagData td = tagData.get(fieldGroupIndex);
		ExTagData exTd = exTagData.get(fieldGroupIndex);
		if (exTd == null) {
			exTd = new ExTagData();
			exTd.td = td;
			exTagData.put(fieldGroupIndex, exTd);
		}
		return exTd;
	}

	private List<Short> getAntennaIds(short antennaMask) {
		List<Short> ret = new ArrayList<>();
		if (antennaMask == 0) {
			ret.add((short) 0);
		} else {
			if ((antennaMask & 1) == 1) {
				ret.add((short) 1);
			}
			if ((antennaMask & 2) == 2) {
				ret.add((short) 2);
			}
		}
		return ret;
	}

	private Filter createEpcFilter(byte[] epc) {
		Filter filter = new Filter();
		filter.setBank((short) 1); // EPC
		filter.setBitOffset((short) 32); // CRC (2 bytes) + PC (2 bytes)
		filter.setBitLength((short) (epc.length * 8));
		byte[] mask = new byte[epc.length];
		Arrays.fill(mask, (byte) 0xFF);
		filter.setMask(mask);
		filter.setData(epc);
		filter.setMatch(true);
		return filter;
	}

	private List<Filter> createSelectionMaskFilter(List<SelectionMask> selectionMasks) {
		List<Filter> ret = new ArrayList<>();
		for (SelectionMask selectionMask : selectionMasks) {
			if (selectionMask.bitLength == 0) {
				continue;
			}
			if (selectionMask.data == null || selectionMask.data.length * 8 < selectionMask.bitLength) {
				int byteLength = selectionMask.bitLength / 8;
				if (selectionMask.bitLength % 2 == 1) {
					byteLength++;
				}
				byte[] data = new byte[byteLength];
				if (selectionMask.data != null) {
					for (int i = 0; i < selectionMask.data.length; i++) {
						data[i] = selectionMask.data[i];
					}
				}
				selectionMask.data = data;
			}
			Filter filter = new Filter();
			filter.setBank((short) selectionMask.bank);
			filter.setBitOffset((short) selectionMask.bitOffset);
			filter.setBitLength((short) selectionMask.bitLength);
			byte[] mask = new byte[selectionMask.data.length];
			Arrays.fill(mask, (byte) 0xFF);
			filter.setMask(mask);
			filter.setData(selectionMask.data);
			filter.setMatch(true);
			ret.add(filter);
		}
		return ret;
	}

	private List<TagData> inventory(short antennaMask, List<SelectionMask> selectionMasks, int tagsInField)
			throws ModuleException {
		List<TagData> ret;
		try {
			List<Short> antennaIds = getAntennaIds(antennaMask);
			List<TagOperation> tagOperations = new ArrayList<>();
			// read TID bank for ETB transponders
			if (tidLength > 0) {
				ReadOperation readTidBank = new ReadOperation();
				readTidBank.setBank((short) 2); // TID
				readTidBank.setOffset((short) 0);
				readTidBank.setLength((short) 0); // whole bank
				readTidBank.setPassword(accessPassword);
				readTidBank.setOperationId("g01");
				tagOperations.add(readTidBank);
			}
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Starting inventory");
			}
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "  antennaIds=" + antennaIds + ", selectionMasks=" + selectionMasks);
			}
			// execute an inventory
			ret = service.execute(antennaIds, createSelectionMaskFilter(selectionMasks), tagOperations);
		} catch (Exception e) {
			throw new ModuleException("Cannot execute RF service", e);
		}
		if (ret == null) {
			ret = new ArrayList<>();
		} else if (ret.size() > tagsInField) {
			throw new RfModuleException(
					"Inventory failed: Max. tag count exceeded: " + tagData.size() + "/" + tagsInField,
					RfConstants.RfErrorCode.TAGS_IN_FIELD_EXCEEDED);
		}
		// sort tags by EPC
		Collections.sort(ret, new Comparator<TagData>() {

			@Override
			public int compare(TagData a, TagData b) {
				return RfConnector.compare(a.getEpc(), b.getEpc());
			}
		});
		return ret;
	}

	private static int compare(byte[] a, byte[] b) {
		int minLength = a.length > b.length ? b.length : a.length;
		for (int i = 0; i < minLength; i++) {
			if (a[i] != b[i]) {
				return a[i] - b[i];
			}
		}
		return a.length - b.length;
	}

	private byte[] read(short antennaId, byte[] epc, short bank, short wordOffset, short wordCount, int accessPassword)
			throws ModuleException {
		if (wordCount > MAX_WORDS_READ) {
			throw new RfModuleException("Reader is unable to read more than " + MAX_WORDS_READ + " words at once", RfErrorCode.NON_SPECIFIC_READER_ERROR);
		}

		// create EPC filter
		Filter filter = createEpcFilter(epc);
		// create ReadOperation for password
		ReadOperation op = new ReadOperation();
		op.setBank(bank);
		op.setOffset(wordOffset);
		op.setLength(wordCount);
		op.setPassword(accessPassword);
		op.setOperationId("g01");
		List<TagData> result = null;
		try {
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Sending READ_OPERATION");
			}
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"  antennaId=" + antennaId + ", epc=" + Arrays.toString(epc) + ", bank=" + bank
								+ ", wordOffset=" + wordOffset + ", wordCount=" + wordCount + ", accessPassword="
								+ (accessPassword == 0 ? "0" : "***"));
			}
			result = service.execute(Arrays.asList(new Short[] { antennaId }), Arrays.asList(new Filter[] { filter }),
					Arrays.asList(new TagOperation[] { op }));
		} catch (Exception e) {
			throw new ModuleException("Cannot read data", e);
		}
		if (result == null || result.isEmpty() || result.get(0).getResultList() == null
				|| result.get(0).getResultList().isEmpty()) {
			throw new ModuleException("Cannot read data: Missing result");
		}
		ReadResult r = (ReadResult) result.get(0).getResultList().get(0);
		if (r.getResult() != ReadResult.Result.SUCCESS) {
			throw new RfModuleException("Cannot read data: " + r.getResult(),
					getErrorCode(r.getResult(), RfErrorCode.NON_SPECIFIC_READER_ERROR));
		}
		return r.getReadData();
	}

	private byte[] shiftData(byte[] data, short wordOffset, short wordCount) {
		// if we have an offset, we have to prepend or cut the bytes to
		// read/write the correct position in the registers
		if (data != null && data.length > 0) {
			if (wordOffset > 0) {
				int byteOffset = wordOffset * 2;
				int length = wordCount > 0 ? wordCount * 2 : data.length + byteOffset;
				byte[] result = new byte[length];
				if (byteOffset < length)
					System.arraycopy(data, 0, result, byteOffset, Math.min(data.length, length - byteOffset));
				return result;
			} else if (wordOffset < 0) {
				int byteOffset = wordOffset * -2;
				int length = wordCount > 0 ? wordCount * 2 : data.length - byteOffset;
				byte[] result = new byte[length];
				System.arraycopy(data, byteOffset, result, 0, Math.min(data.length - byteOffset, result.length));
				return result;
			}
		}

		return data;
	}

	private RfErrorCode getErrorCode(ReadResult.Result result, RfErrorCode dflt) {
		switch (result) {
		case INCORRECT_PASSWORD_ERROR:
			return RfErrorCode.INCORRECT_PASSWORD;
		case MEMORY_LOCKED_ERROR:
			return RfErrorCode.TAG_MEMORY_LOCKED;
		case MEMORY_OVERRUN_ERROR:
			return RfErrorCode.TAG_MEMORY_OVERRUN;
		case NON_SPECIFIC_READER_ERROR:
			return RfErrorCode.NON_SPECIFIC_READER_ERROR;
		case NON_SPECIFIC_TAG_ERROR:
			return RfErrorCode.NON_SPECIFIC_TAG_ERROR;
		case NO_RESPONSE_FROM_TAG:
			return RfErrorCode.NO_RESPONSE_FROM_TAG;
		case SUCCESS:
			return RfErrorCode.NONE;
		}
		return dflt;
	}

	private short write(short antennaId, byte[] epc, short bank, short wordOffset, byte[] data, int accessPassword)
			throws ModuleException {
		if (data != null && (data.length / 2) > MAX_WORDS_WRITE) {
			throw new RfModuleException("Reader is unable to write more than " + MAX_WORDS_WRITE + " words at once", RfErrorCode.NON_SPECIFIC_READER_ERROR);
		}

		// create EPC filter
		Filter filter = createEpcFilter(epc);
		// create WriteOperation for password
		WriteOperation op = new WriteOperation();
		op.setBank(bank);
		op.setOffset(wordOffset);
		op.setData(data);
		op.setPassword(accessPassword);
		op.setOperationId("g01");
		List<TagData> result = null;
		try {
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Sending WRITE_OPERATION");
			}
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"  antennaId=" + antennaId + ", epc=" + Arrays.toString(epc) + ", bank=" + bank
								+ ", wordOffset=" + wordOffset + ", data=" + Arrays.toString(data) + ", accessPassword="
								+ (accessPassword == 0 ? "0" : "***"));
			}
			result = service.execute(Arrays.asList(new Short[] { antennaId }), Arrays.asList(new Filter[] { filter }),
					Arrays.asList(new TagOperation[] { op }));
		} catch (Exception e) {
			throw new ModuleException("Cannot write data", e);
		}
		if (result == null || result.isEmpty() || result.get(0).getResultList() == null
				|| result.get(0).getResultList().isEmpty()) {
			throw new ModuleException("Cannot write data: Missing result");
		}
		WriteResult r = (WriteResult) result.get(0).getResultList().get(0);
		if (r.getResult() != WriteResult.Result.SUCCESS) {
			throw new RfModuleException("Cannot write data: " + r.getResult(),
					getErrorCode(r.getResult(), RfErrorCode.NON_SPECIFIC_READER_ERROR));
		}
		return r.getWordsWritten();
	}

	private RfErrorCode getErrorCode(WriteResult.Result result, RfErrorCode dflt) {
		switch (result) {
		case INCORRECT_PASSWORD_ERROR:
			return RfErrorCode.INCORRECT_PASSWORD;
		case INSUFFICIENT_POWER:
			return RfErrorCode.INSUFFICIENT_POWER;
		case MEMORY_LOCKED_ERROR:
			return RfErrorCode.TAG_MEMORY_LOCKED;
		case MEMORY_OVERRUN_ERROR:
			return RfErrorCode.TAG_MEMORY_OVERRUN;
		case NON_SPECIFIC_READER_ERROR:
			return RfErrorCode.NON_SPECIFIC_READER_ERROR;
		case NON_SPECIFIC_TAG_ERROR:
			return RfErrorCode.NON_SPECIFIC_TAG_ERROR;
		case NO_RESPONSE_FROM_TAG:
			return RfErrorCode.NO_RESPONSE_FROM_TAG;
		case SUCCESS:
			return RfErrorCode.NONE;
		}
		return dflt;
	}

	private byte[] execCustomCmd(short antennaId, byte[] epc, byte[] data, short bitCount, int accessPassword)
			throws ModuleException {
		// create EPC filter
		Filter filter = createEpcFilter(epc);
		List<TagOperation> tagOperations = new ArrayList<>();
		// create ReadOperation for password
		CustomOperation op = new CustomOperation();
		op.setData(data);
		op.setLength(bitCount);
		op.setPassword(accessPassword);
		op.setOperationId("g01");
		tagOperations.add(op);
		// read TID bank for ETB transponders
		if (tidLength > 0) {

			ReadOperation readEpcBank = new ReadOperation();
			readEpcBank.setBank((short) 1); // EPC
			readEpcBank.setOffset((short) 32);
			readEpcBank.setLength((short) (epc.length * 8));
			readEpcBank.setPassword(accessPassword);
			readEpcBank.setOperationId("g02");
			tagOperations.add(readEpcBank);

			ReadOperation readTidBank = new ReadOperation();
			readTidBank.setBank((short) 2); // TID
			readTidBank.setOffset((short) 0);
			readTidBank.setLength((short) 0); // whole bank
			readTidBank.setPassword(accessPassword);
			readTidBank.setOperationId("g01");
			tagOperations.add(readTidBank);
		}
		List<TagData> result = null;
		try {
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Sending CUSTOM_OPERATION");
			}
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE,
						"  antennaId=" + antennaId + ", epc=" + Arrays.toString(epc) + ", data=" + Arrays.toString(data)
								+ "bitCount=" + bitCount + ", accessPassword=" + (accessPassword == 0 ? "0" : "***"));
			}
			result = service.execute(Arrays.asList(new Short[] { antennaId }), Arrays.asList(new Filter[] { filter }),
					tagOperations);
		} catch (Exception e) {
			throw new ModuleException("Cannot execute custom command", e);
		}
		if (result == null || result.isEmpty() || result.get(0).getResultList() == null
				|| result.get(0).getResultList().isEmpty()) {
			throw new ModuleException("Cannot execute custom command: Missing result");
		}
		CustomResult r = (CustomResult) result.get(0).getResultList().get(0);
		if (r.getResult() != CustomResult.Result.SUCCESS) {
			throw new RfModuleException("Cannot execute custom command: " + r.getResult(),
					getErrorCode(r.getResult(), RfErrorCode.NON_SPECIFIC_READER_ERROR));
		}
		return r.getResultData();
	}

	private RfErrorCode getErrorCode(CustomResult.Result result, RfErrorCode dflt) {
		switch (result) {
		case INCORRECT_PASSWORD_ERROR:
			return RfErrorCode.INCORRECT_PASSWORD;
		case INSUFFICIENT_POWER:
			return RfErrorCode.INSUFFICIENT_POWER;
		case MEMORY_LOCKED_ERROR:
			return RfErrorCode.TAG_MEMORY_LOCKED;
		case MEMORY_OVERRUN_ERROR:
			return RfErrorCode.TAG_MEMORY_OVERRUN;
		case NON_SPECIFIC_READER_ERROR:
			return RfErrorCode.NON_SPECIFIC_READER_ERROR;
		case NON_SPECIFIC_TAG_ERROR:
			return RfErrorCode.NON_SPECIFIC_TAG_ERROR;
		case NO_RESPONSE_FROM_TAG:
			return RfErrorCode.NO_RESPONSE_FROM_TAG;
		case OP_NOT_POSSIBLE_ERROR:
			return RfErrorCode.NON_SPECIFIC_READER_ERROR;
		case SUCCESS:
			return RfErrorCode.NONE;
		}
		return dflt;
	}

	private Capabilities getCapabilities(CapabilityType type) throws ModuleException {
		List<Capabilities> caps = null;
		try {
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Sending GET_" + type);
			}
			caps = service.getCapabilities(type);
		} catch (Exception e) {
			throw new ModuleException("Cannot get capabilities of type " + type, e);
		}
		if (caps == null || caps.isEmpty()) {
			throw new ModuleException("Cannot get capabilities of type " + type + ": Missing result");
		}
		return caps.get(0);
	}

	private Configuration getConfiguration(ConfigurationType type, short antennaId) throws ModuleException {
		List<Configuration> confs = null;
		try {
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Sending GET_" + type);
			}
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "  antennaId=" + antennaId);
			}
			confs = service.getConfiguration(type, antennaId, (short) 0 /* gpiPort */, (short) 0 /* gpoPort */);
		} catch (Exception e) {
			throw new ModuleException("Cannot get configuration of type " + type + " for antenna " + antennaId, e);
		}
		if (confs == null || confs.isEmpty()) {
			throw new ModuleException(
					"Cannot get configuration of type " + type + " for antenna " + antennaId + ": Missing result");
		}
		return confs.get(0);
	}

	private Short getTransmitPower(short antennaId) throws ModuleException {
		AntennaConfiguration antennaConf = (AntennaConfiguration) getConfiguration(
				ConfigurationType.ANTENNA_CONFIGURATION, antennaId);
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Received " + ConfigurationType.ANTENNA_CONFIGURATION);
		}
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  transmitPowerIndex=" + antennaConf.getTransmitPower());
		}
		// if a transmit power index exists
		if (antennaConf.getTransmitPower() != null) {
			// get transmit power table
			RegulatoryCapabilities regulatoryCaps = (RegulatoryCapabilities) getCapabilities(
					CapabilityType.REGULATORY_CAPABILITIES);
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Received " + CapabilityType.REGULATORY_CAPABILITIES);
			}
			// for each entry
			for (TransmitPowerTableEntry entry : regulatoryCaps.getTransmitPowerTable().getEntryList()) {
				// if entry has the transmit power index
				if (entry.getIndex() == antennaConf.getTransmitPower()) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "  transmitPowerIndex=" + entry.getIndex() + ", transmitPower="
								+ entry.getTransmitPower());
					}
					// return the transmit power (in dBm)
					return entry.getTransmitPower();
				}
			}
			throw new ModuleException("Cannot get transmit power for index " + antennaConf.getTransmitPower()
					+ " and antenna " + antennaId);
		}
		return null;
	}

	private void setTransmitPower(short antennaId, short value) throws ModuleException {
		// get transmit power table
		RegulatoryCapabilities regulatoryCaps = (RegulatoryCapabilities) getCapabilities(
				CapabilityType.REGULATORY_CAPABILITIES);
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Received " + CapabilityType.REGULATORY_CAPABILITIES);
		}
		// for each entry
		for (TransmitPowerTableEntry entry : regulatoryCaps.getTransmitPowerTable().getEntryList()) {
			// if entry has the transmit power
			if (entry.getTransmitPower() == value) {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE,
							"  transmitPowerIndex=" + entry.getIndex() + ", transmitPower=" + entry.getTransmitPower());
				}
				// set transmit power index
				setAntennaConfiguration(antennaId, entry.getIndex());
				return;
			}
		}
		throw new ModuleException("Cannot get index for transmit power " + value + " and antenna " + antennaId);
	}

	private void setAntennaProperties(short antennaId, boolean isConnected) throws ModuleException {
		// get properties due to mandatory value "gain"
		AntennaProperties props = (AntennaProperties) getConfiguration(ConfigurationType.ANTENNA_PROPERTIES, antennaId);
		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Received " + ConfigurationType.ANTENNA_PROPERTIES);
		}
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "  isConnected=" + props.isConnected());
		}
		if (props.isConnected() != isConnected) {
			try {
				props.setConnected(isConnected);
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Sending SET_" + ConfigurationType.ANTENNA_PROPERTIES);
				}
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "  antennaId=" + antennaId + ", isConnected=" + isConnected);
				}
				service.setConfiguration(Arrays.asList((Configuration) props));
			} catch (Exception e) {
				throw new ModuleException("Cannot set connection state to " + isConnected + " for antenna " + antennaId,
						e);
			}
		}
	}

	private void setAntennaConfiguration(short antennaId, short transmitPowerIndex) throws ModuleException {
		try {
			AntennaConfiguration conf = new AntennaConfiguration();
			conf.setId(antennaId);
			conf.setTransmitPower(transmitPowerIndex);
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Sending SET_" + ConfigurationType.ANTENNA_CONFIGURATION);
			}
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "  antennaId=" + antennaId + ", transmitPowerIndex=" + transmitPowerIndex);
			}
			service.setConfiguration(Arrays.asList((Configuration) conf));
		} catch (Exception e) {
			throw new ModuleException("Cannot set transmit power for antenna 1", e);
		}
	}

	private short[] ushorts2shorts(int[] value) {
		short[] ret = new short[value.length];
		for (int i = 0; i < value.length; i++) {
			ret[i] = (short) value[i];
		}
		return ret;
	}

	private byte[] shorts2bytes(short[] value) {
		byte[] ret = new byte[value.length * 2];
		for (int i = 0; i < value.length; i++) {
			ret[i * 2] = (byte) (value[i] >> 8);
			ret[i * 2 + 1] = (byte) value[i];
		}
		return ret;
	}

	private String getProperty(String propertyName, String dfltValue) {
		String propertyValue = System.getProperty(propertyName);
		if (propertyValue != null) {
			propertyValue = propertyValue.trim();
		}
		if (propertyValue == null || propertyValue.isEmpty()) {
			propertyValue = System.getenv(propertyName);
			if (propertyValue != null) {
				propertyValue = propertyValue.trim();
			}
		}
		return propertyValue == null || propertyValue.isEmpty() ? dfltValue : propertyValue;
	}

	private short[] bigInt2shorts(BigInteger value, int returnArraySize) {
		short[] ret = new short[returnArraySize];
		int destTypeSize = 2; // short
		byte[] bytes = value.toByteArray();
		int destTypeCount = bytes.length / destTypeSize;
		if (bytes.length % destTypeSize > 0) {
			destTypeCount++;
		}
		if (destTypeCount > 0) {
			int retIndex = returnArraySize - destTypeCount;
			if (retIndex < 0) {
				throw new NumberFormatException(
						"Value '" + value + "' cannot be stored in " + returnArraySize + " short values");
			}
			int bytesStartIndex = (bytes.length % destTypeSize == 0) ? 0 : (bytes.length % destTypeSize) - destTypeSize;
			for (int i = bytesStartIndex; i < bytes.length; i += destTypeSize) {
				for (int j = 0; j < destTypeSize; j++) {
					if (i + j >= 0) {
						ret[retIndex] |= (bytes[i + j] & 0xFF) << ((destTypeSize - 1 - j) * 8);
					}
				}
				retIndex++;
			}
		}
		return ret;
	}
}
