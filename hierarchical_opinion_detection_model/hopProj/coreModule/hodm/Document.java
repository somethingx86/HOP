package hodm;

import java.util.ArrayList;//ArrayList�ǲ����������ʼ����
import java.util.Vector;//vector��setsize,�����Ԫ�ػᱻ��ʼ��ΪNull,���������ʼ��
import java.lang.Math;
import funcUtil.Sums;
import funcUtil.SampleFromProb;

/**
 * 
 * @author ZhuLixing
 *
 */
public class Document {
	public ArrayList<Integer> phrases;//ÿ��phrase,�����ֱ�ʾ,����w_{m,n}
	public ArrayList<Integer> levels;//ÿ��phrase������level,����z_{m,n}

	public int id;//d_{m}�е�m,��ʾ��idƪ�ĵ�,��0��ʼ
	public ArrayList<Topic> path;//��Ӧһ��CRP����path
	
	//�����Ǽ���˹����ʱ���õ�����ʱ����
	public ArrayList<Double> tot_levels;//����˹����ʱ���õ�����ʱ����,һƪ�ĵ���ÿ������phrase����
	public ArrayList<Double> log_p_levels;//����˹����ʱ���õ�����ʱ����,��p71 <*>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
	
	/*
	 * ������ƪ�ĵ��Ĳ���,����gibbsState��ʼ��ʱʹ��һ��,������ƪ�ĵ��Ĳ���,��topic��nWordsToTopic������ֻ��������,��Ϊֻ�ǳ�ʼ������,���ذ��ղ�����ʽ��
	 */
	public void sample_doc_levels_withThisDoc(int depth,Vector<Double> alpha,Vector<Double> eta)
	{
		int i;
		Vector<Double> vec_log_prob=new Vector<Double>(depth);//p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
		vec_log_prob.setSize(depth);//p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
		
		//Ϊÿ��phrase��sample level
		for(i=0;i<this.phrases.size();++i)
		{
			int word=this.phrases.get(i);//�����z_{d,n}�Ķ�Ӧ��phrase.��ֻ��һ��phrase�ı��
			//����p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
			compute_vec_log_prob_level(depth,alpha);//����p71 <*>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�,������this.log_p_levels��
			int l;//��ʱ����,����level
			for(l=0;l<depth;++l)
			{
				vec_log_prob.set(l,this.log_p_levels.get(l)+this.path.get(l).posterior_log_prob_phrases.get(word));//posterior_log_prob_word.get(word)�ڳ�ʼ����ʱ����ֵ��ʵ��1/V
			}
			
			//����,����,�õ�new z_{d,n},����vec_log_prob
			int new_zdn=SampleFromProb.sample_from_log_prob(vec_log_prob);//����//log_p_levels�ѱ���ֵ,���в���
			Topic topicToUpdate=this.path.get(new_zdn);
			topicToUpdate.topic_update_nPhrasesToTopic(word, 1,new_zdn,eta);//ÿ��topic��Ͻ��phrase��Ҫ����
			this.levels.set(i,new_zdn);//��Ӧphrase������level��z��ȻҪ����
			document_update_level(new_zdn,1);//��Ӧ����䵽��phrase������ȻҪ����,����CRPNode�е����ݿɲ���Ҫ����//�ݹ�ȫ��,�洢���ݵĲ�����DocumentArray��CRPNode�Լ�topics,������Щ����ȷʵ��������һ�����Ͽ�
		}
		vec_log_prob=null;//��ʽ�ͷ�
	}
	
