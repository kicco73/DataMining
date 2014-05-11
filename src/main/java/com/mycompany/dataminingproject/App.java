package com.mycompany.dataminingproject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.JoinAndFillMissing;
import weka.filters.unsupervised.attribute.MakeBins;
import weka.filters.unsupervised.attribute.GridAddLatLng;
import weka.filters.unsupervised.attribute.NormalizeGrid;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.GridGpsArea;
import weka.filters.unsupervised.instance.RemoveDuplicates;

public class App {

    static double nwLat;
    static double seLat;
    static double nwLng;
    static double seLng;
    static double cellXSizeInMeters;
    static double cellYSizeInMeters;
    static int numBins;
    static Date minDate;
    static Date maxDate;
    static String pickupsFileName;
    static String dropoffsFileName;
    static String csvOutFileName;
    static String arffOutFileName;

    private void loadProps(String propertiesName) {
        Properties config;

        try {
            InputStream is = getClass().getResourceAsStream(propertiesName);
            config = new Properties();
            config.load(is);
            is.close();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            pickupsFileName = getClass().getResource("/resources/"+config.getProperty("pickupsFileName")).getFile();
            dropoffsFileName = getClass().getResource("/resources/"+config.getProperty("dropoffsFileName")).getFile();
            csvOutFileName = config.getProperty("csvOutFileName");
            arffOutFileName = config.getProperty("arffOutFileName");

            nwLat = Double.parseDouble(config.getProperty("gridGpsArea.nwLat"));
            nwLng = Double.parseDouble(config.getProperty("gridGpsArea.nwLng"));
            seLat = Double.parseDouble(config.getProperty("gridGpsArea.seLat"));
            seLng = Double.parseDouble(config.getProperty("gridGpsArea.seLng"));
            cellXSizeInMeters = Double.parseDouble(config.getProperty("gridGpsArea.cellXSizeInMeters"));
            cellYSizeInMeters = Double.parseDouble(config.getProperty("gridGpsArea.cellYSizeInMeters"));
            minDate = format.parse(config.getProperty("makeBins.minDate"));
            maxDate = format.parse(config.getProperty("makeBins.maxDate"));
            numBins = Integer.parseInt(config.getProperty("makeBins.numBins", "1"));
        } catch (ParseException ioe) {
            System.err.println("ParseException in loadProps");
        } catch (IOException ioe) {
            System.err.println("IOException in loadProps");
        }
    }
    
    public static Instances loadCsv(String fileName) throws IOException {
        File file = new File(fileName);
        CSVLoader cl = new CSVLoader();
        cl.setFile(file);
        return cl.getDataSet();
    }

    
    public static void saveCsv(String fileName, Instances finalFeatures) throws IOException {
        File out = new File(fileName);
        CSVSaver saver = new CSVSaver();
        saver.setFile(out);
        saver.setInstances(finalFeatures);
        saver.writeBatch();
        System.out.println("*** CSV SAVED");
    }
    
    public static void saveArff(String fileName, Instances finalFeatures) throws IOException {
        File out = new File(fileName);
        ArffSaver saver = new ArffSaver();
        saver.setFile(out);
        saver.setInstances(finalFeatures);
        saver.writeBatch();
        System.out.println("*** ARFF SAVED");
    }
    
    public static Instances cleanData(Instances rawDataSet) throws Exception {
        // Filtra campioni doppi
        RemoveDuplicates removeDuplicateFilter = new RemoveDuplicates();
        removeDuplicateFilter.setInputFormat(rawDataSet);
        Instances dataSet = Filter.useFilter(rawDataSet, removeDuplicateFilter);

        // Filtra regione geografica di interesse entro certe coordinate
        // e aggiunge etichetta relativa alla regione di appartenenza.
        GridGpsArea cropGpsAreaFilter = new GridGpsArea();
        cropGpsAreaFilter.setArea(nwLat, nwLng, seLat, seLng);
        cropGpsAreaFilter.setCell(cellXSizeInMeters, cellYSizeInMeters);
        cropGpsAreaFilter.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, cropGpsAreaFilter);

        // Elimina attributi inutili
        int[] removeIndices = {0, 5};
        Remove removeFilter = new Remove();
        removeFilter.setAttributeIndicesArray(removeIndices);
        removeFilter.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, removeFilter);
        return dataSet;
    }

    public static Instances createFeatures(Instances dataSet) throws Exception {
        MakeBins binMaker = new MakeBins();
        binMaker.setPeriod(MakeBins.Period.LINEAR);
        binMaker.setMinDate(minDate);
        binMaker.setMaxDate(maxDate);
        binMaker.setNumBins(numBins);
        binMaker.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, binMaker);
        NormalizeGrid normalizer = new NormalizeGrid();
        normalizer.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, normalizer);
        return dataSet;
    }
    
    public static Instances joinFeatures(Instances pickUps, Instances dropOffs) throws Exception {
        JoinAndFillMissing joinAndFillMissing = new JoinAndFillMissing();
        joinAndFillMissing.setComplementaryDataSet(dropOffs);
        joinAndFillMissing.setInputFormat(pickUps);
        Instances dataSet = Filter.useFilter(pickUps, joinAndFillMissing);
        GridAddLatLng mapGridToGps = new GridAddLatLng();
        mapGridToGps.setArea(nwLat, nwLng, seLat, seLng);
        mapGridToGps.setCell(cellXSizeInMeters, cellYSizeInMeters);
        mapGridToGps.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, mapGridToGps);
        return dataSet;
    }
    
    public void run(String configName) throws Exception {
        loadProps(configName);

        // Load first set and create conditioned features
        
        Instances pickUps = loadCsv(pickupsFileName);
        pickUps = cleanData(pickUps);
        pickUps = createFeatures(pickUps);
        
        // Load second set and create conditioned features
        
        Instances dropOffs = loadCsv(dropoffsFileName);
        dropOffs = cleanData(dropOffs);
        dropOffs = createFeatures(dropOffs);

        // Join features into one bigger set

        Instances finalFeatures = joinFeatures(pickUps, dropOffs);

        // Export to filesystem
        
        saveCsv(csvOutFileName, finalFeatures);
        saveArff(arffOutFileName, finalFeatures);
    }

    public static void main(String[] args) throws Exception {
        App a = new App();
        String configName = "/resources/config.properties";
        a.run(configName);
    }
}
