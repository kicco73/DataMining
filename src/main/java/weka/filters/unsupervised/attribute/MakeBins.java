package weka.filters.unsupervised.attribute;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Capabilities.Capability;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import weka.core.Attribute;
import weka.core.FastVector;

/**
 * <!-- globalinfo-start -->
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * <!-- options-end -->
 *
 * @author Enrico Carniani (enrico.carniani@iit.cnr.it)
 * @author Filippo Ricci
 * @version $Revision: $
 */
public class MakeBins
        extends PotentialClassIgnorer
        implements UnsupervisedFilter, OptionHandler {

    /**
     * for serialization.
     */
    static final long serialVersionUID = -8158531150984362900L;

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

    private Date minDate;
    private Date maxDate;
    private int binsInADay = 1;

    public enum Period {
        LINEAR, BY_HOURS_IN_A_DAY, BY_WORKING_DAYS, BY_DAY_OF_WEEK
    };

    private Period period = Period.LINEAR;
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private Map<String, int[]> aggregate;

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
    public boolean setInputFormat(Instances instanceInfo)
            throws Exception {
        super.setInputFormat(instanceInfo);
        return false;
    }

    private int getNumBins() {
        final long oneDay = 86400 * 1000;
        final long millisInAHour = 1000*60*60;
        switch(period) {
            case LINEAR:
                return (int) ((maxDate.getTime()+oneDay - minDate.getTime())/millisInAHour*binsInADay/24);
            case BY_DAY_OF_WEEK:
                return 7 * binsInADay;
            case BY_WORKING_DAYS:
                return 2 * binsInADay;
            case BY_HOURS_IN_A_DAY:
                return binsInADay;
        }
        return 1;
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

        if (m_NewBatch) {
            resetQueue();
            aggregate = new HashMap<String, int[]>();
            m_NewBatch = false;
        }
        make(instance);
        return false;
    }

    private int getBinIndex(Date gpsDate) {
        int bin = 0;
        switch (period) {
            case LINEAR:
                final long oneDay = 86400 * 1000;
                long binDuration = (maxDate.getTime() + oneDay - minDate.getTime()) / getNumBins();
                bin = (int) ((gpsDate.getTime() - minDate.getTime()) / binDuration);
                break;
            case BY_HOURS_IN_A_DAY:
                bin = gpsDate.getHours() * binsInADay / 24;
                break;
            case BY_DAY_OF_WEEK:
                bin = gpsDate.getDay() * binsInADay + gpsDate.getHours() * binsInADay / 24;
                break;
            case BY_WORKING_DAYS:
                bin = gpsDate.getHours() * binsInADay / 24;
                bin += gpsDate.getDay() == 0 || gpsDate.getDay() == 6? binsInADay : 0;
                break;
        }
        return bin;
    }

    private Map<String, int[]> make(Instance instance) {
        Attribute cellId = getInputFormat().attribute("cellId");
        int dateIndex = getInputFormat().attribute("gpsdate").index();
        int timeIndex = getInputFormat().attribute("gpstime").index();
        String key = instance.stringValue(cellId.index());
        String dateStr = instance.stringValue(dateIndex)+" "+instance.stringValue(timeIndex);
        try {
            Date gpsDate = format.parse(dateStr);
            int bin = getBinIndex(gpsDate);
            if (bin >= 0 && bin < getNumBins()) {
                int[] resultCell = aggregate.get(key);
                if (resultCell == null) {
                    resultCell = new int[getNumBins()];
                    aggregate.put(key, resultCell);
                }
                resultCell[bin]++;
            } else {
                System.err.println("*** WARNING: date " + dateStr + " not in range " + minDate + " : " + maxDate + ", ignoring (bin " + bin + ")");
            }
        } catch (ParseException p) {
            System.err.println("*** WARNING: error parsing date " + dateStr);
        }
        return aggregate;
    }

    private Instances convertToInstances(Map<String, int[]> aggregate) {
        FastVector attributes = new FastVector();
        Attribute cellId = new Attribute("cellId", (FastVector) null);
        attributes.addElement(cellId);
        for (int i = 0; i < getNumBins(); i++) {
            attributes.addElement(new Attribute("bin" + i));
        }
        Instances dataset = new Instances("grid", attributes, aggregate.size());
        for (String key : aggregate.keySet()) {
            int[] features = aggregate.get(key);
            Instance instance = new Instance(1 + getNumBins());
            instance.setDataset(dataset);
            instance.setValue(0, key);
            for (int i = 0; i < getNumBins(); i++) {
                instance.setValue(i + 1, features[i]);
            }
            dataset.add(instance);
        }
        return dataset;
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
        Instances instances = convertToInstances(aggregate);
        setOutputFormat(instances);
        for(int i = 0; i < instances.numInstances(); i++)
            push(instances.instance(i));
        m_NewBatch = true;
        System.out.println("MakeBins(): created "+getNumBins()+" time bins, pushed "+instances.numInstances()+ " instances");
        return false;
    }

    public Date getMinDate() {
        return minDate;
    }

    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    public int getBinsInADay() {
        return binsInADay;
    }

    public void setBinsInADay(int binsInADay) {
        this.binsInADay = binsInADay;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    public String getRevision() {
        return RevisionUtils.extract("$Revision: $");
    }

    /**
     * Main method for running this filter.
     *
     * @param args should contain arguments to the filter, use -h for help
     */
    public static void main(String[] args) {
        runFilter(new MakeBins(), args);
    }
}
