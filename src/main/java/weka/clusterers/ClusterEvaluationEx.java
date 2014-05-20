/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.clusterers;

import java.util.Arrays;
import weka.core.DistanceFunction;
import weka.core.Instances;
import weka.core.ManhattanDistance;

/**
 *
 * @author kicco
 */
public class ClusterEvaluationEx extends ClusterEvaluation {
    
    double avgSilhouetteCoefficient = Double.MIN_VALUE;

    public double getAvgSilhouetteCoefficient() {
        return avgSilhouetteCoefficient;
    }
    
    private double silhouetteCoefficient(Instances instances, DistanceFunction df) {
        double clusterAssignments[] = getClusterAssignments();	
        int [] clusterSizes = new int[getNumClusters()];
	for (int i = 0; i < instances.numInstances(); i++) {
            int cluster = (int)clusterAssignments[i];
            clusterSizes[cluster]++;
        }
        // Calculate Silhouette Coefficient
        double[] SilCoeff = new double[instances.numInstances()];
        double AvgSilCoeff = 0;
        for (int z = 0; z < instances.numInstances(); z++) {
            double[] distance = new double[getNumClusters()];
            Arrays.fill(distance, 0.0);
            //Sum
            for (int y = 0; y < instances.numInstances(); y++) {
                double delta = df.distance(instances.instance(z), instances.instance(y));
                distance[(int)clusterAssignments[y]] += delta;
            }
            //Average
            for (int x = 0; x < getNumClusters(); x++) {
                distance[x] = distance[x] / clusterSizes[x];
            }
            double a = distance[(int)clusterAssignments[z]];
            distance[(int)clusterAssignments[z]] = Double.MAX_VALUE;
            Arrays.sort(distance);
            double b = distance[0];
            SilCoeff[z] = (b - a) / Math.max(a, b);
            AvgSilCoeff += SilCoeff[z];
        }
        AvgSilCoeff = AvgSilCoeff / instances.numInstances();
        return AvgSilCoeff;
    }
    
    public void evaluateClusterer(Instances test, DistanceFunction df) throws Exception {
        evaluateClusterer(test);
        DistanceFunction distanceFunction = new ManhattanDistance();
        distanceFunction.setInstances(test);
        System.out.println("*** Silhouette Coefficient (Manhattan):\t" + silhouetteCoefficient(test, distanceFunction));
        avgSilhouetteCoefficient = silhouetteCoefficient(test, df);
        System.out.println("*** Silhouette Coefficient (custom):\t" + avgSilhouetteCoefficient);
    }
}
