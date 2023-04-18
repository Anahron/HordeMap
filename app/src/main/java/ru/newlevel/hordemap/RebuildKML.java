package ru.newlevel.hordemap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class RebuildKML {
    static File rebuildKML (File inputFile){
        File outputFile = null;
        try {

            //На основе прошлого файла получаем родительский каталог и объявляем новый файл
            outputFile = new File(inputFile.getParent(),"docnew.kml");
            //удаляем если существует(например старая версия)
            if (outputFile.delete()) {
                System.out.println("File deleted successfully.");
            } else {
                System.out.println("Failed to delete file.");
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),"UTF-8")) ;
            //читаем построчно и меняем пути
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("files/")) {
                    line = line.replace("files/", inputFile.getParent() + "/files/" );
                }
                bufferedWriter.write(line + "\n");
            }
            bufferedReader.close();
            bufferedWriter.close();
            System.out.println(outputFile.exists() + "файл существует?");
            System.out.println(outputFile.getAbsolutePath() + " путь к новому файлу");
            System.out.println(outputFile.length() + " размер нового файла");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile;
    }
}
