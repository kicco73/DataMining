package com.mycompany.dataminingproject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.CropGpsArea;
import weka.filters.unsupervised.instance.RemoveDuplicates;

/**
 * Hello world!
 *
 */
public class App {

    static final double minLat = 39.750;
    static final double maxLat = 40.120;
    static final double minLng = 116.130;
    static final double maxLng = 116.650;
    static final double cellXSizeInMeters = 100;
    static final double cellYSizeInMeters = 100;
    static final String pickupsFileName = "pechino_starttime.csv";
    static final String dropoffsFileName = "pechino_endtime.csv";
    static final String outFileName = "outfile.csv";

    private static Instances filterDateRange(Instances dataSet,
            String minDate, String maxDate) {
        int attrIndex = dataSet.attribute("gpsdate").index();
        for (int i = 0; i < dataSet.numInstances();) {
            String attr = dataSet.instance(i).stringValue(attrIndex);
            if (attr.compareTo(minDate) < 0 || attr.compareTo(maxDate) > 0) {
                dataSet.delete(i);
            } else {
                i++;
            }
        }
        return dataSet;
    }

    public static double distanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        final double factor = 7.91959594934121e-06;
        //System.out.println("*** distance in meters: ("+lat1+","+lng1+") - ("+lat2+","+lng2+") = " + Math.sqrt(Math.pow(lat2-lat1, 2)+Math.pow(lng2-lng1, 2))/factor);
        ;
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lng2 - lng1, 2)) / factor;
    }

    public static void populateGrid(Map<String, List<Instance>> grid, Instances inputDataSet) {
        int latIndex = inputDataSet.attribute("latitude").index();
        int lngIndex = inputDataSet.attribute("longitude").index();

        double xSizeInMeters = distanceInMeters(minLat, 0, maxLat, 0);
        double ySizeInMeters = distanceInMeters(0, minLng, 0, maxLng);
        int xCells = (int) Math.ceil(xSizeInMeters / cellXSizeInMeters);
        int yCells = (int) Math.ceil(ySizeInMeters / cellYSizeInMeters);

        System.out.println("*** xSize:  " + xSizeInMeters + "\tySize: " + ySizeInMeters);
        System.out.println("*** xCells: " + xCells + "\tyCells: " + yCells);

        for (int i = 0; i < inputDataSet.numInstances(); i++) {
            Instance instance = inputDataSet.instance(i);
            double lat = instance.value(latIndex);
            double lng = instance.value(lngIndex);
            int x = (int) Math.floor(distanceInMeters(minLat, 0, lat, 0) / cellXSizeInMeters);
            int y = (int) Math.floor(distanceInMeters(0, minLng, 0, lng) / cellYSizeInMeters);
            String key = "(" + x + "," + y + ")";
            List<Instance> cell = grid.get(key);
            if (cell == null) {
                cell = new ArrayList<Instance>();
                grid.put(key, cell);
            }
            cell.add(instance);
            System.out.println("*** " + key + " " + instance);
        }
    }

    public static Instances preprocess(Instances rawDataSet) throws Exception {
        // Filtra regione geografica di interesse entro certe coordinate
        CropGpsArea cropGpsAreaFilter = new CropGpsArea();
        cropGpsAreaFilter.setArea(minLat, minLng, maxLat, maxLng);
        cropGpsAreaFilter.setInputFormat(rawDataSet);
        Instances dataSet = Filter.useFilter(rawDataSet, cropGpsAreaFilter);

        // Filtra campioni doppi
        RemoveDuplicates removeDuplicateFilter = new RemoveDuplicates();
        removeDuplicateFilter.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, removeDuplicateFilter);

        // Elimina attributi inutili 
        int[] removeIndices = {0, 5};
        Remove removeFilter = new Remove();
        removeFilter.setAttributeIndicesArray(removeIndices);
        removeFilter.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, removeFilter);

        // Filtra range di date
        //dataSet = filterDateRange(dataSet, "2008-01-01", "2008-12-31");
        System.out.println("Numero campioni: " + dataSet.numInstances());
        return dataSet;
    }

    public void run(String pickUpName, String dropOffName, String outName) throws Exception {
        System.out.println("*** " + getClass().getResource("/resources/" + pickupsFileName));
        System.out.println(getClass().getResource("/resources/pechino_endtime.csv"));
        if (pickUpName == null) {
            pickUpName = getClass().getResource("/resources/" + pickupsFileName).getFile();
        }
        if (dropOffName == null) {
            dropOffName = getClass().getResource("/resources/" + dropoffsFileName).getFile();
        }
        if (outName == null) {
            outName = "DataMiningOut.csv";
        }

        File file = new File(pickUpName);
        CSVLoader cl = new CSVLoader();
        cl.setFile(file);
        Instances rawDataSet = cl.getDataSet();
        Instances pickUps = preprocess(rawDataSet);

        file = new File(dropOffName);
        cl = new CSVLoader();
        cl.setFile(file);
        rawDataSet = cl.getDataSet();
        Instances dropOffs = preprocess(rawDataSet);

        Map<String, List<Instance>> grid = new HashMap<String, List<Instance>>();
        populateGrid(grid, pickUps);
        populateGrid(grid, dropOffs);
        for(String key: grid.keySet()) {
            System.out.println("Cell: "+key+", #instances: "+grid.get(key).size());
        }
            System.out.println("#Cells: "+grid.size());
        
        
        File out = new File(outName);
        CSVSaver saver = new CSVSaver();
        saver.setFile(out);
        saver.setInstances(dropOffs);
        saver.writeBatch();
    }

    public static void main(String[] args) throws Exception {
        App a = new App();
        String pickupName = args.length >= 1 ? args[0] : null;
        String dropoffName = args.length >= 2 ? args[1] : null;
        String outName = args.length >= 3 ? args[2] : null;
        a.run(pickupName, dropoffName, outName);
    }
}
