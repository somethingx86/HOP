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
	public ArrayList<Integer> words;//ÿ������,�����ֱ�ʾ,����w_{m,n}
	public ArrayList<Integer> levels;//ÿ�����ʹ�����level,����z_{m,n}
	public ArrayList<Integer> words_stance_flag;//�����ʱ����Ϊ1,������д�,���Ȼ������һ��
	
	public int id;//d_{m}�е�m,��ʾ��idƪ�ĵ�,��0��ʼ
	public ArrayList<Topic> path;//��Ӧһ��CRP����path
	public int stance_flag;//���ĵ��ڶ����Ƿ񱻱��,��Ϊ0,��ڶ�����ʰ�ԭ������,��Ϊ1,��ڶ�����������仯,��Ȼ��,��Ϊ-1,���Ȼ��
	
	//�����Ǽ���˹����ʱ���õ�����ʱ����
	public ArrayList<Double> tot_levels;//����˹����ʱ���õ�����ʱ����,һƪ�ĵ���ÿ�����ĵ�������
	public ArrayList<Double> log_p_levels;//����˹����ʱ���õ�����ʱ����,��p71 <*>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
	
	/*
	 * ������ƪ�ĵ��Ĳ���,����gibbsState��ʼ��ʱʹ��һ��,������ƪ�ĵ��Ĳ���,��topic��nWordsToTopic������ֻ��������,��Ϊ
	 */
	public void sample_doc_levels_withThisDoc(int depth,Vector<Double> alpha,Vector<Double> eta)
	{
		int i;
		Vector<Double> vec_log_prob=new Vector<Double>(depth);//p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
		vec_log_prob.setSize(depth);//p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
		
		//Ϊÿ��word��sample level
		for(i=0;i<this.words.size();++i)
		{
			
			int tag_whether_stance_word=this.words_stance_flag.get(i);//z_{d,n}��word��Ӧ��stance
			int new_zdn=-1;//�²�����z_{d,n}
			int word=-1;//���word��Ӧ�ı��
			
			
			if(tag_whether_stance_word==0)//������д�
			{
				word=this.words.get(i);//�����z_{d,n}��word
			
				//����p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
				compute_vec_log_prob_level(depth,alpha);//����p71 <*>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�,������this.log_p_levels��
				int l;//��ʱ����,����level
				for(l=0;l<depth;++l)
				{
					vec_log_prob.set(l,this.log_p_levels.get(l)+this.path.get(l).posterior_log_prob_words.get(word));//posterior_log_prob_word.get(word)�ڳ�ʼ����ʱ����ֵ��ʵ��1/V
				}
			
				//����,����,�õ�new z_{d,n},����vec_log_prob
				new_zdn=SampleFromProb.sample_from_log_prob(vec_log_prob);//����//log_p_levels�ѱ���ֵ,���в���
				Topic topicToUpdate=this.path.get(new_zdn);
				topicToUpdate.topic_update_nWordsToTopic(word, 1,new_zdn,eta);//ÿ��topic��Ͻ�ĵ�����Ҫ����
				this.levels.set(i,new_zdn);//��Ӧ���ʹ�����level��z��ȻҪ����
				document_update_level(new_zdn,1);//��Ӧ����䵽�ĵ���������ȻҪ����,����CRPNode�е����ݿɲ���Ҫ����//�ݹ�ȫ��,�洢���ݵĲ�����DocumentArray��CRPNode�Լ�topics,������Щ����ȷʵ��������һ�����Ͽ�
			}
			else//����д�
			{
				word=this.words.get(i);//�����z_{d,n}��word
				
				new_zdn=1;
				//����
				Topic topicToUpdate=this.path.get(new_zdn);
				topicToUpdate.topic_update_nWordsToTopic(word, 1, new_zdn,eta);//ÿ��topic��Ͻ�ĵ�����Ҫ����
				this.levels.set(i,new_zdn);//��Ӧ���ʹ�����level��z��ȻҪ����
				document_update_level(new_zdn,1);//��Ӧ����䵽�ĵ���������ȻҪ����,����CRPNode�е����ݿɲ���Ҫ����//�ݹ�ȫ��,�洢���ݵĲ�����DocumentArray��CRPNode�Լ�topics,������Щ����ȷʵ��������һ�����Ͽ�				
			}
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
		
		//Ϊÿ��word��sample level,�����word��stance word,�򲻽��в���,�Զ���Ϊ��1��(�±�Ϊ1,ʵ��Ϊ��2��)
		for(i=0;i<this.words.size();++i)
		{
			int tag_whether_stance_word=this.words_stance_flag.get(i);//z_{d,n}��word��Ӧ��stance
			int new_zdn=-1;//�²�����z_{d,n}
			int word=-1;//���word��Ӧ�ı��
			
			//�����stance_word,��Ҫ����,����,������
			if(tag_whether_stance_word==0)//������д�
			{
			
				word=this.words.get(i);//�����z_{d,n}��word
				
				//�������þ����ų�ĳ��word��z�����в���,�������topic��Ȼ�Ǵ��ڵ�,ֻ������Ӧ��topic��wordcount��ĳ��term�Ͽ���Ϊ0,��Ӧ��topic��wordcountҲ����Ϊ0.��gibbsIterate��,do_remove=1�ǳ�̬
	            int doc_level_l = this.levels.get(i);
	            document_update_level(doc_level_l, -1);//��level����䵽��topic����-1
	            Topic node_level_l=this.path.get(doc_level_l);
	            node_level_l.topic_update_nWordsToTopic(word, -1, doc_level_l,eta);//��level���Ӧ��topic���䵽�ĵ�������-1
				
				//����p70 <**>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�
				compute_vec_log_prob_level(depth,alpha);//����p71 <*>ʽ��z_{d,n}ȡ����ֵʱ�ķֲ�,������this.log_p_levels��
				int l;//��ʱ����,����level
				for(l=0;l<depth;++l)
				{
					vec_log_prob.set(l,this.log_p_levels.get(l)+this.path.get(l).posterior_log_prob_words.get(word));//posterior_log_prob_word.get(word)�ڳ�ʼ����ʱ����ֵ��ʵ��1/V
				}
			
				//����,����,�õ�new z_{d,n},����vec_log_prob
				new_zdn=SampleFromProb.sample_from_log_prob(vec_log_prob);//����//log_p_levels�ѱ���ֵ,���в���
				
				//����
				Topic topicToUpdate=this.path.get(new_zdn);
				topicToUpdate.topic_update_nWordsToTopic(word, 1, new_zdn,eta);//ÿ��topic��Ͻ�ĵ�����Ҫ����
				this.levels.set(i,new_zdn);//��Ӧ���ʹ�����level��z��ȻҪ����
				document_update_level(new_zdn,1);//��Ӧ����䵽�ĵ���������ȻҪ����,����CRPNode�е����ݿɲ���Ҫ����//�ݹ�ȫ��,�洢���ݵĲ�����DocumentArray��CRPNode�Լ�topics,������Щ����ȷʵ��������һ�����Ͽ�
			}
			else//����д�
			{
				word=this.words.get(i);//�����z_{d,n}��word
				
				//�������þ����ų�ĳ��word��z�����в���,�������topic��Ȼ�Ǵ��ڵ�,ֻ������Ӧ��topic��wordcount��ĳ��term�Ͽ���Ϊ0,��Ӧ��topic��wordcountҲ����Ϊ0.��gibbsIterate��,do_remove=1�ǳ�̬
	            int doc_level_l = this.levels.get(i);
	            if(doc_level_l!=1)
	            {
	            	System.err.print("Error: Stance word not belong to level 1");
	            }
	            
	            //document_update_level(doc_level_l, -1);//��level����䵽��topic����-1
	            //Topic node_level_l=this.path.get(doc_level_l);
	            //node_level_l.topic_update_nWordsToTopic(word, -1, doc_level_l,eta);//��level���Ӧ��topic���䵽�ĵ�������-1
				
				//new_zdn=1;//�̶��ڵڶ���
			}
			
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
		double levels_sum;//ÿ�����ĵ��ʵ�����֮��,�Դ�gibbs_init_state���ú�,Ӧ�ú�word.size()���
		
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
