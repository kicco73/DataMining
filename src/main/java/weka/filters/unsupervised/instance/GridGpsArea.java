/*
 */
package weka.filters.unsupervised.instance;

import java.util.ArrayList;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.w3c.dom.Attr;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.InstanceComparator;

/**
 * <!-- globalinfo-start -->
 Crops an area described by (nwLat, nwLng, seLat, seLng) coordinates
 by removing all instances whose latitude and longitude values are outside
 the boundaries.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are:
 * <p/>
 *
 * <pre> -nwLat &lt;degrees&gt;
 *  The north-western latitude of the cropped area</pre>
 *
 * <pre> -nwLng &lt;degrees&gt;
 *  The north-western longitude of the cropped area</pre>
 *
 * <pre> -seLat &lt;degrees&gt;
 *  The south-eastern latitude of the cropped area</pre>
 *
 * <pre> -seLng &lt;degrees&gt;
 *  The south-eastern longitude of the cropped area</pre>
 *
 * <pre> -cellXSizeInMeters &lt;meters&gt;
 *  The longitude size of a cell grid in meters. Default is 100.</pre>
 *
 * <pre> -cellYSizeInMeters &lt;meters&gt;
 *  The latitude size of a cell grid in meters. Default is 100.</pre>
 *
 * <pre> -latitudeAttribute &lt;attributeName&gt;
 *  The name of the latitude attribute. Default is 'latitude'.</pre>
 *
 * <pre> -longitudeAttribute &lt;attributeName&gt;
 *  The name of the longitude attribute. Default is 'longitude'.</pre>
 *
 * <!-- options-end -->
 *
 * @author Enrico Carniani (enrico.carniani@iit.cnr.it)
 * @author Filippo Ricci
 * @version $Revision: 5548 $
 */
