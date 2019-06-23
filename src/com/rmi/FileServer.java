/**
 * com.rmi package of rmi File Server FDFS
 */
package com.rmi;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * File Server Interface. Implemented by the main class FileServerImpl
 */
public interface FileServer extends Remote {
    /**
     * Method that read a single line from the filename provided as parameter. It mimics the usage of {@link BufferedReader#readLine()}
     * @param filename File to read from
     * @return Line read as String.
     * @throws IOException
     * @throws NotBoundException
     */
    String readLine(String filename) throws IOException, NotBoundException;

    /**
     * Writes a single line to the filename porovided as parameter. It overwrites an existing file with the same name
     * @param filename File to write to
     * @param line Line to write
     * @throws IOException
     * @throws NotBoundException
     * @deprecated
     */
    void writeLine(String filename, String line) throws IOException, NotBoundException;

    /**
     * Write a list of lines (representing a whole text file) to the filename provided as parameter.
     * It overwrites an existing file with the same name
     * @param filename File to write to
     * @param text Content to write
     * @throws IOException
     * @throws NotBoundException
     */
    void writeFile(String filename, List<String> text) throws IOException, NotBoundException;

    /**
     * Open a BufferedReader from rmi. DREPECATED
     * @param filename Filename to open from
     * @return The BufferedReader
     * @throws IOException
     * @throws NotBoundException
     * @deprecated
     */
    BufferedReader openBufferedReader(String filename) throws IOException, NotBoundException;

    /**
     * List the content of the provided path. If the server is the main server, it appends the root names of all the slave servers.
     * @param path Path to list the content from
     * @return List of lines representing each file found
     * @throws NotBoundException
     * @throws RemoteException
     * @throws NotDirectoryException
     * @throws FileNotFoundException
     */
    List<String> listFolderContent(String path) throws NotBoundException, RemoteException, NotDirectoryException, FileNotFoundException;

    /**
     * Deletes the file provided as argument
     * @param filename File to be deleted
     * @return True if successful, False otherwise
     * @throws RemoteException
     * @throws NotBoundException
     * @throws FileNotFoundException
     */
    boolean deleteFile(String filename) throws RemoteException, NotBoundException, FileNotFoundException;

    /**
     * Copy the first file provided as parameter to the filename provided as second parameter.
     * Overwrites files with same name.
     * @param fileFrom File to copy from
     * @param fileTo File to copy to
     * @return True if successful. False otherwise
     * @throws IOException
     * @throws NotBoundException
     */
    boolean copyFile(String fileFrom, String fileTo) throws IOException, NotBoundException;

    /**
     * Move the first file provided as parameter to the filename provided as second parameter. It can be used also to rename the file.
     * The method calls {@link #copyFile(String, String)} and then {@link #deleteFile(String)}
     * @param fileFrom File to move
     * @param fileTo New file name
     * @return True if successful. False otherwise
     * @throws IOException
     * @throws NotBoundException
     */
    boolean moveFile(String fileFrom, String fileTo) throws IOException, NotBoundException;

    /**
     * Retrive a file as an array of bytes. Utilizes {@link java.nio.file.Files#readAllBytes(Path)}
     * @param filename File to be retrieved
     * @return Returns the array of bytes read from the file.
     * @throws IOException
     * @throws NotBoundException
     */
    byte[] getFile(String filename) throws  IOException, NotBoundException;

    /**
     * Writes a file from an array of bytes provided as parameter.
     * @param fileBytes Array of bytes to write
     * @param filename Filename to write to
     * @throws IOException
     * @throws NotBoundException
     */
    void writeFileBytes(byte[] fileBytes, String filename) throws IOException, NotBoundException;

    /**
     * Controls if path provided is a directory
     * @param path Path to control
     * @return True if successful. False otherwise
     * @throws RemoteException
     * @throws NotBoundException
     * @throws FileNotFoundException
     */
    boolean isDir(String path) throws RemoteException, NotBoundException, FileNotFoundException;

    /**
     * Create directory at provided path
     * @param dir directory to create
     * @return True if successful. False otherwise
     * @throws FileNotFoundException
     * @throws RemoteException
     * @throws NotBoundException
     */
    boolean createDir(String dir) throws FileNotFoundException, RemoteException, NotBoundException;
}
