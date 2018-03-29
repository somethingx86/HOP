package funcUtil;


/**
 * 各种gamma函数的近似计算方法
 * @author ZhuLixing
 *
 */
public class Dirich {
	public static final double HALF_LOG_TWO_PI = Math.log(2 * Math.PI) / 2;
	
	/**
	 * Use real Lanczos method to calculate logGamma value. Uses Lanczos approximation formula
	 * Default logGammaFunction
	 * @param x
	 * @return logGamma value
	 */	
	public static double logGammaLanczos(double x){
		 double[] coef = new double[] { 76.18009172947146,
				    -86.50532032941677, 24.01409824083091,
				    -1.231739572450155, 0.1208650973866179E-2,
				    -0.5395239384953E-5 };
		 
				            double LogSqrtTwoPi = 0.91893853320467274178;
				            double denom = x + 1;
				            double y = x + 5.5;
				            double series = 1.000000000190015;
				            for (int i = 0; i < 6; ++i)
				            {
				                series += coef[i] / denom;
				                denom += 1.0;
				            }
				            return (LogSqrtTwoPi + (x + 0.5) * Math.log(y) -
				            y + Math.log(series / x));
	}
	
	/**
	 * Use real Lanczos method to calculate logGamma value. Uses Lanczos approximation formula,Not Precise
	 * Default logGammaFunction
	 * @param x
	 * @return logGamma value
	 */	
	public static double logGammaLanczosNotPrecise(double x) {
	      double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
	      double ser = 1.000000000190015 + 76.18009172947146    / (x + 0)   - 86.50532032941677    / (x + 1)
	                       + 24.01409824083091    / (x + 2)   - 1.231739572450155   / (x + 3)
	                       +  0.1208650973866179E-2 / (x + 4)   -  0.5395239384953E-5 / (x + 5);
	      return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
	   }
	
	public static double gammaLanczos(double x) { return Math.exp(logGammaLanczos(x)); }
	
	/** Use a fifth order Stirling's approximation.
	 * 
	 *	@param z Note that Stirling's approximation is increasingly unstable as <code>z</code> approaches 0. If <code>z</code> is less than 2, we shift it up, calculate the approximation, and then shift the answer back down.
	 */
	public static double logGammaStirling(double z) {
		int shift = 0;
		while (z < 2) {
			z++;
			shift++;
		}

		double result = HALF_LOG_TWO_PI + (z - 0.5) * Math.log(z) - z +
		1/(12 * z) - 1 / (360 * z * z * z) + 1 / (1260 * z * z * z * z * z);

		while (shift > 0) {
			shift--;
			z--;
			result -= Math.log(z);
		}

		return result;
	}
	
	/** Gergo Nemes' approximation */
	public static double logGammaNemes(double z) {
		double result = HALF_LOG_TWO_PI - (Math.log(z) / 2) +
		z * (Math.log(z + (1/(12 * z - (1/(10*z))))) - 1);
		return result;
	}
	
	
}
