package okio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;

public class ByteString implements Serializable, Comparable<ByteString> {
    public static final ByteString EMPTY = m64of(new byte[0]);
    static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final long serialVersionUID = 1;
    final byte[] data;
    transient int hashCode;
    transient String utf8;

    ByteString(byte[] bArr) {
        this.data = bArr;
    }

    /* renamed from: of */
    public static ByteString m64of(byte... bArr) {
        if (bArr != null) {
            return new ByteString((byte[]) bArr.clone());
        }
        throw new IllegalArgumentException("data == null");
    }

    public static ByteString encodeUtf8(String str) {
        if (str != null) {
            ByteString byteString = new ByteString(str.getBytes(Util.UTF_8));
            byteString.utf8 = str;
            return byteString;
        }
        throw new IllegalArgumentException("s == null");
    }

    public String utf8() {
        String str = this.utf8;
        if (str != null) {
            return str;
        }
        String str2 = new String(this.data, Util.UTF_8);
        this.utf8 = str2;
        return str2;
    }

    public String hex() {
        byte[] bArr = this.data;
        char[] cArr = new char[(bArr.length * 2)];
        int i = 0;
        for (byte b : bArr) {
            int i2 = i + 1;
            char[] cArr2 = HEX_DIGITS;
            cArr[i] = cArr2[(b >> 4) & 15];
            i = i2 + 1;
            cArr[i2] = cArr2[b & 15];
        }
        return new String(cArr);
    }

    public static ByteString read(InputStream inputStream, int i) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("in == null");
        } else if (i >= 0) {
            byte[] bArr = new byte[i];
            int i2 = 0;
            while (i2 < i) {
                int read = inputStream.read(bArr, i2, i - i2);
                if (read != -1) {
                    i2 += read;
                } else {
                    throw new EOFException();
                }
            }
            return new ByteString(bArr);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("byteCount < 0: ");
            sb.append(i);
            throw new IllegalArgumentException(sb.toString());
        }
    }

    public ByteString substring(int i, int i2) {
        if (i >= 0) {
            byte[] bArr = this.data;
            if (i2 <= bArr.length) {
                int i3 = i2 - i;
                if (i3 < 0) {
                    throw new IllegalArgumentException("endIndex < beginIndex");
                } else if (i == 0 && i2 == bArr.length) {
                    return this;
                } else {
                    byte[] bArr2 = new byte[i3];
                    System.arraycopy(this.data, i, bArr2, 0, i3);
                    return new ByteString(bArr2);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("endIndex > length(");
                sb.append(this.data.length);
                sb.append(")");
                throw new IllegalArgumentException(sb.toString());
            }
        } else {
            throw new IllegalArgumentException("beginIndex < 0");
        }
    }

    public byte getByte(int i) {
        return this.data[i];
    }

    public int size() {
        return this.data.length;
    }

    /* access modifiers changed from: 0000 */
    public byte[] internalArray() {
        return this.data;
    }

    public boolean rangeEquals(int i, ByteString byteString, int i2, int i3) {
        return byteString.rangeEquals(i2, this.data, i, i3);
    }

    public boolean rangeEquals(int i, byte[] bArr, int i2, int i3) {
        if (i >= 0) {
            byte[] bArr2 = this.data;
            if (i <= bArr2.length - i3 && i2 >= 0 && i2 <= bArr.length - i3 && Util.arrayRangeEquals(bArr2, i, bArr, i2, i3)) {
                return true;
            }
        }
        return false;
    }

    public final boolean startsWith(ByteString byteString) {
        return rangeEquals(0, byteString, 0, byteString.size());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0019, code lost:
        if (r5.rangeEquals(0, r4, 0, r4.length) != false) goto L_0x001d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(java.lang.Object r5) {
        /*
            r4 = this;
            r0 = 1
            if (r5 != r4) goto L_0x0004
            return r0
        L_0x0004:
            boolean r1 = r5 instanceof okio.ByteString
            r2 = 0
            if (r1 == 0) goto L_0x001c
            okio.ByteString r5 = (okio.ByteString) r5
            int r1 = r5.size()
            byte[] r4 = r4.data
            int r3 = r4.length
            if (r1 != r3) goto L_0x001c
            int r1 = r4.length
            boolean r4 = r5.rangeEquals(r2, r4, r2, r1)
            if (r4 == 0) goto L_0x001c
            goto L_0x001d
        L_0x001c:
            r0 = r2
        L_0x001d:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: okio.ByteString.equals(java.lang.Object):boolean");
    }

    public int hashCode() {
        int i = this.hashCode;
        if (i != 0) {
            return i;
        }
        int hashCode2 = Arrays.hashCode(this.data);
        this.hashCode = hashCode2;
        return hashCode2;
    }

    public int compareTo(ByteString byteString) {
        int size = size();
        int size2 = byteString.size();
        int min = Math.min(size, size2);
        int i = 0;
        while (true) {
            int i2 = -1;
            if (i < min) {
                byte b = getByte(i) & 255;
                byte b2 = byteString.getByte(i) & 255;
                if (b == b2) {
                    i++;
                } else {
                    if (b >= b2) {
                        i2 = 1;
                    }
                    return i2;
                }
            } else if (size == size2) {
                return 0;
            } else {
                if (size >= size2) {
                    i2 = 1;
                }
                return i2;
            }
        }
    }

    public String toString() {
        String str;
        String str2;
        if (this.data.length == 0) {
            return "[size=0]";
        }
        String utf82 = utf8();
        int codePointIndexToCharIndex = codePointIndexToCharIndex(utf82, 64);
        String str3 = "…]";
        String str4 = "[size=";
        String str5 = "]";
        if (codePointIndexToCharIndex == -1) {
            if (this.data.length <= 64) {
                StringBuilder sb = new StringBuilder();
                sb.append("[hex=");
                sb.append(hex());
                sb.append(str5);
                str2 = sb.toString();
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str4);
                sb2.append(this.data.length);
                sb2.append(" hex=");
                sb2.append(substring(0, 64).hex());
                sb2.append(str3);
                str2 = sb2.toString();
            }
            return str2;
        }
        String replace = utf82.substring(0, codePointIndexToCharIndex).replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
        if (codePointIndexToCharIndex < utf82.length()) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(str4);
            sb3.append(this.data.length);
            sb3.append(" text=");
            sb3.append(replace);
            sb3.append(str3);
            str = sb3.toString();
        } else {
            StringBuilder sb4 = new StringBuilder();
            sb4.append("[text=");
            sb4.append(replace);
            sb4.append(str5);
            str = sb4.toString();
        }
        return str;
    }

    static int codePointIndexToCharIndex(String str, int i) {
        int length = str.length();
        int i2 = 0;
        int i3 = 0;
        while (i2 < length) {
            if (i3 == i) {
                return i2;
            }
            int codePointAt = str.codePointAt(i2);
            if ((Character.isISOControl(codePointAt) && codePointAt != 10 && codePointAt != 13) || codePointAt == 65533) {
                return -1;
            }
            i3++;
            i2 += Character.charCount(codePointAt);
        }
        return str.length();
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        ByteString read = read(objectInputStream, objectInputStream.readInt());
        try {
            Field declaredField = ByteString.class.getDeclaredField("data");
            declaredField.setAccessible(true);
            declaredField.set(this, read.data);
        } catch (NoSuchFieldException unused) {
            throw new AssertionError();
        } catch (IllegalAccessException unused2) {
            throw new AssertionError();
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeInt(this.data.length);
        objectOutputStream.write(this.data);
    }
}
