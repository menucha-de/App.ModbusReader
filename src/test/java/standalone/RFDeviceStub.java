package standalone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import havis.device.rf.RFConsumer;
import havis.device.rf.capabilities.AntennaReceiveSensitivityRangeTable;
import havis.device.rf.capabilities.AntennaReceiveSensitivityRangeTableEntry;
import havis.device.rf.capabilities.Capabilities;
import havis.device.rf.capabilities.CapabilityType;
import havis.device.rf.capabilities.DeviceCapabilities;
import havis.device.rf.capabilities.FixedFreqTable;
import havis.device.rf.capabilities.FreqHopTable;
import havis.device.rf.capabilities.ReceiveSensitivityTable;
import havis.device.rf.capabilities.ReceiveSensitivityTableEntry;
import havis.device.rf.capabilities.RegulatoryCapabilities;
import havis.device.rf.capabilities.TransmitPowerTable;
import havis.device.rf.capabilities.TransmitPowerTableEntry;
import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.AntennaProperties;
import havis.device.rf.configuration.Configuration;
import havis.device.rf.configuration.ConfigurationType;
import havis.device.rf.exception.ConnectionException;
import havis.device.rf.exception.ImplementationException;
import havis.device.rf.exception.ParameterException;
import havis.device.rf.tag.Filter;
import havis.device.rf.tag.TagData;
import havis.device.rf.tag.operation.CustomOperation;
import havis.device.rf.tag.operation.ReadOperation;
import havis.device.rf.tag.operation.TagOperation;
import havis.device.rf.tag.operation.WriteOperation;
import havis.device.rf.tag.result.CustomResult;
import havis.device.rf.tag.result.OperationResult;
import havis.device.rf.tag.result.ReadResult;
import havis.device.rf.tag.result.WriteResult;

public class RFDeviceStub implements havis.device.rf.RFDevice {

    private Logger log = Logger.getLogger(RFDeviceStub.class.getName());

    private final long GET_CAPABILITIES_DELAY = 20; // ms
    private final long GET_CONFIGURATION_DELAY = 15; // ms
    private final long EXECUTE_DELAY = 2; // ms

    class Tag {
        TagData tagData;
        byte[] pwdBank;
        byte[] tidBank;
        byte[] userBank;
    }

    private List<AntennaProperties> antennaProperties = new ArrayList<>();
    private List<AntennaConfiguration> antennaConfigurations = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    public RFDeviceStub() {
        AntennaProperties props = new AntennaProperties();
        props.setId((short) 1);
        props.setConnected(true);
        antennaProperties.add(props);
        props = new AntennaProperties();
        props.setId((short) 2);
        props.setConnected(false);
        antennaProperties.add(props);

        AntennaConfiguration conf = new AntennaConfiguration();
        conf.setId((short) 1);
        conf.setTransmitPower((short) 9);
        antennaConfigurations.add(conf);
        conf = new AntennaConfiguration();
        conf.setId((short) 2);
        conf.setTransmitPower((short) 10);
        antennaConfigurations.add(conf);

        Tag tag = new Tag();
        tag.tagData = new TagData();
        tag.tagData.setTagDataId(25);
        tag.tagData.setCrc((short) 0x8000); // 32768 -> -32768
        // 3611 (EPCLength: 6 (5 bits), UserMemoryIndicator: 1 (1 bit),
        // XPCIndicator: 1 (1 bit), Toggle: 0 (1 bit, 0: bank contains EPC),
        // AFI: 0x11)
        tag.tagData.setPc((short) 0x3611); // 13841
        // 300D964A3120004000000001
        tag.tagData.setEpc(new byte[] { (byte) 0x30, (byte) 0x0D, (byte) 0x96, (byte) 0x4A,
                        (byte) 0x31, (byte) 0x20, (byte) 0x00, (byte) 0x40, (byte) 0x00,
                        (byte) 0x00, (byte) 0x00, (byte) 0x01 });
        // 87654321
        tag.tagData.setXpc(0x8765_4321); // 0x8765: 34661 -> -30875, 0x4321: 17185
        tag.tagData.setAntennaID((short) 1);
        tag.tagData.setRssi((byte) -10);
        tag.tagData.setChannel((short) 1);
        tag.pwdBank = new byte[] { (byte) 0xAF, (byte) 0xFE, (byte) 0x00, (byte) 0x01, (byte) 0x12,
                        (byte) 0x34, (byte) 0x00, (byte) 0x01 };
        tag.tidBank = new byte[] { (byte) 0xE2, (byte) 0x11, (byte) 0x10, (byte) 0x03 };
        tag.userBank = new byte[] { (byte) 0x98, (byte) 0x76, (byte) 0x54, (byte) 0 };
        tags.add(tag);
    }

