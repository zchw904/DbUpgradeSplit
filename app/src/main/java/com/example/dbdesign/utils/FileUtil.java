package com.example.dbdesign.utils;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
    /**
     * if File not exist return null
     * @param path file path include file name
     * @return String
     */
    public static String readFile(String path){
        File file = new File(path);
        StringBuilder sb = new StringBuilder();

        if (file.exists()) {
            FileReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                fileReader = new FileReader(file);
                bufferedReader = new BufferedReader(fileReader);
                String read = null;
                while (true){
                    read = bufferedReader.readLine();
                    if (read == null){
                        continue;
                    }
                    sb.append(read);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bufferedReader != null)
                        bufferedReader.close();
                    if (fileReader != null)
                        fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bufferedReader = null;
                    fileReader = null;
                }
            }
        }

        return sb.length()>0 ? sb.toString() : null;
    }

    /**
     * if path does not exist, create all dirs and create file
     * @param write context
     * @param path file path include file name
     * @param append if true and file not empty, append to line end
     * @return if error exception return false, else true
     */
    public static boolean writeFile(String write, String path, boolean append){
        File file = new File(path);
        FileWriter fileWriter = null;
        byte[]encodingBytes = null;
        BufferedWriter bufferedWriter = null;
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (!parent.exists())
                    parent.mkdirs();
                file.createNewFile();
                fileWriter = new FileWriter(file);
                encodingBytes = write.getBytes("UTF-8");
            } else {
                fileWriter = new FileWriter(file, append);
                encodingBytes = write.getBytes(fileWriter.getEncoding());
            }
            bufferedWriter = new BufferedWriter(fileWriter);
            String newString = new String(encodingBytes, "UTF-8");
            bufferedWriter.write(newString);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (bufferedWriter != null)
                    bufferedWriter.close();
                if (fileWriter != null)
                    fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                bufferedWriter = null;
                encodingBytes = null;
            }
        }
        return true;
    }

    public static boolean copySingleFile(String oldFilePath, String newFilePath) {
        boolean successFlag = false;

        File oldFile = new File(oldFilePath);
        if (oldFile.exists()){

            int byteRead = 0;
            File newFile = new File(newFilePath);
            InputStream inputStream = null;
            FileOutputStream fileOutputStream = null;

            try {
                if (!newFile.exists()) {
                    File parent = newFile.getParentFile();
                    if (!parent.exists())
                        parent.mkdirs();
                    newFile.createNewFile();
                }
                inputStream = new FileInputStream(oldFile);
                fileOutputStream = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];
                while ((byteRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, byteRead);
                }
                fileOutputStream.flush();
                successFlag = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                    if (fileOutputStream != null)
                        fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    inputStream = null;
                    fileOutputStream = null;
                }
            }
        }
        return successFlag;
    }

    public static void deleteFile(String backupPath) {
        File file = new File(backupPath);
        if (file.exists()) {
            file.delete();
        }
    }
}
