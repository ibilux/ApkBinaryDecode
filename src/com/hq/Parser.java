package com.hq;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author bilux (i.bilux@gmail.com)
 */

public class Parser {
    /**
     * Binary XML String pool
     */
    ArrayList<String> xmlStringPool = new ArrayList<>();
    
    /**
     * Binary Resource String pool
     */
    ArrayList<String> resTableStringPool = new ArrayList<>();
    ArrayList<String> resPackageStringPool = new ArrayList<>();
    
    /**
     * Resource Map
     */
    ArrayList<Integer> resMap = new ArrayList<>();
    
    // xml builder
    BuildXml buildXml = new BuildXml();
    
    //HashMap<Integer, Object> attributes = new HashMap<>();
    
    /**
     * Order of XML node. Used by XML generator to track root node should
     * include XML NameSpace definition and children should not.
     */

    private static final int // Resource Types
            RES_STRING_POOL_TYPE = 0x0001,
            RES_TABLE_TYPE = 0x0002,
            RES_XML_TYPE = 0x0003,
            // Chunk types in RES_XML_TYPE
            RES_XML_START_NAMESPACE_TYPE = 0x0100,
            RES_XML_END_NAMESPACE_TYPE = 0x0101,
            RES_XML_START_ELEMENT_TYPE = 0x0102,
            RES_XML_END_ELEMENT_TYPE = 0x0103,
            // This contains a uint32_t array mapping strings in the string
            // pool back to resource identifiers. It is optional.
            RES_XML_RESOURCE_MAP_TYPE = 0x0180,
            // Chunk types in RES_TABLE_TYPE
            RES_TABLE_PACKAGE_TYPE = 0x0200;

    public BuildXml getXmlBuilder() {
        return buildXml;
    }
    
    public ArrayList<String> getXmlStringPool() {
        return xmlStringPool;
    }

    public ArrayList<String> getResourcesPackageStringPool() {
        return resPackageStringPool;
    }
    
    public ArrayList<String> getResourcesTableStringPool() {
        return resTableStringPool;
    }
   
    public ArrayList<Integer> getResourcesMap() {
        return resMap;
    }