public class GridGpsArea
        extends Filter
        implements UnsupervisedFilter, OptionHandler {

    /**
     * for serialization
     */
    static final long serialVersionUID = 3119607037607101162L;
    private int latIndex = -1;
    private int lngIndex = -1;
    private double nwLat = 0.0;
    private double nwLng = 0.0;
    private double seLat = 0.0;
    private double seLng = 0.0;
    private double cellXSizeInMeters = 100;
    private double cellYSizeInMeters = 100;
    private Map<String, List<Instance>> cell = new HashMap<String, List<Instance>>();
    private String latitudeName = "latitude";
    private String longitudeName = "longitude";
    private Attribute cellId;

    /**
     * Returns a string describing this classifier
     *
     * @return a description of the classifier suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "Crops a terrestrial area described by (minLat, minLng, "
              +"maxLat, maxLng) by removing all features whose (latitude, "
              +"longitude) fall outside the boundaries.";
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    public Enumeration listOptions() {
        Vector result = new Vector();
        return result.elements();
    }

    /**
     * Parses a given list of options.
     * <p/>
     *
     * <!-- options-start -->
     * <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {
    }

    /**
     * Gets the current settings of the filter.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String[] getOptions() {
        Vector<String> result = new Vector();
        return result.toArray(new String[result.size()]);
    }

    /**
     * Returns the Capabilities of this filter.
     *
     * @return the capabilities of this object
     * @see Capabilities
     */
    @Override
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();

        // attributes
        result.enableAllAttributes();
        result.enable(Capability.MISSING_VALUES);

        // class
        result.enableAllClasses();
        result.enable(Capability.MISSING_CLASS_VALUES);
        result.enable(Capability.NO_CLASS);

        return result;
    }

    /**
     * Input an instance for filtering.
     *
     * @param instance the input instance
     * @return true if the filtered instance may now be collected with output().
     * @throws IllegalStateException if no input structure has been defined
     */

    public static double distanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        final double factor = 7.91959594934121e-06;
        //System.out.println("*** distance in meters: ("+lat1+","+lng1+") - ("+lat2+","+lng2+") = " + Math.sqrt(Math.pow(lat2-lat1, 2)+Math.pow(lng2-lng1, 2))/factor);
        double dlng = Math.abs(lng2-lng1) >= Math.PI? Math.abs(lng2-lng1) - Math.PI : lng2-lng1;
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(dlng, 2)) / factor;
    }

    @Override
    public boolean input(Instance instance) {
        if (getInputFormat() == null)
            throw new IllegalStateException("No input instance format defined");
        double lat = instance.value(latIndex);
        double lng = instance.value(lngIndex);
        if (lat >= nwLat && lat <= seLat && lng >= nwLng && lng <= seLng) {
            int x = (int) Math.floor(distanceInMeters(0, nwLng, 0, lng) / cellXSizeInMeters);
            int y = (int) Math.floor(distanceInMeters(nwLat, 0, lat, 0) / cellYSizeInMeters);
            String key = "(" + y + ";" + x + ")";
            Instance outInstance = new Instance(instance);
            outInstance.insertAttributeAt(instance.numAttributes());
            push(outInstance);
            outInstance.setValue(instance.numAttributes(), key);
            System.out.println("*** " + outInstance);
        }
        return true;
    }

    /**
     * Signify that this batch of input to the filter is finished. If the filter
     * requires all instances prior to filtering, output() may now be called to
     * retrieve the filtered instances.
     *
     * @return true if there are instances pending output
     * @throws IllegalStateException if no input structure has been defined
     */
    /**
     * Returns the revision string.
     *
     * @return	the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 5548 $");
    }

    @Override
    public boolean setInputFormat(Instances instanceInfo)
            throws Exception {

        super.setInputFormat(instanceInfo);
        Instances output = new Instances(instanceInfo,instanceInfo.numInstances());
        cellId = new Attribute("cellId", (FastVector)null);
        output.insertAttributeAt(cellId, output.numAttributes());
        latIndex = output.attribute(latitudeName).index();
        lngIndex = output.attribute(longitudeName).index();
        setOutputFormat(output);
        return true;
    }

    public int getLatIndex() {
        return latIndex;
    }

    public void setLatIndex(int latIndex) {
        this.latIndex = latIndex;
    }

    public int getLngIndex() {
        return lngIndex;
    }

    public void setLngIndex(int lngIndex) {
        this.lngIndex = lngIndex;
    }

    public double getNwLat() {
        return nwLat;
    }

    public void setNwLat(double nwLat) {
        this.nwLat = nwLat;
    }

    public double getNwLng() {
        return nwLng;
    }

    public void setNwLng(double nwLng) {
        this.nwLng = nwLng;
    }

    public double getSeLat() {
        return seLat;
    }

    public void setSeLat(double seLat) {
        this.seLat = seLat;
    }

    public double getSeLng() {
        return seLng;
    }

    public void setSeLng(double seLng) {
        this.seLng = seLng;
    }

    public String getLatitudeName() {
        return latitudeName;
    }

    public void setLatitudeName(String latitudeName) {
        this.latitudeName = latitudeName;
    }

    public String getLongitudeName() {
        return longitudeName;
    }

    public void setLongitudeName(String longitudeName) {
        this.longitudeName = longitudeName;
    }

    public double getCellXSizeInMeters() {
        return cellXSizeInMeters;
    }

    public void setCellXSizeInMeters(double cellXSizeInMeters) {
        this.cellXSizeInMeters = cellXSizeInMeters;
    }

    public double getCellYSizeInMeters() {
        return cellYSizeInMeters;
    }

    public void setCellYSizeInMeters(double cellYSizeInMeters) {
        this.cellYSizeInMeters = cellYSizeInMeters;
    }

    public void setArea(double minLat, double minLng, double maxLat, double maxLng) {
        setNwLat(minLat);
        setNwLng(minLng);
        setSeLat(maxLat);
        setSeLng(maxLng);
    }
    
    public void setCell(double nsMeters, double weMeters) {
        setCellXSizeInMeters(nsMeters);
        setCellYSizeInMeters(weMeters);
    }
    
    /**
     * Main method for testing this class.
     *
     * @param argv should contain arguments to the filter: use -h for help
     */
    public static void main(String[] argv) {
        runFilter(new GridGpsArea(), argv);
    }
}
