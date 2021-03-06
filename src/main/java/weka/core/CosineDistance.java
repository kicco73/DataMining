package weka.core;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.neighboursearch.PerformanceStats;

/**
 * <!-- globalinfo-start --> Implements the Cosine distance. The distance
 * between two points is the cosine value of their corresponding vectors (from
 * the origin).<br/>
 * <br/>
 * For more information, see:<br/>
 * <br/>
 * Wikipedia. Cosine similarity. URL
 * http://en.wikipedia.org/wiki/Cosine_similarity.
 * <p/>
 * <!-- globalinfo-end -->
 *
 * 
*  @author Enrico Carniani
*  @author Filippo Ricci
 * @version $Revision: $
 */

public class CosineDistance extends EuclideanDistance implements DistanceFunction, Serializable,
        RevisionHandler {

	 /**
     * for serialization.
     */
    private static final long serialVersionUID = -123123123123123L;
    private boolean splitMax = false;
    
    /**
     * Returns a string describing this object.
     *     
    * @return a description of the evaluator suitable for displaying in the
     * explorer/experimenter gui
     */
    @Override
    public String globalInfo() {
        return "The cosine rule of vector distance.";
    }

    /**
     * Returns the revision string.
     *     
* @return the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision: $");
    }

    /**
     * Gets the current settings. Returns empty array.
     *     
* @return an array of strings suitable for passing to setOptions()
     */
    @Override
    public String[] getOptions() {
        String[] options = new String[0];
        return options;
    }

    /**
     * Returns an enumeration describing the available options.
     *     
* @return an enumeration of all the available options.
     */
    @Override
    public Enumeration listOptions() {
        Vector newVector = new Vector();
        return newVector.elements();
    }

    /**
     * Parses a given list of options.
     *     
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {
    }

    private double cos(Instance first, Instance second, String prefix) {
        int classidx = first.classIndex();
        double similarity = 0.0, product = 0.0, lengthA = 0.0, lengthB = 0.0;
        
        for (int v = 0; v < first.numAttributes(); v++) {
            if (v != classidx && first.attribute(v).isNumeric() && (prefix == null 
                    || first.attribute(v).name().startsWith(prefix))) {                
                double valueA = first.value(v);
                double valueB = second.value(v);
                product += valueA * valueB;
                lengthA += valueA * valueA;
                lengthB += valueB * valueB;
           }
        }
        
        if (lengthA == 0) {
        	System.err.println("*** CosineDistance(): null vector "+prefix+": "+first);
        	similarity = Double.NaN;
        } else if (lengthB == 0) {
        	System.err.println("*** CosineDistance(): null vector "+prefix+": "+second);
        	similarity = Double.NaN;
        } else similarity = product / (Math.sqrt(lengthA * lengthB)); 
        return similarity;
    }
    
    /**
     * Calculates the distance between two instances.
     *     
     * @param first the first instance
     * @param second the second instance
     * @return the similarity (max of CosineDistance between number of Get-ons vector and number o
     * of Get-offs Vector) between the two given instances
     */
    @Override
    public double distance(Instance first, Instance second) {

        if (first.equalHeaders(second) == false) {
            System.err.println("Headers of the two instances don't match!");
            return Double.NaN;
        }
        if(splitMax) {
            double cosineDistanceOn = 1-cos(first, second, "up");        
            double cosineDistanceOff = 1-cos(first, second, "dn");
            return Math.max(cosineDistanceOn, cosineDistanceOff);
        }
        return 1-cos(first, second, null);
    }

    public boolean isSplitMax() {
        return splitMax;
    }

    public void setSplitMax(boolean splitMax) {
        this.splitMax = splitMax;
    }
    
    /**
     * Calculates the distance between two instances.
     *     
* @param first the first instance
     * @param second the second instance
     * @param stats the performance stats object
     * @return the distance between the two given instances
     */
    @Override
    public double distance(Instance first, Instance second, PerformanceStats stats) {
        return distance(first, second, Double.POSITIVE_INFINITY, stats);
    }

    /**
     * Calculates the distance between two instances. Offers speed up (if the
     * distance function class in use supports it) in nearest neighbour search
     * by taking into account the cutOff or maximum distance. Depending on the
     * distance function class, post processing of the distances by
     * postProcessDistances(double []) may be required if this function is used.
     *     
     * @param first the first instance
     * @param second the second instance
     * @param cutOffValue If the distance being calculated becomes larger than
     * cutOffValue then the rest of the calculation is discarded.
     * @return the distance between the two given instances or
     * Double.POSITIVE_INFINITY if the distance being calculated becomes larger
     * than cutOffValue.
     */
    @Override
    public double distance(Instance first, Instance second, double cutOffValue) {
        return distance(first, second, cutOffValue, null);
    }

    /**
     * Calculates the distance between two instances. Offers speed up (if the
     * distance function class in use supports it) in nearest neighbour search
     * by taking into account the cutOff or maximum distance. Depending on the
     * distance function class, post processing of the distances by
     * postProcessDistances(double []) may be required if this function is used.
     *     
     * @param first the first instance
     * @param second the second instance
     * @param cutOffValue If the distance being calculated becomes larger than
     * cutOffValue then the rest of the calculation is discarded.
     * @param stats the performance stats object
     * @return the distance between the two given instances or
     * Double.POSITIVE_INFINITY if the distance being calculated becomes larger
     * than cutOffValue.
     */
    @Override
    public double distance(Instance first, Instance second, double cutOffValue,
            PerformanceStats stats) {
        double distance = distance(first, second);
        if (distance > cutOffValue) {
            return Double.POSITIVE_INFINITY;
        }
        return distance;
    }

}