    /**
     * Parse XML resourcs...
     *
     * [String Pool] [Resource Map] [Namespace Start] [XML Start] [XML End] [XML
     * Start] [XML End] ..... [Namespace End] * [Namespace Start] [XML Start]
     * [XML End] [XML Start] [XML End] ..... [Namespace End] .... # There can be
     * multiple Namespace and within one Name space multiple XML nodes.
     *
     * @param buf
     * @throws IOException
     */
    public void parseXMLResourcs(byte buf[]) throws IOException {
        int headerSize, chunkSize, nsStartLineNumber = -1, nsStartPrefixIndex = -1, nsStartUriIndex = -1;
        byte[] chunkType_buf2 = new byte[2];
        byte[] headerSize_buf2 = new byte[2];
        byte[] chunkSize_buf4 = new byte[4];

        ByteArrayInputStream in = new ByteArrayInputStream(buf);

        // Is it an valid BXML ?
        /*
         * Chunk header meta size - 8 bytes
         * [Chunk Type] - 2 bytes
         * [Chunk Header Size] - 2 bytes
         * [Chunk Size] - 4 bytes
         */
        in.read(chunkType_buf2);

        if (Utils.getShort(chunkType_buf2) != RES_XML_TYPE) {
            //log(logKey, "It's an invalid BXML file. Exiting!");
            return;
        }

        in.read(headerSize_buf2);
        //headerSize = Utils.getShort(headerSize_buf2);

        in.read(chunkSize_buf4);
        //chunkSize = Utils.getInt(chunkSize_buf4);

        in.read(chunkType_buf2);
        if (Utils.getShort(chunkType_buf2) == RES_STRING_POOL_TYPE) {
            // String Pool/Tokens
            in.read(headerSize_buf2);
            headerSize = Utils.getShort(headerSize_buf2);

            in.read(chunkSize_buf4);
            chunkSize = Utils.getInt(chunkSize_buf4);

            byte[] spBuf = new byte[chunkSize - 8];
            in.read(spBuf);

            // Parse String pool
            xmlStringPool = parseStringPool(spBuf, headerSize, chunkSize);

            // Get the next Chunk
            in.read(chunkType_buf2);
        }

        // Resource Mapping- Optional Content
        if (Utils.getShort(chunkType_buf2) == RES_XML_RESOURCE_MAP_TYPE) {
            in.read(headerSize_buf2);
            //headerSize = Utils.getShort(headerSize_buf2);

            in.read(chunkSize_buf4);
            chunkSize = Utils.getInt(chunkSize_buf4);

            byte[] rmBuf = new byte[chunkSize - 8];
            in.read(rmBuf);

            // Parse Resource Mapping
            parseResMapping(rmBuf);

            // Get the next Chunk
            in.read(chunkType_buf2);
        }

        /*
         * There can be multiple Name space and XML node sections
         * [XML_NameSpace_Start]
         * 	[XML_Start]
         *  	[XML_Start]
         * 		[XML_End]
         *  [XML_END]
         * [XML_NameSpace_End]
         * [XML_NameSpace_Start]
         * 	[XML_Start]
         * 	[XML_End]
         * [XML_NameSpace_End]
         */
        // Name space Start
        if (Utils.getShort(chunkType_buf2) == RES_XML_START_NAMESPACE_TYPE) {
            in.read(headerSize_buf2);
            //headerSize = Utils.getShort(headerSize_buf2);

            in.read(chunkSize_buf4);
            chunkSize = Utils.getInt(chunkSize_buf4);

            byte[] nsStartBuf = new byte[chunkSize - 8];
            in.read(nsStartBuf);

            // Parse Start of Name space
            /**
             * One NameSpace includes multiple XML elements [ChunkType]
             * [HeaderSize] [Chunk Size]
             * <-- Chunk Body -->
             * [Line Number] - Line number where to place this [Comment] - TODO:
             * Skip it for the time being [Prefix] - String pool index [URI] -
             * String Pool index
             */
            buildXml.startXML();
            ByteArrayInputStream nsin = new ByteArrayInputStream(nsStartBuf);
            byte[] buf4 = new byte[4];
            nsin.read(buf4);
            nsStartLineNumber = Utils.getInt(buf4);

            nsin.read(buf4);
            int nsComment = Utils.getInt(buf4);

            nsin.read(buf4);
            nsStartPrefixIndex = Utils.getInt(buf4);

            nsin.read(buf4);
            nsStartUriIndex = Utils.getInt(buf4);
        }

        // Handle multiple XML Elements
        in.read(chunkType_buf2);
        int chunk_type = Utils.getShort(chunkType_buf2);

        while (chunk_type != RES_XML_END_NAMESPACE_TYPE) {
            /*
             * XML_Start
             * 	XML_Start
             *  XML_End
             * XML_End
             * .......
             */
            in.read(headerSize_buf2);
            //headerSize = Utils.getShort(headerSize_buf2);

            in.read(chunkSize_buf4);
            chunkSize = Utils.getInt(chunkSize_buf4);

            byte[] elementBuf = new byte[chunkSize - 8];
            in.read(elementBuf);

            if (chunk_type == RES_XML_START_ELEMENT_TYPE) {
                // Start of XML Node
                parseXMLStart(elementBuf, nsStartLineNumber, nsStartPrefixIndex, nsStartUriIndex);
            } else if (chunk_type == RES_XML_END_ELEMENT_TYPE) {
                // End of XML Node
                parseXMLEnd(elementBuf, nsStartPrefixIndex, nsStartUriIndex);
            }

            // TODO: CDATA
            // Next Chunk type
            in.read(chunkType_buf2);
            chunk_type = Utils.getShort(chunkType_buf2);
        }

        // End of Name space
        if (chunk_type == RES_XML_END_NAMESPACE_TYPE) {
            in.read(headerSize_buf2);
            //headerSize = Utils.getShort(headerSize_buf2);

            in.read(chunkSize_buf4);
            chunkSize = Utils.getInt(chunkSize_buf4);

            in.skip(chunkSize - 8);

            // Parse End of Name space
            /**
             * End of Name space. Chunk structure same as Start of Name space
             */
            
            /*
            byte[] nsEndBuf = new byte[chunkSize - 8];
            in.read(nsEndBuf);
            
            ByteArrayInputStream nsin = new ByteArrayInputStream(nsEndBuf);

            byte[] buf4 = new byte[4];
            nsin.read(buf4);
            int nsEndLineNumber = Utils.getInt(buf4);

            nsin.read(buf4);
            int nsEndComment = Utils.getInt(buf4);

            nsin.read(buf4);
            int nsEndPrefixIndex = Utils.getInt(buf4);

            nsin.read(buf4);
            int nsEndUriIndex = Utils.getInt(buf4);
            */
        }

        String xml = buildXml.getXML();
        String xmlx = "";
        // That's it. TODO: Handle multiple Name spaces
    }

