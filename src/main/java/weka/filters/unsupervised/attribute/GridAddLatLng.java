/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.filters.unsupervised.attribute;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Capabilities.Capability;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import weka.core.Attribute;
import weka.core.FastVector;

/**
 * <!-- globalinfo-start -->
 * Normalizes all numeric values in the given dataset (apart from the class
 * attribute, if set). The resulting values are by default in [0,1] for the data
 * used to compute the normalization intervals. But with the scale and
 * translation parameters one can change that, e.g., with scale = 2.0 and
 * translation = -1.0 you get values in the range [-1,+1].
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * Valid options are:
 * <p/>
 *
 * <pre> -unset-class-temporarily
 *  Unsets the class index temporarily before the filter is
 *  applied to the data.
 *  (default: no)</pre>
 *
 * <pre> -S <num>
 *  The scaling factor for the output range.
 *  (default: 1.0)</pre>
 *
 * <pre> -T <num>
 *  The translation of the output range.
 *  (default: 0.0)</pre>
 *
 * <!-- options-end -->
 *
 * @author Enrico Carniani (enrico.carniani@iit.cnr.it)
 * @author Filippo Ricci
 * @version $Revision: 5987 $
 */
public class GridAddLatLng
        extends PotentialClassIgnorer
        implements UnsupervisedFilter, OptionHandler {

    /**
     * for serialization.
     */
    static final long serialVersionUID = -8158531150984362895L;
    private int pushed = 0;
    private int cellIdIndex;
    private double nwLat = 0.0;
    private double nwLng = 0.0;
    private double seLat = 0.0;
    private double seLng = 0.0;
    private double cellXSizeInMeters = 100;
    private double cellYSizeInMeters = 100;

    private Pattern pattern = Pattern.compile("\\((\\d+);(\\d+)\\)");

    /**
     * Returns a string describing this filter.
     *
     * @return a description of the filter suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "Normalizes all numeric values in the given dataset (apart from the "
                + "class attribute, if set). The resulting values are normalized by default "
                + "in [0,1] for the data used to compute the normalization intervals. "
                + "But with the scale and translation parameters one can change that, "
                + "e.g., with scale = 2.0 and translation = -1.0 you get values in the "
                + "range [-1,+1].";
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    public Enumeration listOptions() {
        Vector result = new Vector();

        Enumeration en = super.listOptions();
        while (en.hasMoreElements()) {
            result.addElement(en.nextElement());
        }

        result.addElement(new Option(
                "\tThe scaling factor for the output range.\n"
                + "\t(default: 1.0)",
                "S", 1, "-S <num>"));

        result.addElement(new Option(
                "\tThe translation of the output range.\n"
                + "\t(default: 0.0)",
                "T", 1, "-T <num>"));

        return result.elements();
    }

    /**
     * Parses a given list of options.
     * <p/>
     *
     * <!-- options-start -->
     * Valid options are:
     * <p/>
     *
     * <pre> -unset-class-temporarily
     *  Unsets the class index temporarily before the filter is
     *  applied to the data.
     *  (default: no)</pre>
     *
     * <pre> -S <num>
     *  The scaling factor for the output range.
     *  (default: 1.0)</pre>
     *
     * <pre> -T <num>
     *  The translation of the output range.
     *  (default: 0.0)</pre>
     *
     * <!-- options-end -->
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {
        if (getInputFormat() != null) {
            setInputFormat(getInputFormat());
        }
    }

    /**
     * Gets the current settings of the filter.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {
        Vector<String> result = new Vector<String>();
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
     * Sets the format of the input instances.
     *
     * @param instanceInfo an Instances object containing the input instance
     * structure (any instances contained in the object are ignored - only the
     * structure is required).
     * @return true if the outputFormat may be collected immediately
     * @throws Exception if the input format can't be set successfully
     */
    @Override
    public boolean setInputFormat(Instances instanceInfo)
            throws Exception {

        cellIdIndex = instanceInfo.attribute("cellId").index();
        super.setInputFormat(instanceInfo);
        Instances output = new Instances(instanceInfo, instanceInfo.numInstances());
        Attribute longitude = new Attribute("longitude", (FastVector)null);
        output.insertAttributeAt(longitude, cellIdIndex+1);
        Attribute latitude = new Attribute("latitude", (FastVector)null);
        output.insertAttributeAt(latitude, cellIdIndex+1);
        setOutputFormat(output);
        return true;
    }

    public static double xMetersToRad(double meters) {
        final double factor = 7.91959594934121e-06;
        //System.out.println("*** distance in meters: ("+lat1+","+lng1+") - ("+lat2+","+lng2+") = " + Math.sqrt(Math.pow(lat2-lat1, 2)+Math.pow(lng2-lng1, 2))/factor);
        return meters * factor;
    }

    public static double yMetersToRad(double meters) {
        final double factor = 7.91959594934121e-06;
        System.out.println("*** y meters: ("+meters+") = " + meters*factor);
        return meters * factor;
    }
    /**
     * Input an instance for filtering. Filter requires all training instances
     * be read before producing output.
     *
     * @param instance the input instance
     * @return true if the filtered instance may now be collected with output().
     * @throws Exception if an error occurs
     * @throws IllegalStateException if no input format has been set.
     */
    @Override
    public boolean input(Instance instance) throws Exception {
        if (getInputFormat() == null) {
            throw new IllegalStateException("No input instance format defined");
        }
        String key = instance.stringValue(cellIdIndex);
        Matcher m = pattern.matcher(key);
        m.matches();
        int yCell = Integer.parseInt(m.group(1));
        int xCell = Integer.parseInt(m.group(2));
        // FIXME vorrei centrarle sulla cella, per questo aggiungo 0.5 ma non e' generale
        // e non ho neanche verificato se e' ok in questo caso specifico
        double radPerXCell = xMetersToRad(cellXSizeInMeters);
        double radPerYCell = yMetersToRad(cellYSizeInMeters);
        double latitude = nwLat-(yCell+0.5)*radPerYCell;
        double longitude = nwLng+(xCell+0.5)*radPerXCell;
        Instance outInstance = new Instance(instance);
        outInstance.insertAttributeAt(outInstance.numAttributes());
        outInstance.insertAttributeAt(outInstance.numAttributes());
        outInstance.setValue(getOutputFormat().attribute("cellId"), key);
        outInstance.setValue(getOutputFormat().attribute("latitude"), Double.toString(latitude));
        outInstance.setValue(getOutputFormat().attribute("longitude"), Double.toString(longitude));
        push(outInstance);
        pushed++;
        return true;
    }

    /**
     * Signify that this batch of input to the filter is finished. If the filter
     * requires all instances prior to filtering, output() may now be called to
     * retrieve the filtered instances.
     *
     * @return true if there are instances pending output
     * @throws Exception if an error occurs
     * @throws IllegalStateException if no input structure has been defined
     */
    @Override
    public boolean batchFinished() throws Exception {
        if (getInputFormat() == null) {
            throw new IllegalStateException("No input instance format defined");
        }
        System.out.println("MapGridToGps(): mapped "+pushed
                +" instances");
        return super.batchFinished();
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
     * Returns the revision string.
     *
     * @return the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 5987 $");
    }

    /**
     * Main method for running this filter.
     *
     * @param args should contain arguments to the filter, use -h for help
     */
    public static void main(String[] args) {
        runFilter(new GridAddLatLng(), args);
    }
}
