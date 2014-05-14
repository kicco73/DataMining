package com.mycompany.dataminingproject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import weka.core.CosineDistance;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.GridAddLatLng;
import weka.filters.unsupervised.attribute.GridJoin;
import weka.filters.unsupervised.attribute.GridNormalize;
import weka.filters.unsupervised.attribute.MakeBins;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToNominal;
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
    static int numClusters;
    static MakeBins.Period period;

    private void loadProps(String propertiesName) throws ParseException {
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
            period = MakeBins.Period.LINEAR;
            for(MakeBins.Period p: MakeBins.Period.values()) 
                if(p.name().equalsIgnoreCase(config.getProperty("makeBins.period")))
                    period = p;
            numBins = Integer.parseInt(config.getProperty("makeBins.numBins", "1"));
            numClusters = Integer.parseInt(config.getProperty("numClusters", "3"));
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

    public static Instances extractFeatures(Instances dataSet) throws Exception {
        // Crea bin temporali
        MakeBins binMaker = new MakeBins();
        binMaker.setPeriod(MakeBins.Period.LINEAR);
        binMaker.setMinDate(minDate);
        binMaker.setMaxDate(maxDate);
        binMaker.setNumBins(numBins);
        binMaker.setPeriod(period);
        binMaker.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, binMaker);
        // Normalizza
        GridNormalize normalizer = new GridNormalize();
        normalizer.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, normalizer);
        return dataSet;
    }
    
    public static Instances joinFeatures(Instances pickUps, Instances dropOffs) throws Exception {
        GridJoin joinAndFillMissing = new GridJoin();
        joinAndFillMissing.setComplementaryDataSet(dropOffs);
        joinAndFillMissing.setInputFormat(pickUps);
        Instances dataSet = Filter.useFilter(pickUps, joinAndFillMissing);
        // Aggiunge coordinate geografiche
        GridAddLatLng mapGridToGps = new GridAddLatLng();
        mapGridToGps.setNWLocation(nwLat, nwLng);
        mapGridToGps.setCell(cellXSizeInMeters, cellYSizeInMeters);
        mapGridToGps.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, mapGridToGps);        
        // Trasforma stringhe in nominali
        StringToNominal stringToNominal = new StringToNominal();
        stringToNominal.setAttributeRange("first-last");
        stringToNominal.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, stringToNominal);        
        return dataSet;
    }
    
    private int[] kMeans(DistanceFunction df, Instances finalFeatures) throws Exception {
        SimpleKMeans simpleKMeans = new SimpleKMeans();
        // There's a bug in the default random initializer, so we use 1 = kmeans++
        String options[] = {"-init", "1"};
        simpleKMeans.setOptions(options);
        simpleKMeans.setNumClusters(numClusters);
        simpleKMeans.setDistanceFunction(df);
        simpleKMeans.setPreserveInstancesOrder(true);
        simpleKMeans.buildClusterer(finalFeatures);
        return simpleKMeans.getAssignments();
    }
    
    
    private int[] agglomerative(DistanceFunction df, Instances finalFeatures) throws Exception {
        HierarchicalClusterer clusterer = new HierarchicalClusterer();
        clusterer.setNumClusters(numClusters);
        clusterer.setDistanceFunction(df);
        clusterer.buildClusterer(finalFeatures);
        int [] assignments = new int[finalFeatures.numInstances()];
        for(int i = 0; i < finalFeatures.numInstances(); i++)
            assignments[i] = clusterer.clusterInstance(finalFeatures.instance(i));
        return assignments;
    }
    
    public void run(String configName) throws Exception {
        loadProps(configName);

        // Load first set and create conditioned features
        
        Instances pickUps = loadCsv(pickupsFileName);
        pickUps = cleanData(pickUps);
        pickUps = extractFeatures(pickUps);
        
        // Load second set and create conditioned features
        
        Instances dropOffs = loadCsv(dropoffsFileName);
        dropOffs = cleanData(dropOffs);
        dropOffs = extractFeatures(dropOffs);

        // Join features into one bigger set

        Instances finalFeatures = joinFeatures(pickUps, dropOffs);

        // Clustering with different algorithms and distance functions
        
        int kMeansAssignments[] = kMeans(new EuclideanDistance(), finalFeatures);
        int kMeansCosineAssignments[] = kMeans(new CosineDistance(), finalFeatures);
        int agglomerativeAssignments[] = agglomerative(new EuclideanDistance(), finalFeatures);
        int agglomerativeCosineAssignments[] = agglomerative(new CosineDistance(), finalFeatures);
        
        // Add clustering results to dataset
        
        Attribute kMeansClusterId = new Attribute("kMeansEuclidean", 0);
        finalFeatures.insertAttributeAt(kMeansClusterId, 0);
        Attribute kMeansCosineClusterId = new Attribute("kMeansCosine", 1);
        finalFeatures.insertAttributeAt(kMeansCosineClusterId, 1);
        Attribute agglomerativeId = new Attribute("agglomerativeEuclidean", 2);
        finalFeatures.insertAttributeAt(agglomerativeId, 2);
        Attribute agglomerativeCosineId = new Attribute("agglomerativeCosine", 3);
        finalFeatures.insertAttributeAt(agglomerativeCosineId, 3);

        for(int i = 0; i < finalFeatures.numInstances(); i++) {
            Instance instance = finalFeatures.instance(i);
            instance.setValue(kMeansClusterId, kMeansAssignments[i]);
            instance.setValue(kMeansCosineClusterId, kMeansCosineAssignments[i]);
            instance.setValue(agglomerativeId, agglomerativeAssignments[i]);
            instance.setValue(agglomerativeCosineId, agglomerativeCosineAssignments[i]);
        }
        
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
