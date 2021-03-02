package havis.custom.harting.modbus.reader.api;

import java.util.Arrays;

import havis.custom.harting.modbus.reader.core.FieldProperties.FieldDataType;

public class FieldValue {
    private boolean[] booleanValue;
    private byte[] byteValue;
    private short[] shortValue;
    private int[] ushortValue;
    private float[] floatValue;
    private String[] stringValue;

    private FieldDataType dataType;

    public FieldValue() {
    }

    public FieldValue(boolean[] booleanValue) {
        setBooleanValue(booleanValue);
    }

    public FieldValue(byte[] byteValue) {
        setByteValue(byteValue);
    }

    public FieldValue(short[] shortValue) {
        setShortValue(shortValue);
    }

    public FieldValue(int[] ushortValue) {
        setUShortValue(ushortValue);
    }

    public FieldValue(float[] floatValue) {
        setFloatValue(floatValue);
    }

    public FieldValue(String[] stringValue) {
        setStringValue(stringValue);
    }

    public FieldDataType getDataType() {
        return dataType;
    }

    public boolean[] getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean[] booleanValue) {
        dataType = FieldDataType.BOOLEAN;
        this.booleanValue = booleanValue;
    }

    public byte[] getByteValue() {
        return byteValue;
    }

    public void setByteValue(byte[] byteValue) {
        dataType = FieldDataType.BYTE;
        this.byteValue = byteValue;
    }

    public short[] getShortValue() {
        return shortValue;
    }

    public void setShortValue(short[] shortValue) {
        dataType = FieldDataType.SHORT;
        this.shortValue = shortValue;
    }

    public int[] getUShortValue() {
        return ushortValue;
    }

    public void setUShortValue(int[] ushortValue) {
        dataType = FieldDataType.USHORT;
        this.ushortValue = ushortValue;
    }

    public float[] getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(float[] floatValue) {
        dataType = FieldDataType.FLOAT;
        this.floatValue = floatValue;
    }

    public String[] getStringValue() {
        return stringValue;
    }

    public void setStringValue(String[] stringValue) {
        dataType = FieldDataType.STRING;
        this.stringValue = stringValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(booleanValue);
        result = prime * result + Arrays.hashCode(byteValue);
        result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
        result = prime * result + Arrays.hashCode(floatValue);
        result = prime * result + Arrays.hashCode(shortValue);
        result = prime * result + Arrays.hashCode(stringValue);
        result = prime * result + Arrays.hashCode(ushortValue);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FieldValue other = (FieldValue) obj;
        if (!Arrays.equals(booleanValue, other.booleanValue))
            return false;
        if (!Arrays.equals(byteValue, other.byteValue))
            return false;
        if (dataType != other.dataType)
            return false;
        if (!Arrays.equals(floatValue, other.floatValue))
            return false;
        if (!Arrays.equals(shortValue, other.shortValue))
            return false;
        if (!Arrays.equals(stringValue, other.stringValue))
            return false;
        if (!Arrays.equals(ushortValue, other.ushortValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (dataType == null) {
            return "FieldValue [dataType=null]";
        }
        StringBuffer ret = new StringBuffer("FieldValue [dataType=" + dataType + ", value=");
        switch (dataType) {
        case BOOLEAN:
            ret.append(Arrays.toString(booleanValue));
            break;
        case BYTE:
            ret.append(Arrays.toString(byteValue));
            break;
        case FLOAT:
            ret.append(Arrays.toString(floatValue));
            break;
        case SHORT:
            ret.append(Arrays.toString(shortValue));
            break;
        case STRING:
            ret.append(Arrays.toString(stringValue));
            break;
        case USHORT:
            ret.append(Arrays.toString(ushortValue));
            break;
        default:
            break;
        }
        ret.append("]");
        return ret.toString();
    }
}
