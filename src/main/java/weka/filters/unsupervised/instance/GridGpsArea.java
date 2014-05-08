/*
 */
package weka.filters.unsupervised.instance;

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
 * Crops an area described by (minLat, minLng, maxLat, maxLng) coordinates
 * by removing all instances whose latitude and longitude values are outside
 * the boundaries.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are:
 * <p/>
 *
 * <pre> -minLat &lt;degrees&gt;
 *  The north-western latitude of the cropped area</pre>
 *
 * <pre> -minLng &lt;degrees&gt;
 *  The north-western longitude of the cropped area</pre>
 *
 * <pre> -maxLat &lt;degrees&gt;
 *  The south-eastern latitude of the cropped area</pre>
 *
 * <pre> -maxLng &lt;degrees&gt;
 *  The south-eastern longitude of the cropped area</pre>
 *
 * <pre> -sizeX &lt;meters&gt;
 *  The longitude size of a cell grid in meters. Default is 100.</pre>
 *
 * <pre> -sizeY &lt;meters&gt;
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
    private double minLat = 0.0;
    private double minLng = 0.0;
    private double maxLat = 0.0;
    private double maxLng = 0.0;
    private double sizeX = 100;
    private double sizeY = 100;
    private Map<String, List<Instance>> cell = new HashMap<String, List<Instance>>();
    private String latitudeName = "latitude";
    private String longitudeName = "longitude";
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

    @Override
    public boolean input(Instance instance) {
        double lat = instance.value(latIndex);
        double lng = instance.value(lngIndex);
        if (lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng) {
            push(instance);            
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
        latIndex = instanceInfo.attribute(latitudeName).index();
        lngIndex = instanceInfo.attribute(longitudeName).index();
        
        FastVector attributes = new FastVector();
        attributes.addElement(new Attribute("cellId"));
        
        attributes.addElement(new Attribute("id"));
	Instances dataset = new Instances("grid", attributes, 200);
        setOutputFormat(dataset);
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

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMinLng() {
        return minLng;
    }

    public void setMinLng(double minLng) {
        this.minLng = minLng;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMaxLng() {
        return maxLng;
    }

    public void setMaxLng(double maxLng) {
        this.maxLng = maxLng;
    }

    public double getSizeX() {
        return sizeX;
    }

    public void setSizeX(double sizeX) {
        this.sizeX = sizeX;
    }

    public double getSizeY() {
        return sizeY;
    }

    public void setSizeY(double sizeY) {
        this.sizeY = sizeY;
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

    public void setArea(double minLat, double minLng, double maxLat, double maxLng) {
        setMinLat(minLat);
        setMinLng(minLng);
        setMaxLat(maxLat);
        setMaxLng(maxLng);
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