    @Override
    public void openConnection(RFConsumer consumer, int timeout)
                    throws ConnectionException, ImplementationException {
    }

    @Override
    public void closeConnection() {
    }

    @Override
    public List<Capabilities> getCapabilities(CapabilityType type) {
        try {
            Thread.sleep(GET_CAPABILITIES_DELAY);
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Sleeping in 'getCapabilities' was interrupted", e);
        }

        List<Capabilities> ret = new ArrayList<>();
        switch (type) {
        case ALL:
        case DEVICE_CAPABILITIES:
            DeviceCapabilities devCaps = new DeviceCapabilities();

            ReceiveSensitivityTable receiveSensitivityTable = new ReceiveSensitivityTable();
            ReceiveSensitivityTableEntry receiveSensitivityTableEntry = new ReceiveSensitivityTableEntry();
            receiveSensitivityTableEntry.setIndex((short) 1);
            receiveSensitivityTableEntry.setReceiveSensitivity((short) 125);
            receiveSensitivityTable.getEntryList().add(receiveSensitivityTableEntry);
            devCaps.setReceiveSensitivityTable(receiveSensitivityTable);

            AntennaReceiveSensitivityRangeTable antennaReceiveSensitivityRangeTable = new AntennaReceiveSensitivityRangeTable();
            AntennaReceiveSensitivityRangeTableEntry antennaReceiveSensitivityRangeTableEntry = new AntennaReceiveSensitivityRangeTableEntry();
            antennaReceiveSensitivityRangeTable.getEntryList()
                            .add(antennaReceiveSensitivityRangeTableEntry);
            devCaps.setAntennaReceiveSensitivityRangeTable(antennaReceiveSensitivityRangeTable);

            devCaps.setFirmware("0.0.9");
            devCaps.setManufacturer((short) 1);
            devCaps.setModel((short) 2);
            devCaps.setMaxReceiveSensitivity((short) 1);
            devCaps.setNumberOfAntennas((short) 1);
            ret.add(devCaps);
            break;
        default:
        }
        switch (type) {
        case ALL:
        case REGULATORY_CAPABILITIES:
            RegulatoryCapabilities regCaps = new RegulatoryCapabilities();
            regCaps.setCommunicationStandard((short) 2);
            regCaps.setCountryCode((short) 276);
            regCaps.setHopping(false);
            regCaps.setFixedFreqTable(new FixedFreqTable());
            regCaps.setFreqHopTable(new FreqHopTable());
            TransmitPowerTable tpt = new TransmitPowerTable();
            List<TransmitPowerTableEntry> tptEntries = new ArrayList<>();
            TransmitPowerTableEntry tptEntry = new TransmitPowerTableEntry();
            tptEntry.setIndex((short) 9);
            tptEntry.setTransmitPower((short) 18);
            tptEntries.add(tptEntry);
            tptEntry = new TransmitPowerTableEntry();
            tptEntry.setIndex((short) 10);
            tptEntry.setTransmitPower((short) 20);
            tptEntries.add(tptEntry);
            tpt.setEntryList(tptEntries);
            regCaps.setTransmitPowerTable(tpt);
            ret.add(regCaps);
            break;
        default:
        }
        return ret;
    }

