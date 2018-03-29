package funcUtil;

import java.lang.Math;
import java.util.Vector;


/**
 * �Ӹ��ָ����ĸ��ʷֲ�����
 * @author ZhuLixing
 *
 */
public class SampleFromProb {

	/*
	 * ��log���ʷֲ�����
	 */
	public static int sample_from_log_prob(Vector<Double> vec_log_prob)
	{
		double logsum;//��һ������ֵ
		int i;
		logsum=vec_log_prob.get(0);
		for(i=1;i<vec_log_prob.size();++i)
		{
			logsum=Sums.log_sum(logsum,vec_log_prob.get(i));
		}
		
		double uni_rand_x=Math.random();//����0-1�ֲ�,��α�����,������seed����ϵͳʱ����仯��
		int result=-1;//ѡ����z
		double interval_left=0;
		do
		{
			++result;
			interval_left=interval_left+Math.exp(vec_log_prob.get(result)-logsum);//��������ֵ
		}while(uni_rand_x>=interval_left);
		
		return result;
		
	}
}
