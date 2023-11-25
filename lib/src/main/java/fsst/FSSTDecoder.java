package fsst;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FSSTDecoder {
    final long FSST_CORRUPT = 32774747032022883L;
    final long FSST_VERSION_20190218 = 20190218;
    final long FSST_VERSION = ((long) FSST_VERSION_20190218);

    long version;
    boolean zeroTerminated;
    char[] len = new char[255];
    long[] symbol = new long[256];

    FSSTDecoder() {
    }

    int import_from_buffer(byte[] buf) {
        long version = 0;
        int code, pos = 17;
        byte[] lenHisto = new byte[8];

        // Convert first 8 bytes of buf to a long (for the version)
        version = ByteBuffer.wrap(buf, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();

        if ((version >> 32) != FSST_VERSION)
            return 0;

        this.zeroTerminated = (buf[8] & 1) != 0;
        System.arraycopy(buf, 9, lenHisto, 0, 8);

        this.len[0] = 1;
        this.symbol[0] = 0;

        code = this.zeroTerminated ? 1 : 0;
        if (this.zeroTerminated)
            lenHisto[0]--;

        for (int l = 1; l <= 8; l++) {
            for (int i = 0; i < lenHisto[l & 7]; i++, code++) {
                this.len[code] = (char) ((l & 7) + 1);
                this.symbol[code] = 0;

                byte[] tmpBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(this.symbol[code])
                        .array();

                for (int j = 0; j < this.len[code]; j++)
                    tmpBuf[j] = buf[pos++];

                this.symbol[code] = ByteBuffer.wrap(tmpBuf).order(ByteOrder.LITTLE_ENDIAN).getLong();
            }
        }
        if (this.zeroTerminated)
            lenHisto[0]++;

        while (code < 255) {
            this.symbol[code] = FSST_CORRUPT;
            this.len[code++] = 8;
        }

        return pos;

    }

    public int fsst_decompress(int lenIn, byte[] strIn, int size, byte[] output) {
        int posIn = 0;
        int posOut = 0;

        while (posIn + 3 < lenIn) {
            int code, code0, code1;

            code = ((strIn[posIn] & 0xFF) |
                    ((strIn[posIn + 1] & 0xFF) << 8) |
                    ((strIn[posIn + 2] & 0xFF) << 16));

            code0 = code & 4095;
            code1 = (code >> 12) & 4095;
            posIn += 3;

            byte[] dst = new byte[size];
            int lim;

            lim = Math.min(posOut + this.len[code0], size);
            System.arraycopy(this.symbol, code0, dst, posOut, lim - posOut);
            posOut += this.len[code0];

            lim = Math.min(posOut + this.len[code1], size);
            System.arraycopy(this.symbol, code1, dst, posOut, lim - posOut);
            posOut += this.len[code1];
        }

        if (posIn < lenIn) {
            int code = ((strIn[posIn] & 0xFF) | ((strIn[posIn + 1] & 0xFF) << 8));
            code &= 4095;
            posIn = lenIn;
            int lim = Math.min(posOut + this.len[code], size);
            System.arraycopy(this.symbol, code, output, posOut, lim - posOut);
            posOut += this.len[code];
        }

        return posOut;
    }
}