    /**
     * Resource IDs of Attributes Each ID of int32 TODO: Use this information
     * for XML generation.
     *
     * @param rmBuf
     * @throws IOException
     */
    private void parseResMapping(byte[] rmBuf) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(rmBuf);
        // Each ID of 4 bytes
        int num_of_res_ids = rmBuf.length / 4;

        byte[] buf4 = new byte[4];
        for (int i = 0; i < num_of_res_ids; i++) {
            in.read(buf4);
            resMap.add(Utils.getInt(buf4));
        }
    }

    /**
     * XML_Start_Node Chunk Body- line number- init32 comment- init32 ns - int32
     * name - int32 attributeStart- int16 attributeSize- int16 attributeCount -
     * int16 id_index - int16 class_index - int16 style_index - int16
     *
     * [Attributes] ns- int32 name- int32 rawValue- init32 typedValue- size-
     * init16 res- init8 dataType- init8 data- init32
     *
     * TODO: Retrieve Attribute data from resources.arsc
     *
     * @param xmlStartBuf
     * @param nsStartLineNumber
     * @param nsStartPrefixIndex
     * @param nsStartUriIndex
     * @throws IOException
     */
    private void parseXMLStart(byte[] xmlStartBuf, int nsStartLineNumber, int nsStartPrefixIndex, int nsStartUriIndex) throws IOException {
        // 
        ByteArrayInputStream in = new ByteArrayInputStream(xmlStartBuf);
            
        byte[] buf4 = new byte[4];

        in.read(buf4);
        int lineNumber = Utils.getInt(buf4);

        in.read(buf4);
        int comment = Utils.getInt(buf4);

        in.read(buf4);
        int nsIndex = Utils.getInt(buf4);

        in.read(buf4);
        int nameIndex = Utils.getInt(buf4);

        byte[] buf2 = new byte[2];

        in.read(buf2);
        int attributeStart = Utils.getShort(buf2);

        in.read(buf2);
        int attributeSize = Utils.getShort(buf2);

        in.read(buf2);
        int attributeCount = Utils.getShort(buf2);

        // Skip ID, Class and Style index
        in.skip(6);


        if (nameIndex == -1)
            buildXml.startNode(lineNumber, "");
        else
            buildXml.startNode(lineNumber, xmlStringPool.get(nameIndex));
        
        if(lineNumber==nsStartLineNumber)
            if (nsStartPrefixIndex != -1 && nsStartUriIndex != -1)
                buildXml.addAttribute(lineNumber, "xmlns:"+xmlStringPool.get(nsStartPrefixIndex), xmlStringPool.get(nsStartUriIndex));

        if (attributeCount == 0) {
            buildXml.closeNode(lineNumber);
            return;
        }

        for (int ii = 0; ii < attributeCount; ii++) {
            // attr ns
            in.read(buf4);
            int attrNsIndex = Utils.getInt(buf4);
            
            // attr name
            in.read(buf4);
            int attrNameIndex = Utils.getInt(buf4);

            // Raw value. If user has directly mentioned value e.g. android:value="1". Reference to String Pool
            in.read(buf4);
            int attrRawValue = Utils.getInt(buf4);

            String attrValue;
            
            if (attrRawValue == -1) {
                // No Raw value defined.
                // Read Typed Value. Reference to Resource table e.g. String.xml, Drawable etc.
                /*
                         * Size of Types value- init16
                         * Res- init8 (Always 0)
                         * Data Type- init8
                         * Data- init32. Interpreted according to dataType
                 */
                in.read(buf2);
                int dataSize = Utils.getShort(buf2);

                // Skip res value- Always 0
                in.skip(1);

                // TODO: Retrieve data based on Data_Type. Read Resource table.
                int dataType = in.read();

                in.read(buf4);
                int data = Utils.getInt(buf4); // Refer to Resource Table
                if(dataType==1){
                    // Retrieve data from Resource table.
                    if(resMap.contains(data))
                        attrValue = resTableStringPool.get(resMap.indexOf(data));
                    else
                        attrValue = "" + data;
                }else{
                    attrValue = "" + data;
                }
            } else {
                attrValue = xmlStringPool.get(attrRawValue);
                // Skip Typed value bytes
                in.skip(8);
            }
            
            if (attrNameIndex != -1) {
                String attrName;
                if (attrNsIndex != -1)
                    //attrName = stringPool.get(attrNsIndex)+":"+stringPool.get(attrNameIndex);
                    attrName = xmlStringPool.get(nsStartPrefixIndex)+":"+xmlStringPool.get(attrNameIndex);
                else
                    attrName = xmlStringPool.get(attrNameIndex);
                
                buildXml.addAttribute(lineNumber, attrName, attrValue);
            }
        }
        buildXml.closeNode(lineNumber);
    }

    /**
     * XML_END Node Chunk Body- [Line Number] [Comment] [Name space] - Name
     * space. Ref to String pool, unless -1. [Name] - Ref to String pool
     *
     * @param xmlEndBuf
     * @param nsStartPrefixIndex
     * @param nsStartUriIndex
     * @throws IOException
     */
    private void parseXMLEnd(byte[] xmlEndBuf, int nsStartPrefixIndex, int nsStartUriIndex) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(xmlEndBuf);

        byte[] buf4 = new byte[4];
        in.read(buf4);
        int lineNumber = Utils.getInt(buf4);

        in.read(buf4);
        int comment = Utils.getInt(buf4);

        in.read(buf4);
        int nsIndex = Utils.getInt(buf4);

        in.read(buf4);
        int nameIndex = Utils.getInt(buf4);

        if (nameIndex != -1) {
            buildXml.endNode(lineNumber, xmlStringPool.get(nameIndex));
        }
    }

    /**
     * Parse resources.arsc (Resource Table). StringPool and Resource data
     * collected from this File will be used for Binary XML layout files.
     *
     * 1. Parse Resource header 2. Parse Resource string pool 3. Parse Resource
     * packages
     *
     * @param buf Resource Table bytes
     * @throws IOException
     */
    public void parseResourceTable(byte[] buf) throws IOException {
        byte[] buf2 = new byte[2];
        byte[] buf4 = new byte[4];
        ByteArrayInputStream in = new ByteArrayInputStream(buf);
        
        // Is it an valid BXML ?
        /*
         * Chunk header meta size - 8 bytes
         * [Chunk Type] - 2 bytes
         * [Chunk Header Size] - 2 bytes
         * [Chunk Size] - 4 bytes
         */

        // Chunk type- 2 bytes
        in.read(buf2);
        if (Utils.getShort(buf2) != RES_TABLE_TYPE) {
            return;
        }

        // Header size- 2 bytes
        in.read(buf2);
        int headerSize = Utils.getShort(buf2);

        // Chunk size- 4 bytes
        in.read(buf4);
        int chunkSize = Utils.getInt(buf4);

        // Package count- 4 bytes
        in.read(buf4);
        //int package_count = Utils.getInt(buf4);

        /*
         * String Pool
         */
        // Read next chunk- 2 bytes
        in.read(buf2);
        if (Utils.getShort(buf2) == RES_STRING_POOL_TYPE) // String Pool 
        {
            // String Pool/Tokens
            in.read(buf2);
            headerSize = Utils.getShort(buf2);

            in.read(buf4);
            chunkSize = Utils.getInt(buf4);

            byte[] spBuf = new byte[chunkSize - 8];
            in.read(spBuf);

            // Parse String pool
            resTableStringPool = parseStringPool(spBuf, headerSize, chunkSize);

            // Get the next Chunk
            in.read(buf2);
        }

        // TODO: Parse Resource package
        if (Utils.getShort(buf2) == RES_TABLE_PACKAGE_TYPE) // RES_Table_Package
        {
            // Parse Resource package stream
            parseResPackage(in);
        }
    }

    private void parseResPackage(ByteArrayInputStream in) throws IOException {
        byte[] buf2 = new byte[2];
        byte[] buf4 = new byte[4];
        
        // Header size- 2 bytes
        in.read(buf2);
        
        int headerSize = Utils.getShort(buf2);

        // Chunk size- 4 bytes
        in.read(buf4);
        int chunkSize = Utils.getInt(buf4);

        in.read(buf4);
        int packageID = Utils.getInt(buf4);

        // 128 Characters (16-bit Char)
        byte[] packageName_buf = new byte[256];
        in.read(packageName_buf);

        String packageName = Utils.getString(packageName_buf);

        // typeStrings- init32
        // Index/Offset position of Type String Pool
        in.read(buf4);
        int typeStrings = Utils.getInt(buf4);

        // Last public type
        // Index (from end) or Count of Types defined in Type String Pool (last lastPublicType entries)
        in.read(buf4);
        int lastPublicType = Utils.getInt(buf4);

        // Key String
        // Index/Offset position of Key String Pool
        in.read(buf4);
        int keyString = Utils.getInt(buf4);

        // Last index into Key string
        // Index (from end) or Count of Keys defined in Key String Pool (last lastPublicKey entries)
        in.read(buf4);
        int lastPublicKey = Utils.getInt(buf4);

        // Parse "Type String Pool"
        in.read(buf2);
        if (Utils.getShort(buf2) == RES_STRING_POOL_TYPE) {
            // String Pool/Tokens
            in.read(buf2);
            headerSize = Utils.getShort(buf2);

            in.read(buf4);
            chunkSize = Utils.getInt(buf4);

            byte[] spBuf = new byte[chunkSize - 8];
            in.read(spBuf);

            // Parse String pool
            resPackageStringPool = parseStringPool(spBuf, headerSize, chunkSize);

            // Get the next Chunk
            in.read(buf2);
        }
        
        // Parse "Key String Pool"
        if (Utils.getShort(buf2) == RES_STRING_POOL_TYPE) {
            // String Pool/Tokens
            in.read(buf2);
            headerSize = Utils.getShort(buf2);

            in.read(buf4);
            chunkSize = Utils.getInt(buf4);

            byte[] spBuf = new byte[chunkSize - 8];
            in.read(spBuf);

            // Parse String pool
            resPackageStringPool = parseStringPool(spBuf, headerSize, chunkSize);

            // Get the next Chunk
            in.read(buf2);
        }
        // TODO: Res_Type, Res_Type_Spec
    }
    

    private ArrayList<String> parseStringPool(byte[] spBuf, int headerSize, int chunkSize) throws IOException {
        ArrayList<String> stringPool = new ArrayList<>();
        ByteArrayInputStream in = new ByteArrayInputStream(spBuf);

        // String pool header
        byte[] buf4 = new byte[4];
        in.read(buf4);

        int string_count = Utils.getInt(buf4);
        in.read(buf4);
        int style_count = Utils.getInt(buf4);
        in.read(buf4);
        int flag = Utils.getInt(buf4);
        in.read(buf4);
        int string_start = Utils.getInt(buf4);
        in.read(buf4);
        int style_start = Utils.getInt(buf4);

        // String pool data
        // Read index location of each String
        int[] string_indices = new int[string_count];
        if (string_count > 0) {
            for (int i = 0; i < string_count; i++) {
                in.read(buf4);
                string_indices[i] = Utils.getInt(buf4);
            }
        }

        if (style_count > 0) {
            // Skip Style
            in.skip(style_count * 4);
        }

        // Read Strings
        for (int i = 0; i < string_count; i++) {
            int string_len;
            if (i == string_count - 1) {
                if (style_start == 0)// There is no Style span
                {
                    // Length of the last string. Chunk Size - Start position of this String - Header - Len of Indices
                    string_len = chunkSize - string_indices[i] - headerSize - 4 * string_count;
                } else {
                    string_len = style_start - string_indices[i];
                }
            } else {
                string_len = string_indices[i + 1] - string_indices[i];
            }

            /*
             * Each String entry contains Length header (2 bytes to 4 bytes) + Actual String + [0x00]
             * Length header sometime contain duplicate values e.g. 20 20
             * Actual string sometime contains 00, which need to be ignored
             * Ending zero might be  2 byte or 4 byte
             * 
             * TODO: Consider both Length bytes and String length > 32767 characters 
             */
            byte[] buf2 = new byte[2];
            in.read(buf2);
            int actual_str_len;
            if (buf2[0] == buf2[1]) // Its repeating, happens for Non-Manifest file. e.g. 20 20
            {
                actual_str_len = buf2[0];
            } else {
                actual_str_len = Utils.getShort(buf2);
            }

            byte[] str_buf = new byte[actual_str_len];
            byte[] buf = new byte[string_len - 2]; // Skip 2 Length bytes, already read.
            in.read(buf);
            int j = 0;
            for (int k = 0; k < buf.length; k++) {
                // Skipp 0x00
                if (buf[k] != 0x00) {
                    str_buf[j++] = buf[k];
                }
            }
            
            stringPool.add(new String(str_buf));
        }
        return stringPool;
    }
}
