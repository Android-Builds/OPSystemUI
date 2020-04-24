package com.oneplus.lib.widget;

public class IntArray implements Cloneable {
    private int mSize;
    private int[] mValues;

    public IntArray() {
        this(10);
    }

    public IntArray(int i) {
        if (i == 0) {
            this.mValues = EmptyArray.INT;
        } else {
            this.mValues = ArrayUtils.newUnpaddedIntArray(i);
        }
        this.mSize = 0;
    }

    public void add(int i) {
        add(this.mSize, i);
    }

    public void add(int i, int i2) {
        if (i < 0 || i > this.mSize) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(1);
        int i3 = this.mSize;
        if (i3 - i != 0) {
            int[] iArr = this.mValues;
            System.arraycopy(iArr, i, iArr, i + 1, i3 - i);
        }
        this.mValues[i] = i2;
        this.mSize++;
    }

    private void ensureCapacity(int i) {
        int i2 = this.mSize;
        int i3 = i + i2;
        if (i3 >= this.mValues.length) {
            int i4 = (i2 < 6 ? 12 : i2 >> 1) + i2;
            if (i4 > i3) {
                i3 = i4;
            }
            int[] newUnpaddedIntArray = ArrayUtils.newUnpaddedIntArray(i3);
            System.arraycopy(this.mValues, 0, newUnpaddedIntArray, 0, i2);
            this.mValues = newUnpaddedIntArray;
        }
    }

    public void clear() {
        this.mSize = 0;
    }

    public IntArray clone() throws CloneNotSupportedException {
        IntArray intArray = (IntArray) super.clone();
        intArray.mValues = (int[]) this.mValues.clone();
        return intArray;
    }

    public int get(int i) {
        if (i < this.mSize) {
            return this.mValues[i];
        }
        StringBuilder sb = new StringBuilder();
        sb.append("index is:");
        sb.append(i);
        sb.append(" size is :");
        sb.append(this.mSize);
        throw new ArrayIndexOutOfBoundsException(sb.toString());
    }

    public int size() {
        return this.mSize;
    }
}
