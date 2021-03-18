package havis.app.modbus.reader.rf;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class DataTypeConverter {

	private DataTypeConverter() {
	}

	/**
	 * Interprets a signed byte value as an unsigned value.
	 * <p>
	 * The unsigned value can be converted back to the signed byte value by
	 * casting the unsigned value to a byte.
	 * </p>
	 * 
	 * @param v
	 * @return The number
	 */
	public static short ubyte(byte v) {
		return (short) (v & 0xFF);
	}

	/**
	 * Interprets a signed short value as an unsigned value.
	 * <p>
	 * The unsigned value can be converted back to the signed short value by
	 * casting the unsigned value to a short.
	 * </p>
	 * 
	 * @param v
	 * @return The number
	 */
	public static int ushort(short v) {
		return v & 0xFFFF;
	}

	/**
	 * Interprets an signed integer value as unsigned value.
	 * <p>
	 * The unsigned value can be converted back to the signed integer value by
	 * casting the unsigned value to an integer.
	 * </p>
	 * 
	 * @param v
	 * @return The number
	 */
	public static long uint(int v) {
		return v & 0xFFFFFFFFL;
	}

	/**
	 * Interprets a signed long value as unsigned value.
	 * <p>
	 * The unsigned value can be converted back to the signed long value by
	 * calling {@link BigInteger#longValue()}.
	 * </p>
	 * 
	 * @param v
	 * @return The number
	 */
	public static BigInteger ulong(long v) {
		byte[] bytes = ByteBuffer.allocate(8).putLong(v).array();
		return new BigInteger(1, bytes);
	}
}
