package com.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Class used to read from the configuration files.
 */
public class ConfigReader {
    private String cfg_name;
    private FileReader cfg;
    private BufferedReader cfg_buff;
    private String mainServerRoot;
    private String mainServerIp;
    private int mainServerPort;
    private Map<String, Map<String, String>> slaveServersMap;
    private Map<String, String> serverNames;

    private String[] slaveRoots;

    /**
     * Constructor of the class. It takes a String representing the config file to be read.
     * @param cfg_name The configuration file
     */
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

    /**
     *  Secondary constructor that provides a default file name for the configuration file.
     */
    public ConfigReader() {
        this("cfg/server.cfg");
    }

    /**
     * Getter for the main server root
     * @return Main server root
     */
    public String getMainServerRoot() {
        return mainServerRoot;
    }

    /**
     *  Getter for the main server IP
     * @return Main server IP
     */
    public String getMainServerIp() {
        return mainServerIp;
    }

    /**
     * Getter for the main server port
     * @return Main server port
     */
    public int getMainServerPort() {
        return mainServerPort;
    }

    /**
     * Method that returns a Map containing a slave server corresponding to the name given as parameter
     * @param server Slave server name
     * @return Slave server map (root, ip, port)
     */
    public Map<String, String> getSlaveServerByName(String server){
        //Map<String, String> map = slaveServersMap.get(server);
        return slaveServersMap.get(server);
    }

    /**
     * Method that returns a Map containing a slave server corresponding to the root given as parameter
     * @param path Slave server root
     * @return Slave server map (root, ip, port)
     */
    public Map.Entry<String, Map<String, String>> getSlaveServerByRoot(String path){
        for(Map.Entry<String, Map<String, String>> entry : slaveServersMap.entrySet()) {
            Map<String, String> values = entry.getValue();
            if (path.equals(values.get("Root"))){
                //values.put("serverName", entry.getKey())
                return entry;
            }
        }
        return null;
    }

    /**
     * Method that return the list of all the roots of the slave servers in FDFS
     * @return List of slave roots.
     */
    public ArrayList<String> getSlaveRoots(){
        ArrayList<String> slaveRoots = new ArrayList<>();
        for(Map.Entry<String, Map<String, String>> entry : slaveServersMap.entrySet()) {
            Map<String, String> values = entry.getValue();
            slaveRoots.add(values.get("Root"));
        }
        return slaveRoots;
    }
}