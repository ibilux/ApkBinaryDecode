/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hq;

import java.util.TreeMap;


/**
 *
 * @author hq
 */


public class BuildXml {
    private final TreeMap<Integer, String> xmlLines;

    public BuildXml() {
        xmlLines = new TreeMap();
    }
    public void startXML() {
        xmlLines.put(0, "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    }
    
    public void startNode(int line, String name) {
        xmlLines.put(line, "<"+name);
    }
    
    public void addAttribute(int line, String attribute, String value) {
        xmlLines.put(line, xmlLines.get(line).concat(" "+ attribute + "=" + "\"" + value + "\""));
    }
    
    public void closeNode(int line) {
        xmlLines.put(line, xmlLines.get(line).concat(">"));
    }
    
    public void endNode(int line, String name) {
        
        if(xmlLines.containsKey(line))
            xmlLines.put(line, xmlLines.get(line).substring(0, xmlLines.get(line).length() - 1).concat("/>"));
        else
            xmlLines.put(line, "</"+name+">");
    }
    public void endXML() {
        xmlLines.put(xmlLines.size()+1, "/n");
    }
    public String getXML() {
        String xml = "";
        for(Integer key : xmlLines.keySet()) {
            xml += xmlLines.get(key) + "\n";
        }   
        return xml;
    }
}