	/*
	 * ��ȥthis��ƪ�ĵ��Ĳ���
	 */
	public void sample_doc_levels_removeThisDoc(int depth,Vector<Double> alpha,Vector<Double> eta)
	{
		int i;
		Vector<Double> vec_log_prob=new Vector<Double>(depth);//p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
		vec_log_prob.setSize(depth);//p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
		
		//Ϊÿ��phrase��sample level
		for(i=0;i<this.phrases.size();++i)
		{
			int word=this.phrases.get(i);//�����z_{d,n}��word
			
			//�������þ����ų�ĳ��phrases��z�����в���,������Ϊ�ǳ�ȥ��ƪ�ĵ�֮���seating arrangement����phrase�������.
			//�������topic��Ȼ�Ǵ��ڵ�,ֻ������Ӧ��topic��phrasecount��ĳ��key�Ͽ���Ϊ0,��Ӧ��topic��phrasecountҲ����Ϊ0.��gibbsIterate��,do_remove=1�ǳ�̬
            int doc_level_l = this.levels.get(i);
            document_update_level(doc_level_l, -1);//�����˵�level���topic��phrase��-1,Ҳ�����ų��˱�phrase
            Topic node_level_l=this.path.get(doc_level_l);
            node_level_l.topic_update_nPhrasesToTopic(word, -1, doc_level_l,eta);//��level���Ӧ��topic���䵽��phrase����-1
			
			//����p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
			compute_vec_log_prob_level(depth,alpha);//����p71 <*>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�,������this.log_p_levels��
			int l;//��ʱ����,����level
			for(l=0;l<depth;++l)
			{
				vec_log_prob.set(l,this.log_p_levels.get(l)+this.path.get(l).posterior_log_prob_phrases.get(word));//posterior_log_prob_word.get(word)�ڳ�ʼ����ʱ����ֵ��ʵ��1/V
			}
			
			//����,����,�õ�new z_{d,n},����vec_log_prob
			int new_zdn=SampleFromProb.sample_from_log_prob(vec_log_prob);//����//log_p_levels�ѱ���ֵ,���в���
			Topic topicToUpdate=this.path.get(new_zdn);
			topicToUpdate.topic_update_nPhrasesToTopic(word, 1, new_zdn,eta);//ÿ��topic��Ͻ��phrase��Ҫ����
			this.levels.set(i,new_zdn);//��Ӧphrase������level��z��ȻҪ����
			document_update_level(new_zdn,1);//��Ӧ����䵽��phrase������ȻҪ����,����CRPNode�е����ݿɲ���Ҫ����//�ݹ�ȫ��,�洢���ݵĲ�����DocumentArray��CRPNode�Լ�topics,������Щ����ȷʵ��������һ�����Ͽ�
		}
		vec_log_prob=null;//��ʽ�ͷ�
		
		
	}

	
	/*
	 * ����p71 <*>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
	 * �������log_p_levels����ֵ
	 */
	private void compute_vec_log_prob_level(int depth,Vector<Double> alpha)
	{
		int l;
		double levels_sum;//ÿ������phrase������֮��,�Դ�gibbs_init_state���ú�,Ӧ�ú�word.size()���
		
		levels_sum=0;
		for(l=0;l<depth;++l)
		{
			levels_sum+=this.tot_levels.get(l);//���,��gibbs_init_state���õ�ʱ��ҲӦ����0
		}
		
		for(l=0;l<depth;++l)//��Ϊ��dir�ֲ�������gem�ֲ�,��������û��Сѭ��
		{
			double log_p_level=Math.log(this.tot_levels.get(l)+alpha.get(l))-Math.log(levels_sum+depth*alpha.get(l));//��ʼֵ��Ϊ��initGibbsState�б���ʼ��Ϊ0�Ĺ�ϵ,���Ծ���1/L,����L����depth
			this.log_p_levels.set(l,log_p_level);
		}
	}
	
	/*
	 * ����topic.topic_update_nWordsToTopic
	 * document.tot_levels[level]+=update,��������document.log_p_levels[level],��Ϊ������log_p_levels������log_levels
	 */
	private void document_update_level(int level,int update)
	{
		double newlevelval=this.tot_levels.get(level);
		newlevelval+=update;
		this.tot_levels.set(level,newlevelval);
	}
	
}
