package org.project;

import java.io.*;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Main {
    static final int MAX_SERIES_SIZE = 100000;

    public static void main(String[] args) throws IOException {
        File sourceFile = generateInputFile(200000);
        List<File> auxFiles = createAuxFiles();
        clearAuxFiles(auxFiles);
        LocalTime n1 = LocalTime.now();
        polyphaseMergeSort(sourceFile, auxFiles);
        LocalTime n2 = LocalTime.now();
        System.out.println(ChronoUnit.SECONDS.between(n1, n2));
    }

    public static void distributeRuns(File source, List<File> auxFiles) throws IOException {
        //Підрахунок серій у файлі та визначення оптимального числа Фібоначчі для дистрибуції
        int totalNumberOfRuns = countRuns(source);
        int index = findFibonacciIndex(totalNumberOfRuns);
        int runsInFirstFile = fib(index - 1);
        int runsInSecondFile = fib(index - 2);

        //Запис серій з файлу у окремий список
        List<Integer> run = new ArrayList<>();

        try (BufferedWriter bw1 = new BufferedWriter(new FileWriter(auxFiles.getFirst()));
             BufferedWriter bw2 = new BufferedWriter(new FileWriter(auxFiles.get(1)));
             BufferedReader br = new BufferedReader(new FileReader(source)))
        {
            readAndWrite(br, bw1, runsInFirstFile, run, false);
            readAndWrite(br, bw2, runsInSecondFile, run, true);
        }
    }

    public static void polyPhaseMerge(File file1, File file2, File outputFile) throws IOException {
        while (true) {
            try (BufferedReader br1 = new BufferedReader(new FileReader(file1));
                 BufferedReader br2 = new BufferedReader(new FileReader(file2))) {

                boolean hasMoreData = true;
                int count = 0;
                while (hasMoreData) {
                    List<Integer> series1 = readNextRun(br1);
                    List<Integer> series2 = readNextRun(br2);
                    count++;

                    if (series1 .isEmpty() && series2.isEmpty()) {
                        clearFile(file1);
                        clearFile(file2);
                        break;
                    }
                    else if (series1.isEmpty() || series2.isEmpty()) {
                        hasMoreData = false;
                        if (series1.isEmpty()) {
                            clearFile(file1);
                            file2 = residualRuns(br2, file2.getName(), series2);
                        } else {
                            clearFile(file2);
                            file1 = residualRuns(br1, file1.getName(), series1);
                        }
                        break;
                    }

                    // Злиття серій
                    List<Integer> mergedSeries = new ArrayList<>();
                    mergedSeries.addAll(series1);
                    mergedSeries.addAll(series2);
                    Collections.sort(mergedSeries);
                    if (mergedSeries.size() > 1 && mergedSeries.contains(Integer.MAX_VALUE)) {
                        mergedSeries.remove(mergedSeries.indexOf(Integer.MAX_VALUE));
                    }
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, true))){
                        for (int num : mergedSeries) {
                            bw.write(num + "\n");
                        }
                        bw.write("\n"); // Новий ряд для розділення серій
                    }
                }
            }

            // Перевіряємо, чи обидва файли порожні
            boolean file1Empty = isEmpty(file1);
            boolean file2Empty = isEmpty(file2);

            if (file1Empty && file2Empty) {
                break; // Виходимо, якщо обидва файли порожні
            } else if (file1Empty) {
                // Якщо file1 порожній, перемикаємо file2 як новий файл
                File temp = file1;
                file1 = outputFile;
                outputFile = temp;
            } else if (file2Empty) {
                // Якщо file2 порожній, перемикаємо file1 як новий файл
                File temp = file2;
                file2 = outputFile;
                outputFile = temp;
            }
        }
    }

    public static void polyphaseMergeSort(File sourceFile, List<File> auxFiles) throws IOException {
        distributeRuns(sourceFile, auxFiles);
        polyPhaseMerge(auxFiles.getFirst(), auxFiles.get(1), auxFiles.get(2));
    }

    private static File residualRuns(BufferedReader br, String fileName, List<Integer> firstRun) throws IOException {
        List<List<Integer>> runs = new ArrayList<>();
        List<Integer> currentRun = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                currentRun.add(Integer.parseInt(line));
            }
            else {
                runs.add(new ArrayList<>(currentRun));
                currentRun.clear();
            }
        }
        if (!currentRun.isEmpty()) {
            runs.add(new ArrayList<>(currentRun));
        }

        File outputFile = new File(fileName);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            for (int run : firstRun) {
                bw.write(run + "\n");
            }
            bw.write("\n");
            for (List<Integer> run : runs) {
                for (int num : run) {
                    bw.write(num + "\n");
                }
                bw.write("\n");
            }
        }
        return outputFile;
    }

    private static List<Integer> readNextRun(BufferedReader reader) throws IOException {
        List<Integer> series = new ArrayList<>();
        String line;

        // Зчитування серії
        while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
            series.add(Integer.parseInt(line));
        }

        return series;
    }

    public static int fib(int n) {
        if (n == 1) {
            return 0;
        }
        if (n == 2) {
            return 1;
        }
        return fib(n - 1) + fib(n - 2);
    }

    public static int findFibonacciIndex(int numberOfRuns) {
        int i = 1;
        while (numberOfRuns > fib(i)) {
            i++;
        }
        return i;
    }

    public static int countRuns(File source) throws IOException {
        int count = 0;
        String line;
        int lastNum = Integer.MIN_VALUE;
        int currentNum;
        try (BufferedReader br = new BufferedReader(new FileReader(source))) {
            while ((line = br.readLine()) != null) {
                currentNum = Integer.parseInt(line);
                if (currentNum < lastNum) {
                    count++;
                }
                lastNum = currentNum;
            }
            count++;
        }
        return count;
    }

    public static void readAndWrite(BufferedReader br, BufferedWriter bw, int numOfRuns, List<Integer> currentRun, boolean lastCall) throws IOException {
        String line = "";
        int num, counter = 0;
        while (counter < numOfRuns && (line = br.readLine()) != null) {
            num = Integer.parseInt(line);
            if ((currentRun.isEmpty() || currentRun.getLast() <= num) && currentRun.size() < MAX_SERIES_SIZE) {
                currentRun.add(num);
            }
            else {
                for (int run : currentRun) {
                    bw.write(run + "\n");
                }
                bw.write("\n");
                currentRun.clear();
                currentRun.add(num);
                counter++;
            }
        }
        if (lastCall) {
            if (!currentRun.isEmpty()) {
                for (int n : currentRun) {
                    bw.write(n + "\n");
                }
                bw.write("\n");
            }
        }
        if (counter < numOfRuns) {
            while (counter < numOfRuns - 1) {
                bw.write(Integer.MAX_VALUE + "\n" + "\n");
                counter++;
            }
        }
    }

    private static boolean isEmpty(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return br.readLine() == null;
        }
    }

    private static void clearFile(File file) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("");
        }
    }

    private static File generateInputFile(int size) throws IOException {
        Random rand = new Random();
        File inputFile = new File("input.txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(inputFile)))
        {
            for (int i = 0; i < size; i++) {
                bw.write(rand.nextInt(4000000) + "\n");
            }
        }
        return inputFile;
    }

    private static List<File> createAuxFiles() {
        return new ArrayList<>(List.of(new File("auxFile1.txt"),
                                       new File("auxFile2.txt"),
                                       new File("auxFile3.txt")));
    }

    private static void clearAuxFiles(List<File> auxFiles) throws IOException {
        if (auxFiles.getFirst().length() != 0) {
            clearFile(auxFiles.getFirst());
        }
        if (auxFiles.getLast().length() != 0) {
            clearFile(auxFiles.getLast());
        }
        if (auxFiles.get(1).length() != 0) {
            clearFile(auxFiles.get(1));
        }
    }
}