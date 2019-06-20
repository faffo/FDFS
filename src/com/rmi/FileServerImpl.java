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
    private String serverIp;
    private int port;

    private String mainRoot;
    private String mainServerIp;
    private int mainServerPort;

    private boolean main;
    private String path;
    private String fname;
    private String fnameRoot;

    private boolean firstPathLocal;
    private boolean secondPathLocal;

    private File rootContent;

    //private Registry localRegistry;

    private BufferedReader bufferedReader;
    private Map<String, BufferedReader> openReadOnlyFileMap;
    private Map<String, BufferedWriter> openWriteFileMap;
    private ConfigReader configReader;

    private String[] firstSplittedPath;
    private String[] secondSplittedPath;

    public FileServerImpl() throws RemoteException {
        super();

        this.configReader = new ConfigReader();
        this.mainRoot = configReader.getMainServerRoot();
        this.mainServerIp = configReader.getMainServerIp();
        this.mainServerPort = configReader.getMainServerPort();

        this.openReadOnlyFileMap = new HashMap<>();
    }

    public FileServerImpl(String s, String ip, int p) throws RuntimeException, RemoteException {
        this();

        this.root = s;
        this.serverIp = ip;
        try {
            port = p;
            if (!(1024 <= port && port <= 49151)) {
                throw new IllegalArgumentException("port must be between 1024 and 49151");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }

        this.main = this.root.equals(this.mainRoot) && this.serverIp.equals(this.mainServerIp) && this.port == this.mainServerPort;

        this.rootContent = new File(this.root);

    }

    public FileServerImpl(String serverName) throws RemoteException {
        this();

        if (serverName.equals("MainServer")) {
            this.main = true;

            this.root = mainRoot;
            this.serverIp = mainServerIp;
            this.port = mainServerPort;
        } else if (serverName.matches("SlaveServer[0-9]")) {
            this.main = false;

            Map<String, String> slaveServer = configReader.getSlaveServerByName(serverName);
            this.root = slaveServer.get("Root");
            this.serverIp = slaveServer.get("Ip");
            this.port = Integer.parseInt(slaveServer.get("Port"));
        }

        this.rootContent = new File(this.root);

    }
/*
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
*/
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

    private FileServer findInterface(String path) throws RemoteException, NotBoundException {
        Map.Entry<String, Map<String, String>> server = this.configReader.getSlaveServerByPath(path);
        String serverName = server.getKey();
        Map<String, String> values = server.getValue();

        Registry registry = LocateRegistry.getRegistry(values.get("Ip"), Integer.parseInt(values.get("Port")));
        //Registry registry = this.mainRegistry.lookup(values.get("Root"));

        return (FileServer) registry.lookup(serverName);
    }

    private String subPath(String[] path){
        return String.join("/", Arrays.copyOfRange(path, 1, path.length));
    }
/*
    private Boolean isPathOwnRoot_old(String path) {
        int indexRoot = path.indexOf(File.separator);
        String root = null;
        if(indexRoot != -1){
            root = path.substring(0, indexRoot);
        } else root = path;
        if(root.equals(this.mainRoot)){
            String subPath = path.substring(indexRoot + 1);
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
            }
            return true;

        }
        else return false;
    }

    private Boolean isPathOwnRoot() {
        return firstSplittedPath[0].equals(this.root);
    }
*/
    private Boolean isFirstChildRemote(String root){
        Map.Entry<String, Map<String, String>> slaveServer = this.configReader.getSlaveServerByPath(root);

        return slaveServer != null;
    }

    private String getRelPath(String path) {
        int index = path.indexOf(':');
        if (index != -1) return path.replaceFirst(":", "");
        else return path;
    }

    private void processPath(String path) throws FileNotFoundException {
        if(path.charAt(0) == ':'){
            path = path.substring(1);
        }
        this.firstSplittedPath = path.split("/");
        if(!this.firstSplittedPath[0].equals(this.root)){
            throw new FileNotFoundException(path + " (No such file or directory");
        }

        if(this.firstSplittedPath.length == 1){
            this.firstPathLocal = true;
        } else {
            this.firstPathLocal = !isFirstChildRemote(this.firstSplittedPath[1]);
        }
        //if(this.main){
        //    this.firstPathLocal = this.root.equals(this.firstSplittedPath[0]);
        //} else this.firstPathLocal = true;
    }

    private void processPath(String firstPath, String secondPath) throws FileNotFoundException {
        this.processPath(firstPath);
        if(secondPath.charAt(0) == ':'){
            secondPath = secondPath.substring(1);
        }
        this.secondSplittedPath = secondPath.split("/");
        if(!this.secondSplittedPath[0].equals(this.root)){
            throw new FileNotFoundException(secondPath + " (No such file or directory");
        }

        if(this.secondSplittedPath.length == 1){
            this.secondPathLocal = true;
        } else {
            this.secondPathLocal = !isFirstChildRemote(this.firstSplittedPath[1]);
        }
    }

    @Override
    public BufferedReader openBufferedReader(String filename) throws IOException, NotBoundException {
        this.processPath(filename);
        if(this.firstPathLocal) {
            //if(this.openReadOnlyFileMap.get(filename) == null) {
            return this.bufferedReader = new BufferedReader(new FileReader(filename));
            //this.openReadOnlyFileMap.put(filename, this.bufferedReader);
            //}

        } else {
            String subFilename = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.openBufferedReader(subFilename);
        }
    }

    @Override //check
    public String readLine(String filename) throws IOException, NotBoundException {
        BufferedReader br = null;
        this.processPath(filename);
        if(this.firstPathLocal) {
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
            String subFilename = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.readLine(subFilename);
        }
        return null;
    }

    @Override
    public void writeLine(String filename, String line) throws IOException, NotBoundException {
        this.processPath(filename);
        if(this.firstPathLocal) {
            BufferedWriter br = new BufferedWriter(new FileWriter(filename));
            br.write(line);
        } else {
            String subFilename = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            fileServer.writeLine(subFilename, line);
        }
    }
/*
    @Override
    public List<String> readFile(String filename) throws IOException, NotBoundException {
        List<String> text = null;
        String line;
        this.processPath(filename);
        if(this.firstPathLocal) {
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
        }else {
            String subFilename = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.readFile(subFilename);
        }
        return null;
    }
*/
    @Override //check
    public void writeFile(String filename, List<String> text) throws IOException, NotBoundException {
        BufferedWriter bw = null;
        this.processPath(filename);
        if(this.firstPathLocal) {
            filename = this.getRelPath(filename);
            if(this.openWriteFileMap.get(filename) == null) {
                bw = new BufferedWriter(new FileWriter(filename));
                this.openWriteFileMap.put(filename, bw);
            } else bw = openWriteFileMap.get(filename);
            for(String line : text) {
                bw.write(line);
            }
        }else {
            String subFilename = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            fileServer.writeFile(subFilename, text);
        }
    }

    @Override // check
    public File[] listFolderContent(String path) throws NotBoundException, RemoteException, NotDirectoryException, FileNotFoundException {
        this.processPath(path);
        if(this.firstPathLocal){
            File dir = new File(path);
            if (dir.exists()) {
                if(dir.isDirectory()) return dir.listFiles();
                else throw new NotDirectoryException(dir + " (Not a directory");
            } else throw new FileNotFoundException(dir + " (Not a valid path)");
        } else {
            String subPath = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.listFolderContent(subPath);
        }
    }

    @Override //check
    public boolean deleteFile(String filename) throws RemoteException, NotBoundException, FileNotFoundException {
        this.processPath(filename);
        if(this.firstPathLocal){
            filename = this.getRelPath(filename);
            File file = new File(filename);
            if(!file.exists()){
                throw new FileNotFoundException(file + " (No such file or directory)");
            }
            return file.delete();
        } else {
            String subFilename = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.deleteFile(subFilename);
        }
    }

    @Override //check
    public boolean moveFile(String fileFrom, String fileTo) throws IOException, NotBoundException {
        boolean copied = copyFile(fileFrom, fileTo);
        if(copied){
            deleteFile(fileFrom);
            return true;
        }
        else return false;
    }

    @Override //check
    public boolean copyFile(String filePathFrom, String filePathTo) throws IOException, NotBoundException {
        this.processPath(filePathFrom, filePathTo);
        if(this.firstPathLocal) {
            if(this.secondPathLocal){
                filePathTo = this.getRelPath(filePathTo);
                Path source = Paths.get(filePathFrom);

                if(!Files.exists(source)){
                    throw new FileNotFoundException(source + " (No such file or directory)");
                } else {
                    Path dest = Paths.get(filePathTo);
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            } else {
                /*
                BufferedReader br = this.openReadOnlyFileMap.get(filePathFrom);

                if (br == null) {
                    this.bufferedReader = new BufferedReader(new FileReader(filePathFrom));
                    this.openReadOnlyFileMap.put(filePathFrom, this.bufferedReader);
                    br = this.bufferedReader;
                }
                */
                FileServer fileServer = findInterface(this.secondSplittedPath[1]);
                try {
                    String subFilename = subPath(this.secondSplittedPath);
                    File fileFrom = new File(filePathFrom.substring(1));
                    //fileServer.writeLine(subPath(this.secondSplittedPath), br.readLine());
                    fileServer.writeFileBytes(Files.readAllBytes(fileFrom.toPath()), subFilename);
                    return true;
                } catch (IOException | NotBoundException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } else {
            String subFileFrom = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.copyFile(subFileFrom, filePathTo);
        }
    }

    /*
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
*/
    @Override //check
    public byte[] getFile(String filename) throws IOException, NotBoundException {
        this.processPath(filename);
        if(this.firstPathLocal) {
            filename = this.getRelPath(filename);
            Path filePath = Paths.get(filename);
            return Files.readAllBytes(filePath);
        } else {
            String subFilename = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            fileServer.getFile(subFilename);
        }
        return null;
    }

    @Override //check
    public void writeFileBytes(byte[] fileBytes, String filename) throws IOException, NotBoundException {
        this.processPath(filename);
        if(this.firstPathLocal) {
            filename = this.getRelPath(filename);
            File file = new File(filename);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(fileBytes);
        } else {
            String subFileName = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            fileServer.writeFileBytes(fileBytes, subFileName);
        }
    }


    @Override //check
    public boolean isDir(String path) throws RemoteException, NotBoundException, FileNotFoundException {
        this.processPath(path);
        if(this.firstPathLocal) {
            File file = new File(path);
            return file.isDirectory();
        }else {
            String subPath = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.isDir(subPath);
        }
    }

    @Override
    public boolean createDir(String path) throws FileNotFoundException, RemoteException, NotBoundException {
        this.processPath(path);
        if(this.firstPathLocal){
            File dir = new File(path);
            return dir.mkdir();
        } else {
            String subPath = subPath(this.firstSplittedPath);
            FileServer fileServer = findInterface(this.firstSplittedPath[1]);
            return fileServer.createDir(subPath);
        }
    }

    public static void main(String[] args) {
        //ConfigReader configReader = new ConfigReader();
        //String mainRoot = configReader.getMainServerRoot();
        //String mainServerIp = configReader.getMainServerIp();
        //int mainServerPort = configReader.getMainServerPort();
        String serverName = null;
        FileServerImpl fileServerImpl =null;
        //System.out.println(root + " " + ip + ":" + port);

        //System.setProperty("java.rmi.server.hostname", "192.168.0.24");

        try {
            switch (args.length) {
                case 1: {
                    serverName = args[0];
                    fileServerImpl = new FileServerImpl(serverName);
                    break;
                }
                case 3: {
                    String root = args[0];
                    serverName = root;
                    String ip = args[1];
                    int port = Integer.parseInt(args[2]);
                    fileServerImpl = new FileServerImpl(root, ip, port);

                    break;
                }
                default:
                    try {
                        throw new IllegalAccessException("Numero di argomenti errato");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    break;
            }

            if(fileServerImpl != null){
                Registry registry = LocateRegistry.createRegistry(fileServerImpl.port);
                registry.rebind(serverName, fileServerImpl);

                System.out.println("Started Server: " + serverName + " (" + fileServerImpl.serverIp + ":" + fileServerImpl.port + ")");
                System.out.println(("Press CTRL+C to terminate "));
            }

            //FileServerImpl.mainRegistry = LocateRegistry.getRegistry(mainServerIp, mainServerPort);
            //FileServerImpl.mainRegistry.rebind(serverName, FileServerImpl);
            //System.out.println(Arrays.toString(FileServerImpl.mainRegistry.list()));

        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}