    @Override
    public List<Configuration> getConfiguration(ConfigurationType type, short antennaID,
                    short gpiPort, short gpoPort) {
        try {
            Thread.sleep(GET_CONFIGURATION_DELAY);
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "Sleeping in 'getConfiguration' was interrupted", e);
        }
        List<Configuration> ret = new ArrayList<>();
        switch (type) {
        case ALL:
        case ANTENNA_PROPERTIES:
            if (antennaID == 0) {
                ret.addAll(antennaProperties);
            } else {
                ret.add(antennaProperties.get(antennaID - 1));
            }
            break;
        default:
        }
        switch (type) {
        case ALL:
        case ANTENNA_CONFIGURATION:
            if (antennaID == 0) {
                ret.addAll(antennaConfigurations);
            } else {
                ret.add(antennaConfigurations.get(antennaID - 1));
            }
            break;
        default:
        }
        return ret;
    }

    @Override
    public void setConfiguration(List<Configuration> configuration) throws ImplementationException {
        for (Configuration conf : configuration) {
            if (conf instanceof AntennaProperties) {
                AntennaProperties props = (AntennaProperties) conf;
                if (props.getId() == 0) {
                    for (int i = 0; i < antennaProperties.size(); i++) {

                        antennaProperties.set(i, props);
                    }
                } else {
                    antennaProperties.set(props.getId() - 1, props);
                }
            } else {
                AntennaConfiguration aconf = (AntennaConfiguration) conf;
                if (aconf.getId() == 0) {
                    for (int i = 0; i < antennaConfigurations.size(); i++) {
                        antennaConfigurations.set(i, aconf);
                    }
                } else {
                    antennaConfigurations.set(aconf.getId() - 1, aconf);
                }
            }
        }
    }

    @Override
    public void resetConfiguration() throws ImplementationException {
    }

    @Override
    public List<TagData> execute(List<Short> antennas, List<Filter> filter,
                    List<TagOperation> operations)
                    throws ParameterException, ImplementationException {
        try {
            Thread.sleep(EXECUTE_DELAY);
        } catch (InterruptedException e) {
            throw new ImplementationException("Sleeping in 'execute' was interrupted", e);
        }
        Tag tag = tags.get(0);
        List<OperationResult> opResults = new ArrayList<>();
        for (TagOperation op : operations) {
            if (op instanceof ReadOperation) {
                ReadOperation ro = (ReadOperation) op;
                // create result data
                byte[] resultData = null;
                switch (ro.getBank()) {
                case 0:
                    // killPwd: 0xAFFE0001, accessPwd: 0x12340001
                    resultData = getWords(tag.pwdBank, ro.getOffset(), ro.getLength());
                    break;
                case 2: // TID
                    // E2111003
                    resultData = getWords(tag.tidBank, ro.getOffset(), ro.getLength());
                    break;
                case 3: // user
                    // 987654
                    resultData = getWords(tag.userBank, ro.getOffset(), ro.getLength());
                    break;
                }
                // create result
                ReadResult result = new ReadResult();
                result.setOperationId(op.getOperationId());
                result.setReadData(resultData);
                result.setResult(ReadResult.Result.SUCCESS);
                opResults.add(result);
            } else if (op instanceof WriteOperation) {
                WriteOperation wo = (WriteOperation) op;
                // write data
                switch (wo.getBank()) {
                case 0:
                    setWords(tag.pwdBank, wo.getOffset(), wo.getData());
                    break;
                case 1: // EPC
                    switch (wo.getOffset()) {
                    case 0:
                        tag.tagData.setCrc((short) (wo.getData()[0] << 16 | wo.getData()[1]));
                        break;
                    case 1:
                        tag.tagData.setPc((short) (wo.getData()[0] << 16 | wo.getData()[1]));
                        break;
                    case 2:
                        tag.tagData.setEpc(wo.getData());
                        break;
                    }
                    break;
                case 2: // TID
                    setWords(tag.tidBank, wo.getOffset(), wo.getData());
                    break;
                case 3: // user
                    setWords(tag.userBank, wo.getOffset(), wo.getData());
                    break;
                }
                // create result
                WriteResult result = new WriteResult();
                result.setOperationId(op.getOperationId());
                short length = (short) (wo.getData().length / 2);
                if (wo.getData().length % 2 == 1) {
                    length++;
                }
                result.setWordsWritten(length);
                result.setResult(WriteResult.Result.SUCCESS);
                opResults.add(result);
            } else if (op instanceof CustomOperation) {
                CustomOperation co = (CustomOperation) op;
                int byteCount = co.getLength() / 8;
                if (co.getLength() % 8 > 0) {
                    byteCount++;
                }
                if (byteCount > co.getData().length) {
                    byteCount = co.getData().length;
                }
                // create result data
                byte[] resultData = null;
                if (co.getData() == null) {
                    resultData = new byte[0];
                } else {
                    resultData = new byte[byteCount];
                    for (int i = 0; i < byteCount; i++) {
                        resultData[i] = (byte) (co.getData()[i] + 1);
                    }
                }
                // create result
                CustomResult result = new CustomResult();
                result.setOperationId(co.getOperationId());
                result.setResultData(resultData);
                result.setResult(CustomResult.Result.SUCCESS);
                opResults.add(result);
            }
        }
        tag.tagData.setResultList(opResults);
        return Arrays.asList(tag.tagData);
    }

    @Override
    public List<String> getSupportedRegions() {
        return null;
    }

    @Override
    public String getRegion() throws ConnectionException {
        return null;
    }

    @Override
    public void setRegion(String id) throws ParameterException, ImplementationException {
    }

    @Override
    public void installFirmware() throws ImplementationException {
    }

    private byte[] getWords(byte[] bytes, int wordOffset, int wordLength) {
        int offset = 2 * wordOffset;
        int length = 2 * wordLength;
        if (length == 0) {
            length = bytes.length - offset;
            if (length % 2 == 1) {
                length += 1;
            }
        }
        return Arrays.copyOfRange(bytes, offset, offset + length);
    }

    private void setWords(byte[] bytes, int wordOffset, byte[] data) {
        int offset = 2 * wordOffset;
        for (int i = 0; i < data.length; i++) {
            if (offset >= bytes.length) {
                return;
            }
            bytes[offset++] = data[i];
        }
        if (data.length % 2 == 1 && offset < bytes.length) {
            bytes[offset] = 0;
        }
    }
}
