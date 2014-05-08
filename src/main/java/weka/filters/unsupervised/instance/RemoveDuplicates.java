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
import java.util.Vector;
import weka.core.InstanceComparator;

/**
 * <!-- globalinfo-start -->
 * Remove duplicate instances from the data set.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * <!-- options-start -->
 * This filters has no options.
 * <p/>
 * <!-- options-end -->
 *
 * @author Enrico Carniani (enrico.carniani@iit.cnr.it)
 * @author Filippo Ricci
 * @version $Revision: 5548 $
 */
public class RemoveDuplicates
        extends Filter
        implements UnsupervisedFilter, OptionHandler {

    /**
     * for serialization
     */
    static final long serialVersionUID = 3119607037607101161L;

    /**
     * Returns a string describing this classifier
     *
     * @return a description of the classifier suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "Removes instance duplicates.";
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
    private InstanceComparator ic = new InstanceComparator();

    @Override
    public boolean input(Instance instance) {
        for (int i = 0; i < getOutputFormat().numInstances(); i++) {
            if (ic.compare(getInputFormat().instance(i), instance) == 0) {
                return true;
            }
        }
        push(instance);
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
        setOutputFormat(instanceInfo);
        return true;
    }

    /**
     * Main method for testing this class.
     *
     * @param argv should contain arguments to the filter: use -h for help
     */
    public static void main(String[] argv) {
        runFilter(new RemoveDuplicates(), argv);
    }
}
