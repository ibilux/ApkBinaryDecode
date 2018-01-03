package com.hq;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author bilux (i.bilux@gmail.com)
 */
public class ApkTool {

    static String xml;

    // get apk information such as app name, version and icon.
    public static void getPackage(String filePath) throws IOException, ParserConfigurationException, SAXException {
        try (ZipFile zip = new ZipFile(filePath)) {
            ZipEntry amz;
            Parser parser = new Parser();
            amz = zip.getEntry("resources.arsc");
            try (InputStream amis = zip.getInputStream(amz)) {
                int BUFFER_SIZE = (int) (amz.getSize() > 51200 ? 51200 : amz.getSize());
                byte[] buf = new byte[BUFFER_SIZE];
                int bytesRead = amis.read(buf);
                parser.parseResourceTable(buf);
                // 
                // ***********************************
                // TODO : decode resources table map to link attributes togother
                // ***********************************
                // 
                parser.getXmlStringPool();
                parser.getResourcesMap();
                parser.getResourcesPackageStringPool();
                parser.getResourcesTableStringPool();
                //
            }

            amz = zip.getEntry("AndroidManifest.xml");
            try (InputStream amis = zip.getInputStream(amz)) {
                int BUFFER_SIZE = (int) (amz.getSize() > 51200 ? 51200 : amz.getSize());
                byte[] buf = new byte[BUFFER_SIZE];
                int bytesRead = amis.read(buf);
                parser.parseXMLResourcs(buf);
                xml = parser.getXmlBuilder().getXML();
            }
        }
    }
}
