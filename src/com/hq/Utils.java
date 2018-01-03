package com.hq;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Utils {

    /**
     * get a short value from a byte array
     * 
     * @param bytes
     *            the byte array
     * @return the short (16-bit) value
     */
    public static short getShort( byte[] bytes )
    {
        //return (short) ( ( data[1] & 0xFF << 8 ) + ( data[0] & 0xFF ) );
        return (short) (bytes[1] << 8 & 0xff00 | bytes[0] & 0xFF);
    }

    /**
     * get an int value from a byte array
     * 
     * @param bytes
     *            the byte array
     * @return the int (32-bit) value
     */
    public static int getInt( byte[] bytes )
    {
        return bytes[3]
                << 24 & 0xff000000
                | bytes[2]
                << 16 & 0xff0000
                | bytes[1]
                << 8 & 0xff00
                | bytes[0] & 0xFF;
    }
    

    /**
     * Convert Chars (16-bit) to String. Terminated by 0x00 and Padding byte 0.
     * @param charBuf
     * @return
     * @throws IOException
     */
    public static String getString(byte[] charBuf) throws IOException
    {
        StringBuilder strBuf = new StringBuilder();
        byte[] buf_2 = new byte[2];
        ByteArrayInputStream in = new ByteArrayInputStream(charBuf);

        while(in.read(buf_2) != -1){
            int code = getShort(buf_2);
            if(code == 0x00) // End of String
                break;
            else
                strBuf.append((char) code);
        }
        return strBuf.toString();
    }
}
