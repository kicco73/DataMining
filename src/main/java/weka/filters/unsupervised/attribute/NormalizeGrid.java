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
import weka.core.Attribute;

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
public class NormalizeGrid
        extends PotentialClassIgnorer
        implements UnsupervisedFilter, OptionHandler {

    /**
     * for serialization.
     */
    static final long serialVersionUID = -8158531150984362899L;
    int pushed = 0;
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

        super.setInputFormat(instanceInfo);
        setOutputFormat(instanceInfo);
        return true;
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
    public boolean input(Instance instance) throws Exception {
        if (getInputFormat() == null) {
            throw new IllegalStateException("No input instance format defined");
        }

        if (m_NewBatch) {
            resetQueue();
            m_NewBatch = false;
        }
        double sum = 0;
        int n = 0;
        for(int i = 0; i < instance.numAttributes(); i++) {
            Attribute a = instance.attribute(i);
            if(a.isNumeric() && i != instance.classIndex()) {
                sum += instance.value(a);
                n++;
            }
        }
        double average = sum/n;
        Instance normalizedInstance = new Instance(instance.numAttributes());
        normalizedInstance.setDataset(getOutputFormat());
        for(int i = 0; i < instance.numAttributes(); i++) {
            Attribute a = instance.attribute(i);
            if(a.isNumeric() && i != instance.classIndex()) {
                normalizedInstance.setValue(a, instance.value(a) - average);
            } else if(a.isString()) 
                normalizedInstance.setValue(i, instance.stringValue(i));
            else
                normalizedInstance.setValue(i, instance.value(i));
        }
        push(normalizedInstance);
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
        m_NewBatch = true;
        System.out.println("NormalizeGrid(): normalized "+pushed
                +" instances with "+ getOutputFormat().numAttributes()+" attrs");
        return false;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 5987 $");
    }

    /**
     * Main method for running this filter.
     *
     * @param args should contain arguments to the filter, use -h for help
     */
    public static void main(String[] args) {
        runFilter(new NormalizeGrid(), args);
    }
}
