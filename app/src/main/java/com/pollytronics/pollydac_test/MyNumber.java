package com.pollytronics.pollydac_test;

/**
 * Created by pollywog on 9/26/15.
 */
public class MyNumber implements Comparable<MyNumber> {
    private int value;

    public MyNumber(int value) {
        this.value = value;
    }

    public int getValue() { return value; }

    public void setValue(int value) { this.value = value; }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public int compareTo(MyNumber another) {
        return this.getValue() - another.getValue();
    }

}
