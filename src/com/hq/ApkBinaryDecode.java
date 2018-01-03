package com.hq;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author bilux (i.bilux@gmail.com)
 */
public class ApkBinaryDecode {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //String fileName = "/home/hq/temp/uni.apk";
        String fileName = args[0];
        try {
            if (fileName.endsWith(".apk")) {
                ApkTool.getPackage(fileName);
                System.out.println(ApkTool.xml);
            } else {
                System.out.println("Non valide file.");
            }
        } catch (IOException | ParserConfigurationException | SAXException ex) {
        }
    }
    
}
