package funcUtil;

import java.lang.Math;

/**
 * ���ּӷ�,��log_sum
 * @author ZhuLixing
 *
 */
public class Sums {
	
	/*
	 * ����log(a),log(b),���log(a+b)
	 */
	public static double log_sum(double log_a,double log_b)
	{
		  if (log_a < log_b)
		      return(log_b+Math.log(1 + Math.exp(log_a-log_b)));
		  else
		      return(log_a+Math.log(1 + Math.exp(log_b-log_a)));
	}
}
