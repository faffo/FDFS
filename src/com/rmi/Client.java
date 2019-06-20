package com.rmi;

import com.rmi.customExceptions.CommandArgumentNeededException;
import com.rmi.customExceptions.InvalidCommandException;
import com.utils.ConfigReader;
import com.utils.FDFSWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Client {
    String mainRoot;
    String mainServerIp;
    int mainServerPort;

    boolean connectionStatus = false;
    String connectionInfo = new String("no connection");
    String pwd = new String("");
    Registry registry = null;
    FileServer fileServer = null;

    private Client() {
        ConfigReader configReader = new ConfigReader("cfg/client.cfg");
        this.mainRoot = configReader.getMainServerRoot();
        this.mainServerIp = configReader.getMainServerIp();
        this.mainServerPort = configReader.getMainServerPort();
    }

    public static void main(String args[]) {
        Client client = new Client();
        client.clientConsole();
    }

    private void helpMessage() {
        System.out.println("\n### Legenda ##################################################################################\n" +
                "## start                               -       Start connection with main server\n" +
                "## pwd                                 -       Print full path to current directory\n" +
                "## ls <dirname>                        -       List <abs_path> content\n" +
                "## cat <fname>                        -       Read content of file (abs path)\n" +
                "## write <fname>                       -       Write a new file (ovewrite abs path)\n" +
                "## cp <fname_source> <fname_dest>      -       Copy file from source to dest (abs path)\n" +
                "## mv <fname_source> <fname_dest>      -       Move file from source ro dest (abs path)\n" +
                "## rm <fname>                          -       Delete file\n" +
                "## mkdir <path>                        -       Create Directory\n" +
                "## edit <fname>                        -       Edit File with default application\n" +
                "## help                                -       Show this message\n" +
                "#############################################################################################\n\n");
    }

    private void logout() {
        if (this.connectionStatus) {
            this.connectionStatus = false;
            this.pwd = "";
            this.connectionInfo = "no connection";
            System.out.println("Successfully logged out from FDFS Server: " + this.mainServerIp);
        } else {
            System.exit(0);
        }
    }

    private void clientConsole() {
        System.out.println("#################\n" +
                "# Welcome to FDFS\n" +
                "#################\n");

        this.helpMessage();

        boolean exitedSubProgram = false;
        Scanner scanner = new Scanner(System.in);
        //BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        Scanner systemIn = new Scanner(System.in);
        String line = "";
        boolean firstRun = true;

        while (true) {
            System.out.printf("[%s%s]$ ", this.connectionInfo, this.pwd);
            line = systemIn.nextLine();
            String path;
            String[] lineSplit = line.split(" ", 2);
            String command = lineSplit[0];
            String argument = null;


            if (lineSplit.length >= 2) {
                argument = lineSplit[1];
            }

            switch (command) {
                case "start":
                    if (connectionStatus) this.logout();
                    if (argument != null) {
                        this.mainServerIp = this.getAbsolutePath(argument);
                    }
                    try {
                        this.registry = LocateRegistry.getRegistry(this.mainServerIp, this.mainServerPort);
                        this.fileServer = (FileServer) this.registry.lookup("MainServer");
                        this.connectionStatus = true;
                        this.pwd = this.mainRoot;
                        this.connectionInfo = System.getProperty("user.name") + "@FDFS ";

                        System.out.println("Connection successfully made with the main server");
                    } catch (NotBoundException | IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "exit":
                    this.logout();
                    break;
                case "help":
                    this.helpMessage();
                    break;
            }
            if (this.connectionStatus && this.fileServer != null) {
                try {
                    switch (command) {
                        case "pwd":
                            System.out.printf(":%s\n", this.pwd);
                            break;
                        case "ls":
                            if (argument != null) {
                                path = this.getAbsolutePath(argument);
                            } else {
                                path = this.pwd;
                            }
                            List<String> files = this.fileServer.listFolderContent(path);
                            if (files == null) {
                                throw new FileNotFoundException(path + " (No such file or directory)");
                            } else {
                                for (String file : files) {
                                    System.out.println(file);
                                }
                            }
                            break;
                        case "cd":
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify a valid path");
                            } else {
                                switch (argument) {
                                    case ".":
                                        break;
                                    case "..":
                                        int indexDir = this.pwd.lastIndexOf('/');
                                        if (indexDir == -1) {
                                            System.out.println("You are already at root");
                                        } else {
                                            this.pwd = this.pwd.substring(0, indexDir);
                                        }
                                        break;
                                    default:
                                        String dir = getAbsolutePath(argument);
                                        if (this.fileServer.isDir(dir)) {
                                            this.pwd = dir;
                                        } else
                                            throw new CommandArgumentNeededException("You need to specify a valid path");
                                        break;
                                }

                            }
                            break;
                        case "cat":
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify a filename");
                            } else {
                                String filename = this.getAbsolutePath(argument);
                                String readLine = this.fileServer.readLine(filename);
                                while (readLine != null) {
                                    System.out.println(readLine);
                                    readLine = this.fileServer.readLine(filename);
                                }
                            }
                            break;
                        case "write":
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify a filename");
                            } else {
                                String filename = this.getAbsolutePath(argument);
                                FDFSWriter fdfsWriter = new FDFSWriter();
                                List<String> content = fdfsWriter.getContent();
                                this.fileServer.writeFile(filename, content);
                            }
                            break;
                        case "cp":
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify fname source and dest");
                            } else {
                                String[] arguments = argument.split(" ", 2);
                                if (arguments.length < 2) {
                                    throw new CommandArgumentNeededException("You need to specify fname source and dest. BOTH");
                                } else {
                                    String fname_source = this.getAbsolutePath(arguments[0]);
                                    String fname_dest = this.getAbsolutePath(arguments[1]);

                                    this.fileServer.copyFile(fname_source, fname_dest);
                                }
                            }
                            break;
                        case "rm":
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify fname source and dest");
                            } else {
                                String filename = this.getAbsolutePath(argument);
                                this.fileServer.deleteFile(filename);
                            }
                            break;
                        case "mv":
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify fname source and dest");
                            } else {
                                String[] arguments = argument.split(" ", 2);
                                if (arguments.length < 2) {
                                    throw new CommandArgumentNeededException("You need to specify fname source and dest. BOTH");
                                } else {
                                    String fname_source = this.getAbsolutePath(arguments[0]);
                                    String fname_dest = this.getAbsolutePath(arguments[1]);
                                    this.fileServer.moveFile(fname_source, fname_dest);
                                }
                            }
                            break;
                        case "edit":
                            String editor = "nano";
                            String filename = null;
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify fname");
                            } else {
                                String[] arguments = argument.split(" ", 2);
                                if (arguments.length == 2) {
                                    if (arguments[0].contains("-editor=")) {
                                        editor = arguments[0].replace("-editor=", "");
                                    }
                                    filename = this.getAbsolutePath(arguments[1]);
                                } else {
                                    filename = this.getAbsolutePath(arguments[0]);
                                }

                                byte[] fileBytes = this.fileServer.getFile(filename);

                                String fname = this.getFileName(filename);
                                String tmpFname = "tmp/" + fname + "." + UUID.randomUUID().toString();
                                File tmpFile = new File(tmpFname);
                                FileOutputStream tmpOutputStream = new FileOutputStream(tmpFile);
                                tmpOutputStream.write(fileBytes);
                                tmpOutputStream.close();


                                System.out.println("STARTING " + editor);
                                ProcessBuilder processBuilder = new ProcessBuilder(editor, tmpFname);
                                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                                processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

                                Process p = processBuilder.start();
                                // wait for termination.
                                p.waitFor();
                                System.out.println("Exiting " + editor);

                                this.fileServer.writeFileBytes(Files.readAllBytes(tmpFile.toPath()), filename);

                            }
                            break;
                        case "mkdir":
                            if (argument == null) {
                                throw new CommandArgumentNeededException("You need to specify fname");
                            } else{
                                String dir = getAbsolutePath(argument);
                                if(this.fileServer.createDir(dir)){
                                    System.out.println(dir + " Successfully created");
                                } else {
                                    System.out.println(dir + " Could not be created");
                                }
                            }
                            break;
                            /*
                        case "regshow":
                            //this.fileServer.getRegistry("pippo");
                            System.out.println(Arrays.toString(this.registry.list()));
                            break;
                            */
                        case "start":
                        case "exit":
                        case "help":
                            break;
                        default:
                            throw new InvalidCommandException(command + "(Invalid Command. Please Insert a valid command or " +
                                    "type \"help\" for a list of avaible commands ");
                    }
                } catch (CommandArgumentNeededException | FileNotFoundException | NoSuchFileException e) {
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }

    }

    private String getAbsolutePath(String argument) {
        if (argument.startsWith(":")) return argument.substring(1);
        else return this.pwd.concat("/").concat(argument);
    }

    private String getFileName(String file) {
        int index = file.lastIndexOf('/');
        return file.substring(index + 1);
    }
}
