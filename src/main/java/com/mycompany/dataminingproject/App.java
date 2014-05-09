package com.mycompany.dataminingproject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weka.core.Attribute;
import weka.core.FastVector;
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

    Date minDate = new Date(2007, 4, 13);
    Date maxDate = new Date(2009, 9, 23);
    static int numBins = 29;
    
    public static double distanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        final double factor = 7.91959594934121e-06;
        //System.out.println("*** distance in meters: ("+lat1+","+lng1+") - ("+lat2+","+lng2+") = " + Math.sqrt(Math.pow(lat2-lat1, 2)+Math.pow(lng2-lng1, 2))/factor);
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lng2 - lng1, 2)) / factor;
    }

    public static void populateGrid(Map<String, List<Instance>> grid, Instances inputDataSet) {
        int latIndex = inputDataSet.attribute("latitude").index();
        int lngIndex = inputDataSet.attribute("longitude").index();

        double xSizeInMeters = distanceInMeters(0, minLng, 0, maxLng);
        double ySizeInMeters = distanceInMeters(minLat, 0, maxLat, 0);
        int xCells = (int) Math.ceil(xSizeInMeters / cellXSizeInMeters);
        int yCells = (int) Math.ceil(ySizeInMeters / cellYSizeInMeters);

        System.out.println("*** xSize:  " + xSizeInMeters + "\tySize: " + ySizeInMeters);
        System.out.println("*** xCells: " + xCells + "\tyCells: " + yCells);

        for (int i = 0; i < inputDataSet.numInstances(); i++) {
            Instance instance = inputDataSet.instance(i);
            double lat = instance.value(latIndex);
            double lng = instance.value(lngIndex);
            int x = (int) Math.floor(distanceInMeters(0, minLng, 0, lng) / cellXSizeInMeters);
            int y = (int) Math.floor(distanceInMeters(minLat, 0, lat, 0) / cellYSizeInMeters);
            String key = "(" + y + ";" + x + ")";
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

    private Map<String, List<Integer>> makeBins(Map<String, List<Instance>> grid, Instances dataSet, Date minDate, Date maxDate, int numBins) throws ParseException {
        Map<String, List<Integer>> result = new HashMap<String, List<Integer>>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        long binDuration = (maxDate.getTime() - minDate.getTime()) / numBins;
        int dateIndex = dataSet.attribute("gpsdate").index();
        for(String cellId: grid.keySet()) {
            for(Instance instance: grid.get(cellId)) {
                String dateStr = instance.stringValue(dateIndex);
                Date gpsDate = format.parse(dateStr);
                int bin = (int)(gpsDate.getTime() - minDate.getTime()) /(int)binDuration;
                List<Integer> resultCell = result.get(cellId);
                if(resultCell == null) {
                    resultCell = new ArrayList<Integer>(bin);
                    for(int i = 0; i < bin; i++)
                        resultCell.set(i, 0);
                    result.put(cellId, resultCell);
                }
                resultCell.set(bin, resultCell.get(bin)+1);
            }
        }
        return result;
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
        Map<String, List<Integer>> pickUpsAggregate = makeBins(grid, pickUps, minDate, maxDate, numBins);
        grid.clear();
        populateGrid(grid, dropOffs);
        Map<String, List<Integer>> dropOffAggregate = makeBins(grid, dropOffs, minDate, maxDate, numBins);

        FastVector attributes = new FastVector();
        Attribute cellId = new Attribute("cellId");
        attributes.addElement(cellId);
        for(int i = 0; i < numBins; i++) {
            attributes.addElement(new Attribute("up"+i));
            attributes.addElement(new Attribute("dn"+i));
        }
	Instances dataset = new Instances("grid", attributes, 200);
        for(String key: pickUpsAggregate.keySet()) {
            List<Integer> pickupFeatures = pickUpsAggregate.get(key);
            List<Integer> dropoffFeatures = dropOffAggregate.get(key);
            Instance instance = new Instance(1+2*numBins);
            instance.setValue(cellId, key);
            for(int i = 0; i < numBins; i++) {
                Attribute a = dataset.attribute("up"+i);
                instance.setValue(a, pickupFeatures.get(i));
                a = dataset.attribute("dn"+i);
                double dropOffValue = 0.0;
                if(dropoffFeatures != null)
                    dropOffValue = dropoffFeatures.get(i);
                instance.setValue(a, dropOffValue);
            }
            dataset.add(instance);
            if(dropoffFeatures != null)
                dropOffAggregate.remove(key);
        }
        for(String key: dropOffAggregate.keySet()) {
            List<Integer> pickupFeatures = pickUpsAggregate.get(key);
            List<Integer> dropoffFeatures = dropOffAggregate.get(key);
            Instance instance = new Instance(1+2*numBins);
            instance.setValue(cellId, key);
            for(int i = 0; i < numBins; i++) {
                Attribute a = dataset.attribute("dn"+i);
                instance.setValue(a, dropoffFeatures.get(i));
                a = dataset.attribute("up"+i);
                instance.setValue(a, 0.0);
            }
            dataset.add(instance);
        }     
        
        for(String key: grid.keySet()) {
            System.out.println("Cell: "+key+", #instances: "+grid.get(key).size());
        }
            System.out.println("#Cells: "+grid.size());
        
        
        File out = new File(outName);
        CSVSaver saver = new CSVSaver();
        saver.setFile(out);
        saver.setInstances(dataset);
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
