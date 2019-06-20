package com.rmi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface FileServer extends Remote {
    String readLine(String filename) throws IOException, NotBoundException;
    //List<String> readFile(String filename) throws IOException, NotBoundException;
    void writeLine(String filename, String line) throws IOException, NotBoundException;
    void writeFile(String filename, List<String> text) throws IOException, NotBoundException;
    BufferedReader openBufferedReader(String filename) throws IOException, NotBoundException;
    List<String> listFolderContent(String path) throws NotBoundException, RemoteException, NotDirectoryException, FileNotFoundException;
    boolean deleteFile(String filename) throws RemoteException, NotBoundException, FileNotFoundException;
    boolean copyFile(String fileFrom, String fileTo) throws IOException, NotBoundException;
    boolean moveFile(String fileFrom, String fileTo) throws IOException, NotBoundException;
    //void renameFile(String oldFname, String newFname) throws NotBoundException, FileNotFoundException, FileAlreadyExistsException, RemoteException;
    byte[] getFile(String filename) throws  IOException, NotBoundException;
    void writeFileBytes(byte[] fileBytes, String filename) throws IOException, NotBoundException;
    boolean isDir(String path) throws RemoteException, NotBoundException, FileNotFoundException;

    boolean createDir(String dir) throws FileNotFoundException, RemoteException, NotBoundException;
    //String[] getRegistry(String root) throws RemoteException, NotBoundException;
}
