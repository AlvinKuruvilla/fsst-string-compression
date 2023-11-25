package fsst;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
    static int boolToInt(boolean value) {
        return value ? 1 : 0;
    }

    public static long fsst_unaligned_load(byte[] v, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(v, offset, 8);
        buffer.order(ByteOrder.nativeOrder()); // Assuming you want native byte order
        return buffer.getLong();
    }

    public static void fsst_unaligned_store(byte[] dst, int offset, long src) {
        byte[] srcBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(src).array();
        System.arraycopy(srcBytes, 0, dst, offset, 8);
    }

}
