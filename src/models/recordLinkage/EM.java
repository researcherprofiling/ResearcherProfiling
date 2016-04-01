package models.recordLinkage;


import java.util.Arrays;
import java.util.stream.DoubleStream;

/*
* This class implements an EM algorithm that is aimed to learn
*   a pair-wise mergability function between records.
* Generative model assumed:
*   1.  This algorithm is given (n choose 2) similarity vectors.
*   2.  For each similarity vector, there is a chance alpha that it
*       represents two matching records, and 1-alpha not.
*   3.  Given that the records match or not, similarity measures are
*       randomly generated according to different independent normal
*       distributions for each field in the vector.
* */
public class EM {

    /*
    * This method assumes that xs is of shape ((n choose 2) x m)
    *   where n is the number of records, and m is the number of fields.
    * */
    public static double[] computeLabels(double[][] xs) {
        if (xs.length == 0) return new double[0];
        /*
        * Initialize parameters to estimate
        * */
        double[][] sigma = new double[2][xs[0].length];
        Arrays.fill(sigma[0], 0.15);
        Arrays.fill(sigma[1], 0.15);
        double alpha = 0.05;
        /*
        * Initialize memoization cache
        * */
        double[][] Ps = new double[2][xs.length];
        boolean[] ignoreField = new boolean[xs[0].length];
        Arrays.fill(ignoreField, false);
        /*
        * Loop through EM iterations
        * */
        double prevELL = 0;
        double deltaELL = Double.MAX_VALUE;
        double threshold = 1E-3;
        int prevIgnored = 0;
        while (Math.abs(deltaELL) > threshold) {
            /*
            * Estimation: calculate P_i^j
            *   1.  Calculate the generating probabilities given y = 0 and 1.
            *   2.  Calculate P_i^0 and P_i^1
            * */
            for (int i = 0; i < xs.length; i++) {
                double[] sum = new double[] {1, 1};
                for (int k = 0; k < xs[0].length; k++) {
                    if (ignoreField[k]) continue;
                    sum[0] *= normalAt(0, sigma[0][k], xs[i][k]);
                    sum[1] *= normalAt(1, sigma[1][k], xs[i][k]);
                }
                sum[0] *= 1 - alpha;
                sum[1] *= alpha;
                Ps[0][i] = sum[0] / (sum[0] + sum[1]);
                Ps[1][i] = sum[1] / (sum[0] + sum[1]);
            }
            double[] sumPs = new double[] {
                    DoubleStream.of(Ps[0]).parallel().sum(),
                    DoubleStream.of(Ps[1]).parallel().sum()
            };
            /*
            * Maximization: update parameters
            *   alpha = sumP1 / (sumP0 + sumP1) = sumP1
            *   mu_k^j = (sum_i (P_i^j * x_i^k)) / (sum_i (P_i^j))
            *   sigma_k^j = sqrt( (sum_i (P_i^j * (x_i^k - mu_k^j) ** 2)) / (sum_i (P_i^j)) )
            * */
            alpha = sumPs[1] / xs.length;
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < xs[0].length; k++) {
                    if (ignoreField[k]) continue;
                    double sumX_Mu = 0;
                    for (int i = 0; i < xs.length; i++) {
                        sumX_Mu += Ps[j][i] * (xs[i][k] - j) * (xs[i][k] - j);
                    }
                    sigma[j][k] = Math.sqrt(sumX_Mu / sumPs[j]);
                }
            }
            /*
            * Re-evaluate E(LL)
            * */
            int ignored = 0;
            for (int k = 0; k < xs[0].length; k++) {
                ignoreField[k] = ignoreField[k] || sigma[0][k] <= Double.MIN_VALUE || sigma[1][k] <= Double.MIN_VALUE;
                ignored += ignoreField[k] ? 1 : 0;
            }
            double ell = 0;
            for (int i = 0; i < xs.length; i++) {
                for (int j = 0; j < 2; j++) {
                    double term = j == 0 ? Math.log(1 - alpha) : Math.log(alpha);
                    for (int k = 0; k < xs[0].length; k++) {
                        if (ignoreField[k]) continue;
                        term -= (xs[i][k] - j) * (xs[i][k] - j) / (2 * sigma[j][k] * sigma[j][k])
                                + Math.log(sigma[j][k]);
                    }
                    ell += Ps[j][i] * term;
                }
            }
            //  If we ignore more fields than the previous iteration,
            //  the change in E(LL) may not reflect the change in parameters.
            //  So previous delta is kept to iterate once more.
            if (ignored == prevIgnored) deltaELL = ell - prevELL;
            System.out.println("prevELL: " + prevELL + "\tELL: " + ell + "\tdeltaELL: " + deltaELL);
            prevELL = ell;
            prevIgnored = ignored;
        }
        /*
        * Done iterating, and start predicting labels.
        * */
        double[] ret = new double[xs.length];
        for (int i = 0; i < xs.length; i++) {
            double[] prod = new double[] {1, 1};
            for (int k = 0; k < xs[0].length; k++) {
                if (ignoreField[k]) continue;
                prod[0] *= normalAt(0, sigma[0][k], xs[i][k]);
                prod[1] *= normalAt(1, sigma[1][k], xs[i][k]);
            }
            prod[0] *= 1 - alpha;
            prod[1] *= alpha;
            ret[i] = prod[1] / (prod[0] + prod[1]);
        }
        return ret;
    }

    private static double normalAt(double mu, double sigma, double x) {
        return Math.exp(((x - mu) * (x - mu)) / (sigma * sigma * -2))
                / (sigma * Math.sqrt(2 * Math.PI));
    }

}
