package com.rmi;

import com.utils.ConfigReader;

import java.io.*;
import java.nio.file.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileServerImpl extends UnicastRemoteObject implements FileServer {
    private String root;
    private String mainRoot;
    private String serverIp;
    private int port;
    private boolean main;
    private String path;
    private String fname;
    private String fnameRoot;

    //private Registry localRegistry;

    private BufferedReader bufferedReader;
    private Map<String, BufferedReader> openReadOnlyFileMap;
    private Map<String, BufferedWriter> openWriteFileMap;

    private ConfigReader configReader;

    public FileServerImpl(String s, String ip, int p) throws RuntimeException, RemoteException {
        super();
        main = false;
        this.root = s;
        this.serverIp = ip;
        this.configReader = new ConfigReader();

        this.mainRoot = configReader.getMainServerRoot();

        this.openReadOnlyFileMap = new HashMap<>();
        try {
            port = p;
            if (!(1024 <= port && port <= 49151)) {
                throw new IllegalArgumentException("port must be between 1024 and 49151");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
    }

    private String splitPathFileOld(String file) {
        int indexPath = file.lastIndexOf(File.separator);
        this.path = file.substring(0, indexPath);
        this.fname = file.substring(indexPath + 1);

        //int indexRoot = file.indexOf(File.separator);
        //return file.substring(0, indexRoot);

        return  this.path;

    }

    private String splitPathFile(String file) {
        int indexPath = file.lastIndexOf(File.separator);
        if(indexPath > -1){
            path = file.substring(0, indexPath);
            this.fname = file.substring(indexPath + 1);

            //int indexRoot = file.indexOf(File.separator);
            //return file.substring(0, indexRoot);

            return  path;
        } else return file;
    }

    /*
        @Override
        public String[] getRegistry(String root) throws RemoteException, NotBoundException {
            if(isPathOwnRoot(root)) return this.localRegistry.list();
            else{
                FileServer fileServer = findInterface(root);
                return fileServer.getRegistry(root);
            }
        }
    */
    public static void main(String[] args) {
        ConfigReader configReader = new ConfigReader();
        String mainRoot = configReader.getMainServerRoot();
        String mainServerIp = configReader.getMainServerIp();
        int mainServerPort = configReader.getMainServerPort();
        String serverName = null;

        String root = "";
        String ip = null;
        ip = "127.0.0.1";
        int port = 0;


        switch (args.length) {
            case 1: {
                serverName = args[0];
                if (serverName.equals("MainServer")) {
                    root = mainRoot;
                    ip = mainServerIp;
                    port = mainServerPort;
                } else if (args[0].matches("SlaveServer[0-9]")) {
                    Map<String, String> slaveServer = configReader.getSlaveServerByName(args[0]);
                    root = slaveServer.get("Root");
                    ip = slaveServer.get("Ip");
                    port = Integer.parseInt(slaveServer.get("Port"));

                }
                break;
            }
            case 3: {
                root = args[0];
                serverName = root;
                ip = args[1];
                port = Integer.parseInt(args[2]);
                break;
            }
            default:
                try {
                    throw new IllegalAccessException("Numero di argomenti errato");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
        }

        System.out.println(root + " " + ip + ":" + port);

        System.setProperty("java.rmi.server.hostname", "192.168.0.10");

        try {
            FileServer RMIFileServer = new FileServerImpl(root, ip, port);

            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(serverName, RMIFileServer);
            System.out.println(Arrays.toString(registry.list()));

            //FileServerImpl.mainRegistry = LocateRegistry.getRegistry(mainServerIp, mainServerPort);
            //FileServerImpl.mainRegistry.rebind(serverName, FileServerImpl);
            //System.out.println(Arrays.toString(FileServerImpl.mainRegistry.list()));

        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private FileServer findInterface(String path) throws RemoteException, NotBoundException {
        Map.Entry<String, Map<String, String>> server = this.configReader.getSlaveServerByPath(path);
        String serverName = server.getKey();
        Map<String, String> values = server.getValue();

        Registry registry = LocateRegistry.getRegistry(values.get("Ip"), Integer.parseInt(values.get("Port")));
        //Registry registry = this.mainRegistry.lookup(values.get("Root"));

        return (FileServer) registry.lookup(serverName);
    }

    private String subPath(String path){
        int indexRoot = path.indexOf(File.separator);
        return ':' + path.substring(indexRoot + 1);
    }

    private Boolean isPathOwnRoot(String path) {
        int indexRoot = path.indexOf(File.separator);
        String root = null;
        if(indexRoot != -1){
            root = path.substring(0, indexRoot);
        } else root = path;
        if(root.equals(this.mainRoot)){
            /*String subPath = path.substring(indexRoot + 1);
            int indexDir = subPath.indexOf(File.separator);
            String dir = subPath.substring(0, indexDir);

            File[] dirContent = new File(subPath).listFiles();
            if (dirContent != null) {
                for(File file : dirContent){
                    if (file.isDirectory()) {
                        if(dir.equals(file.getName())){
                            return true;
                        }
                    }
                }
                return false;
            } else {
                return false;
            }*/
            return true;

        }
        else return false;
    }

    private String getRelPath(String path) {
        int index = path.indexOf(':');
        if (index != -1) return path.replaceFirst(":", "");
        else return path;
    }

    @Override
    public BufferedReader openBufferedReader(String filename) throws IOException, NotBoundException {
        String path = this.splitPathFile(filename);
        if (isPathOwnRoot(filename)) {
            //if(this.openReadOnlyFileMap.get(filename) == null) {
               return this.bufferedReader = new BufferedReader(new FileReader(filename));
                //this.openReadOnlyFileMap.put(filename, this.bufferedReader);
            //}
        } else {
            String subPath = subPath(path);
            String subFilename = subPath(filename);
            FileServer fileServer = findInterface(subPath);
            return fileServer.openBufferedReader(subFilename);
        }

    }

    @Override
    public String readLine(String filename) throws IOException, NotBoundException {
        BufferedReader br = null;
        String path = this.splitPathFile(filename);
        if (isPathOwnRoot(filename)) {
            if(this.openReadOnlyFileMap.get(filename) == null) {
                br = new BufferedReader(new FileReader(filename));
                this.openReadOnlyFileMap.put(filename, br);
            } else br = this.openReadOnlyFileMap.get(filename);
            if (br != null) {
                String line = br.readLine();
                if(line==null) {
                    this.openReadOnlyFileMap.remove(filename);
                }
                return line;
            }
        } else {
            String subPath = subPath(path);
            String subFilename = subPath(filename);
            FileServer fileServer = findInterface(subPath);
            return fileServer.readLine(subFilename);
        }
        return null;
    }

    @Override
    public void writeLine(String filename, String line) throws IOException, NotBoundException {
        String path = this.splitPathFile(filename);
        if (path.equals(this.root)) {
            BufferedWriter br = new BufferedWriter(new FileWriter(filename));
            br.write(line);
        } else {

        }
    }

    @Override
    public List<String> readFile(String filename) throws IOException, NotBoundException {
        String path = this.splitPathFile(filename);
        List<String> text = null;
        String line;
        if (isPathOwnRoot(filename)) {
            BufferedReader br = this.openReadOnlyFileMap.get(filename);
            if (br != null) {
                while (true) {
                    line = br.readLine();
                    if (line != null) {
                        text.add(line);
                    } else break;
                }
                return text;
            }
        } else {
            String subPath = subPath(path);
            String subFilename = subPath(filename);
            FileServer fileServer = findInterface(subPath);
            return fileServer.readFile(subFilename);
        }
        return null;
    }

    public void writeFile(String filename, List<String> text) throws IOException, NotBoundException {
        String path = this.splitPathFile(filename);
        BufferedWriter bw = null;
        if (isPathOwnRoot(filename)) {
            filename = this.getRelPath(filename);
            if(this.openWriteFileMap.get(filename) == null) {
                bw = new BufferedWriter(new FileWriter(filename));
                this.openWriteFileMap.put(filename, bw);
            } else bw = openWriteFileMap.get(filename);
            for(String line : text) {
                bw.write(line);
            }
        } else {
            String subPath = subPath(path);
            String subFilename = subPath(filename);
            FileServer fileServer = findInterface(subPath);
            fileServer.writeFile(subFilename, text);
        }

    }

    @Override
    public File[] listFolderContent(String path) throws NotBoundException, RemoteException, NotDirectoryException, FileNotFoundException {
        if (isPathOwnRoot(path)) {
            path = this.getRelPath(path);
            File dir = new File(path);
            if (dir.exists()) {
                if(dir.isDirectory()) return dir.listFiles();
                else throw new NotDirectoryException(dir + " (Not a directory");
            } else throw new FileNotFoundException(dir + " (Not a valid path)");
        } else {
            String subPath = subPath(path);
            FileServer fileServer = findInterface(subPath);
            return fileServer.listFolderContent(subPath);
        }
    }

    @Override
    public boolean deleteFile(String filename) throws RemoteException, NotBoundException, FileNotFoundException {
        String path = this.splitPathFile(filename);
        if (isPathOwnRoot(path)){
            filename = this.getRelPath(filename);
            File file = new File(filename);
            if(!file.exists()){
                throw new FileNotFoundException(file + " (No such file or directory)");
            }
            return file.delete();
        }
        else {
            FileServer fileServer = findInterface(this.path);
            return fileServer.deleteFile(filename);
        }
    }

    @Override
    public boolean moveFile(String fileFrom, String fileTo) throws IOException, NotBoundException {
        boolean copied = copyFile(fileFrom, fileTo);
        if(copied){
            deleteFile(fileFrom);
            return true;
        }
        else return false;
    }

    @Override
    public boolean copyFile(String filePathFrom, String filePathTo) throws IOException, NotBoundException {
        String path = this.splitPathFile(filePathFrom);
        if(isPathOwnRoot(path)){
            filePathFrom = this.getRelPath(filePathFrom);
            if(isPathOwnRoot(this.splitPathFile(filePathTo))){
                filePathTo = this.getRelPath(filePathTo);
                Path source = Paths.get(filePathFrom);

                if(!Files.exists(source)){
                    throw new FileNotFoundException(source + " (No such file or directory)");
                } else {
                    Path dest = Paths.get(filePathTo);
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            }
            else {
                BufferedReader br = this.openReadOnlyFileMap.get(filePathFrom);
                if (br == null) {
                    this.bufferedReader = new BufferedReader(new FileReader(filePathFrom));
                    this.openReadOnlyFileMap.put(filePathFrom, this.bufferedReader);

                    br = this.bufferedReader;
                }

                FileServer fileServer = findInterface(subPath(filePathTo));
                try {
                    fileServer.writeLine(subPath(filePathTo), br.readLine());
                    return true;
                } catch (IOException | NotBoundException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        else {
            String subPathFrom = subPath(this.path);
            String subFileFrom = subPath(filePathFrom);
            FileServer fileServer = findInterface(subPathFrom);
            return fileServer.copyFile(subFileFrom, filePathTo);
        }
    }

    @Override
    public void renameFile(String oldFname, String newFname) throws NotBoundException, FileNotFoundException, FileAlreadyExistsException, RemoteException {
        String path = this.splitPathFile(oldFname);
        if (isPathOwnRoot(path)){
            oldFname = this.getRelPath(oldFname);
            if(newFname.contains("/")){
                String newPath = this.splitPathFile(newFname);
                if(!path.equals(newPath)){
                    throw new FileNotFoundException(newFname + " (new name must be on same directory)");
                }
            }
            File fileOld = new File(oldFname);
            File fileNew = new File(newFname);
            if(!fileOld.exists()){
                throw new FileNotFoundException(fileOld + " (No such file or directory)");
            }
            if(fileNew.exists()){
                throw new FileAlreadyExistsException(fileNew + " (New filename already exists)");
            }
            fileOld.renameTo(fileNew);
        } else {
            String subPath = subPath(path);
            String subFilename = subPath(oldFname);
            FileServer fileServer = findInterface(subPath);
            fileServer.renameFile(subFilename, newFname);
        }
    }

    @Override
    public byte[] getFile(String filename) throws IOException, NotBoundException {
        String path = this.splitPathFile(filename);
        if (isPathOwnRoot(path)){
            filename = this.getRelPath(filename);
            Path filePath = Paths.get(filename);
            return Files.readAllBytes(filePath);
        } else {
            String subPath = subPath(path);
            String subFileName = subPath(filename);
            FileServer fileServer = findInterface(subPath);
            fileServer.getFile(subFileName);
        }
        return null;
    }

    @Override
    public void writeFileBytes(byte[] fileBytes, String filename) throws IOException, NotBoundException {
        String path = this.splitPathFile(filename);
        if (isPathOwnRoot(path)){
            filename = this.getRelPath(filename);
            File file = new File(filename);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(fileBytes);
        } else {
            String subPath = subPath(path);
            String subFileName = subPath(filename);
            FileServer fileServer = findInterface(subPath);
            fileServer.writeFileBytes(fileBytes, subFileName);
        }
    }

    @Override
    public boolean isDir(String path) throws RemoteException, NotBoundException {
        //path = this.splitPathFile(path);
        if(isPathOwnRoot(path)){
            path = this.getRelPath(path);
            File file = new File(path);
            return file.isDirectory();
        } else {
            String subPath = subPath(this.path);
            FileServer fileServer = findInterface(subPath);
            return fileServer.isDir(subPath);
        }
    }
}
