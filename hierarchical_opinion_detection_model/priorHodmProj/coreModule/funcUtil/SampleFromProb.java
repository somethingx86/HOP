package funcUtil;

import java.lang.Math;
import java.util.Vector;


/**
 * 从各种各样的概率分布采样
 * @author ZhuLixing
 *
 */
public class SampleFromProb {

	/*
	 * 从log概率分布采样
	 */
	public static int sample_from_log_prob(Vector<Double> vec_log_prob)
	{
		double logsum;//归一化分子值
		int i;
		logsum=vec_log_prob.get(0);
		for(i=1;i<vec_log_prob.size();++i)
		{
			logsum=Sums.log_sum(logsum,vec_log_prob.get(i));
		}
		
		double uni_rand_x=Math.random();//均匀0-1分布,是伪随机数,但是其seed是随系统时间而变化的
		int result=-1;//选定的z
		double interval_left=0;
		do
		{
			++result;
			interval_left=interval_left+Math.exp(vec_log_prob.get(result)-logsum);//闭区间左值
		}while(uni_rand_x>=interval_left);
		
		return result;
		
	}
}
