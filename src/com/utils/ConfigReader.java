package com.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigReader {
    private String cfg_name;
    private FileReader cfg;
    private BufferedReader cfg_buff;
    private String mainServerRoot;
    private String mainServerIp;
    private int mainServerPort;
    private Map<String, Map<String, String>> slaveServersMap;
    private Map<String, String> serverNames;

    public ConfigReader(String cfg_name) {
        //cfg_name = "cfg/server.cfg";
        slaveServersMap = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        try {
            cfg = new FileReader(cfg_name);
            cfg_buff = new BufferedReader(cfg);
            String line = cfg_buff.readLine();
            while (line != null) {

                if(
                        !(line.startsWith("#")) && !(line.trim().isEmpty())
                ){
                    String splitLine[] = line.split("=");
                    String varible = splitLine[0].trim();
                    String value = splitLine[1].trim();

                    switch (varible) {
                        case "MainServerRoot":
                            this.mainServerRoot = value;
                            break;
                        case "MainServerIp":
                            this.mainServerIp = value;
                            break;
                        case "MainServerPort": {
                            this.mainServerPort = Integer.parseInt(value);
                            if (!(1024 <= this.mainServerPort && this.mainServerPort <= 49151)) {
                                throw new IllegalArgumentException("port must be between 1024 and 49151");
                            }
                            break;
                        }
                        default:{
                            if (varible.matches("SlaveServer[0-9]Root")) {
                                map = new HashMap<>();
                                map.put("Root", value);
                            } else if (varible.matches("SlaveServer[0-9]Ip")) {
                                map.put("Ip", value);
                                slaveServersMap.put(varible.substring(0, varible.length() - 2), map);
                            }else if (varible.matches("SlaveServer[0-9]Port")){
                                map.put("Port", value);
                                Map<String, String> slaveServer = map;
                                String serverName = varible.substring(0, varible.length() - 4);
                                slaveServersMap.put(serverName, slaveServer);
                            }
                        }
                    }
                }
                line = cfg_buff.readLine();
            }
        System.out.print("");
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public ConfigReader() {
        this("cfg/server.cfg");
    }

    public String getMainServerRoot() {
        return mainServerRoot;
    }

    public String getMainServerIp() {
        return mainServerIp;
    }

    public int getMainServerPort() {
        return mainServerPort;
    }

    public Map<String, String> getSlaveServerByName(String server){
        Map<String, String> map = slaveServersMap.get(server);
        return slaveServersMap.get(server);
    }

    public Map.Entry<String, Map<String, String>> getSlaveServerByPath(String path){
        for(Map.Entry<String, Map<String, String>> entry : slaveServersMap.entrySet()) {
            Map<String, String> values = entry.getValue();
            if (path.equals(values.get("Root"))){
                //values.put("serverName", entry.getKey())
                return entry;
            }
        }
        return null;
    }
}