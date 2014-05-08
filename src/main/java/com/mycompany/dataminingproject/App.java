package com.mycompany.dataminingproject;

import weka.filters.unsupervised.instance.RemoveDuplicates;
import java.io.File;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Hello world!
 *
 */
public class App {

    private static Instances filterLatLng(Instances dataSet, double minLat, 
            double minLng, double maxLat, double maxLng) {
        int latIndex = dataSet.attribute("latitude").index();
        int lngIndex = dataSet.attribute("longitude").index();
        for (int i = 0; i < dataSet.numInstances();) {
            double lat = dataSet.instance(i).value(latIndex);
            double lng = dataSet.instance(i).value(lngIndex);
            if (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng) {
                dataSet.delete(i);
            } else {
                i++;
            }
        }
        return dataSet;
    }

    private static Instances filterDateRange(Instances dataSet, 
            String minDate, String maxDate) {
        int attrIndex = dataSet.attribute("gpsdate").index();
        for (int i = 0; i < dataSet.numInstances();) {
            String attr = dataSet.instance(i).stringValue(attrIndex);
            if (attr.compareTo(minDate) < 0 || attr.compareTo(maxDate) > 0) {
                dataSet.delete(i);
            } else
                i++;
        }
        return dataSet;
    }
    
    public static void main(String[] args) throws Exception {
        File file = new File("/home/smart-cities-23/Dropbox/Materiale-Didattico/Data Mining For Smart Cities/progettoGPS/pechino_endtime.csv");
        CSVLoader cl = new CSVLoader();
        cl.setFile(file);
        Instances dataSet = cl.getDataSet();

        // Filtra regione geografica di interesse entro certe coordinate
        dataSet = filterLatLng(dataSet, 39.750, 40.120, 116.130, 116.650);

        // Elimina attributi inutili 
        int[] removeIndices = {0, 5};
        Remove removeFilter = new Remove();
        removeFilter.setAttributeIndicesArray(removeIndices);
        removeFilter.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, removeFilter);

        // Filtra campioni doppi
        RemoveDuplicates removeDuplicateFilter = new RemoveDuplicates();
        removeDuplicateFilter.setInputFormat(dataSet);
        dataSet = Filter.useFilter(dataSet, removeDuplicateFilter);
        
        // Filtra range di date
        dataSet = filterDateRange(dataSet, "2008-01-01", "2008-12-31");

        System.out.println("Numero campioni: " + dataSet.numInstances());

        File out = new File("/home/smart-cities-23/Scrivania/out.csv");

        CSVSaver saver = new CSVSaver();
        saver.setFile(out);
        saver.setInstances(dataSet);
        saver.writeBatch();
    }
}
