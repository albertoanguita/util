package jacz.util.numeric.test;

import jacz.util.numeric.NumericUtil;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 28/04/12<br>
 * Last Modified: 28/04/12
 */
public class TestDecimalToBitString {

    public static void main(String args[]) {
        String s = NumericUtil.decimalToBitString(24, 0, 24);

        System.out.println(s);
    }
}
