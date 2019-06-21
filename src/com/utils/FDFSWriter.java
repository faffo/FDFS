package com.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to create a simple program of video writing
 */
public class FDFSWriter implements Runnable{
    private List<String> content = new ArrayList<>();

    /**
     * Constructor of the video writer. It also provides a really slim console interface
     */
    public FDFSWriter() {
        System.out.println("#### FDFS File Writer" +
                "## Write the desired text" +
                "## Type ':EOF:' to exit");
        try {
        BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String line;
        this.content = new ArrayList<>();
        while(true){
                if ((line = systemIn.readLine()).equals(":EOF:")) break;
                else this.content.add(line);
        }} catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
    }

    /**
     * Get the content written with FDFSWriter
     * @return The contente written
     */
    public List<String> getContent() {
        return content;
    }
}
