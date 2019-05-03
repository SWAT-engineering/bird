package engineering.swat.nest.core.bytes;

public class ByteUtils {

    public static void reverseBytes(byte[] bytes) {
        switch (bytes.length) {
            case 1: return;
            case 2: {
                byte tmp = bytes[0];
                bytes[0] = bytes[1];
                bytes[1] = tmp;
                break;
            }
            case 3: {
                byte tmp = bytes[0];
                bytes[0] = bytes[2];
                bytes[2] = tmp;
                break;
            }
            case 4: {
                byte tmp = bytes[0];
                bytes[0] = bytes[3];
                bytes[3] = tmp;
                tmp = bytes[1];
                bytes[1] = bytes[2];
                bytes[2] = tmp;
                break;
            }
            default:
                for (int i = 0; i < bytes.length / 2; i++) {
                    byte temp = bytes[i];
                    bytes[i] = bytes[bytes.length - i - 1];
                    bytes[bytes.length - i - 1] = temp;
                }
        }
    }

    public static byte[] copyReverse(byte[] bytes) {
        byte[] copy = new byte[bytes.length];
        switch (bytes.length) {
            case 1:
                copy[0] = bytes[0];
                break;
            case 2:
                copy[0] = bytes[1];
                copy[1] = bytes[0];
                break;
            case 3:
                copy[0] = bytes[2];
                copy[1] = bytes[1];
                copy[2] = bytes[0];
                break;
            case 4:
                copy[0] = bytes[3];
                copy[1] = bytes[2];
                copy[2] = bytes[1];
                copy[3] = bytes[0];
                break;
            default:
                for (int i = 0; i < bytes.length / 2; i++) {
                    copy[i] = bytes[bytes.length - i - 1];
                    copy[copy.length - i - 1] = bytes[i];
                }
                if (bytes.length % 2 == 1) {
                    // copy middle element
                    copy[bytes.length / 2] = bytes[bytes.length / 2];
                }
                break;
        }
        return copy;
    }
}
