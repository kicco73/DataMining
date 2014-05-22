package com.mycompany.dataminingproject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import weka.clusterers.ClusterEvaluationEx;
import weka.core.CosineDistance;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
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
    static int binsInADay;
    static Date minDate;
    static Date maxDate;
    static String pickupsFileName;
    static String dropoffsFileName;
    static String csvOutCleanPickUpFileName;
    static String csvOutCleanDropOffFileName;
    static String csvOutExtractFileName;
    static String csvOutFileName;
    static String arffOutFileName;
    static int numClusters;
    static MakeBins.Period period;
    static boolean additive;
    private ClusterEvaluationEx bestClusterer = null;
    private int classIndex = -1;

    private void loadProps(InputStream is) throws ParseException {
        Properties config;

        try {
            config = new Properties();
            config.load(is);
            is.close();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            pickupsFileName = getClass().getResource("/resources/"+config.getProperty("pickupsFileName")).getFile();
            dropoffsFileName = getClass().getResource("/resources/"+config.getProperty("dropoffsFileName")).getFile();
            csvOutFileName = config.getProperty("csvOutFileName");
            arffOutFileName = config.getProperty("arffOutFileName");
            csvOutCleanPickUpFileName = config.getProperty("csvOutCleanPickUpFileName");
            csvOutCleanDropOffFileName = config.getProperty("csvOutCleanDropOffFileName");
            csvOutExtractFileName = config.getProperty("csvOutExtractFileName");

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
            binsInADay = Integer.parseInt(config.getProperty("makeBins.binsInADay", "1"));
            numClusters = Integer.parseInt(config.getProperty("numClusters", "3"));
            additive = Boolean.parseBoolean(config.getProperty("gridJoin.additive", "false"));
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
        binMaker.setMinDate(minDate);
        binMaker.setMaxDate(maxDate);
        binMaker.setBinsInADay(binsInADay);
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
        joinAndFillMissing.setAdditive(additive);
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
    
    private ClusterEvaluationEx kMeans(DistanceFunction df, Instances finalFeatures, int index) throws Exception {
        SimpleKMeans simpleKMeans = new SimpleKMeans();
        // There's a bug in the default random initializer, so we use 1 = kmeans++
        String options[] = {"-init", "1"};
        simpleKMeans.setOptions(options);
        simpleKMeans.setNumClusters(numClusters);
        simpleKMeans.setDistanceFunction(df);
        simpleKMeans.setPreserveInstancesOrder(true);
        simpleKMeans.buildClusterer(finalFeatures);
        ClusterEvaluationEx ce = new ClusterEvaluationEx();
        ce.setClusterer(simpleKMeans);
        ce.evaluateClusterer(new Instances(finalFeatures), df);
        if(bestClusterer == null || bestClusterer.getAvgSilhouetteCoefficient() < ce.getAvgSilhouetteCoefficient()) {
            bestClusterer = ce;
            classIndex = index;
        }
        return ce;
    }
    
    private ClusterEvaluationEx agglomerative(DistanceFunction df, Instances finalFeatures, int index) throws Exception {
        HierarchicalClusterer clusterer = new HierarchicalClusterer();
        clusterer.setNumClusters(numClusters);
        clusterer.setDistanceFunction(df);
        clusterer.buildClusterer(finalFeatures);
        ClusterEvaluationEx ce = new ClusterEvaluationEx();
        ce.setClusterer(clusterer);
        ce.evaluateClusterer(new Instances(finalFeatures), df);
        if(bestClusterer == null || bestClusterer.getAvgSilhouetteCoefficient() < ce.getAvgSilhouetteCoefficient()) {
            bestClusterer = ce;
            classIndex = index;
        }
        return ce;
    }
    
    private ClusterEvaluationEx kMeansEval(DistanceFunction df, Instances finalFeatures) throws Exception {
        SimpleKMeans simpleKMeans = new SimpleKMeans();
        // There's a bug in the default random initializer, so we use 1 = kmeans++
        String options[] = {"-init", "1"};
        simpleKMeans.setOptions(options);
        simpleKMeans.setNumClusters(numClusters);
        simpleKMeans.setDistanceFunction(df);
        simpleKMeans.setPreserveInstancesOrder(true);
        Remove remove = new Remove();
        int removeArray[] = {finalFeatures.classIndex()};
        remove.setAttributeIndicesArray(removeArray);
        remove.setInputFormat(finalFeatures);
        Instances data = Filter.useFilter(finalFeatures, remove);
        simpleKMeans.buildClusterer(data);
        ClusterEvaluationEx ce = new ClusterEvaluationEx();
        ce.setClusterer(simpleKMeans);
        ce.evaluateClusterer(finalFeatures);
        return ce;
    }
    
    private ClusterEvaluationEx agglomerativeEval(DistanceFunction df, Instances finalFeatures) throws Exception {
        HierarchicalClusterer clusterer = new HierarchicalClusterer();
        clusterer.setNumClusters(numClusters);
        clusterer.setDistanceFunction(df);
        Remove remove = new Remove();
        int removeArray[] = {finalFeatures.classIndex()};
        remove.setAttributeIndicesArray(removeArray);
        remove.setInputFormat(finalFeatures);
        Instances data = Filter.useFilter(finalFeatures, remove);
        clusterer.buildClusterer(data);
        ClusterEvaluationEx ce = new ClusterEvaluationEx();
        ce.setClusterer(clusterer);
        ce.evaluateClusterer(finalFeatures);
        return ce;
    }
    
    public void process() throws Exception {
        // Load first set and create conditioned features
        bestClusterer = null;
        classIndex = -1;
        Instances pickUps = loadCsv(pickupsFileName);
        pickUps = cleanData(pickUps);
        saveCsv(csvOutCleanPickUpFileName, pickUps);
        pickUps = extractFeatures(pickUps);
        
        // Load second set and create conditioned features
        
        Instances dropOffs = loadCsv(dropoffsFileName);
        dropOffs = cleanData(dropOffs);
        saveCsv(csvOutCleanDropOffFileName, dropOffs);
        dropOffs = extractFeatures(dropOffs);

        // Join features into one bigger set

        Instances finalFeatures = joinFeatures(pickUps, dropOffs);
        saveCsv(csvOutExtractFileName, finalFeatures);

        // Clustering with different algorithms and distance functions
        
        System.out.println("*** KMeans with euclidean distance");
        ClusterEvaluationEx kMeansEuclidean = kMeans(new EuclideanDistance(), finalFeatures, 0);
        System.out.println("*** Agglomerative with euclidean distance");
        ClusterEvaluationEx agglomerativeEuclidean = agglomerative(new EuclideanDistance(), finalFeatures, 1);
        System.out.println("*** KMeans with cosine distance");
        ClusterEvaluationEx kMeansCosine = kMeans(new CosineDistance(), finalFeatures, 2);
        System.out.println("*** Agglomerative with cosine distance");
        ClusterEvaluationEx agglomerativeCosine = agglomerative(new CosineDistance(), finalFeatures, 3);
        
        // Add clustering results to dataset
        
        FastVector fv = new FastVector(numClusters);
        for(int i = 0; i < numClusters; i++)
            fv.addElement(Integer.toString(i));
        Attribute kMeansClusterId = new Attribute("kMeansEuclidean", fv);
        finalFeatures.insertAttributeAt(kMeansClusterId, 0);
        Attribute agglomerativeId = new Attribute("agglomerativeEuclidean", fv);
        finalFeatures.insertAttributeAt(agglomerativeId, 1);
        Attribute kMeansCosineClusterId = new Attribute("kMeansCosine", fv);
        finalFeatures.insertAttributeAt(kMeansCosineClusterId, 2);
        Attribute agglomerativeCosineId = new Attribute("agglomerativeCosine", fv);
        finalFeatures.insertAttributeAt(agglomerativeCosineId, 3);
        
        for(int i = 0; i < finalFeatures.numInstances(); i++) {
            Instance instance = finalFeatures.instance(i);
            instance.setValue(0, Integer.toString((int)kMeansEuclidean.getClusterAssignments()[i]));
            instance.setValue(1, Integer.toString((int)agglomerativeEuclidean.getClusterAssignments()[i]));
            instance.setValue(2, Integer.toString((int)kMeansCosine.getClusterAssignments()[i]));
            instance.setValue(3, Integer.toString((int)agglomerativeCosine.getClusterAssignments()[i]));
        }
        
        // Repeat to perform Classes To Cluster Analysis
        
        System.out.println("Best clusterer: "+bestClusterer);
        
        finalFeatures.setClassIndex(classIndex);
        System.out.println("*** KMeans with euclidean distance:"+
            kMeansEval(new EuclideanDistance(), finalFeatures).clusterResultsToString());
        System.out.println("*** Agglomerative with euclidean distance:"+
            agglomerativeEval(new EuclideanDistance(), finalFeatures).clusterResultsToString());
        System.out.println("*** KMeans with cosine distance:"+
            kMeansEval(new CosineDistance(), finalFeatures).clusterResultsToString());
        System.out.println("*** Agglomerative with cosine distance:"+
            agglomerativeEval(new CosineDistance(), finalFeatures).clusterResultsToString());
        
        // Export to filesystem

        saveCsv(csvOutFileName, finalFeatures);
        saveArff(arffOutFileName, finalFeatures);
    }

    public void run(String args[]) throws Exception {
        
        if(args.length == 0) {
            InputStream is = getClass().getResourceAsStream("/resources/firstRun.properties");
            loadProps(is);
            process();
            is = getClass().getResourceAsStream("/resources/secondRun.properties");
            loadProps(is);
            process();
        } else {
            InputStream is = new FileInputStream(args[0]);
            loadProps(is);
            process();
        }
    }

    public static void main(String[] args) throws Exception {
        App a = new App();
        a.run(args);
    }
}
