package funcUtil;

import java.lang.Math;

/**
 * 各种加法,如log_sum
 * @author ZhuLixing
 *
 */
public class Sums {
	
	/*
	 * 输入log(a),log(b),输出log(a+b)
	 */
	public static double log_sum(double log_a,double log_b)
	{
		  if (log_a < log_b)
		      return(log_b+Math.log(1 + Math.exp(log_a-log_b)));
		  else
		      return(log_a+Math.log(1 + Math.exp(log_b-log_a)));
	}
}
