package hodm;

import java.util.ArrayList;
import java.util.Vector;
import java.lang.Math;
import funcUtil.SampleFromProb;

public class Topic {
	
	//treeNode�����Ϣ
	public ArrayList<Topic> childs;
	public Topic parent;//ָ�򸸽��
	
	public int level;//����������ڵĲ�
	public int nchilds;//�������ӵ�еĺ��ӽ������ 
	
	public double prob;//�Ӹýڵ�Ķ��ӿ�ʼΪnew�ĸ���,��һ��ֱ��depth,����sample_document_path���õ�
	
	//��ˮ��topic�����Ϣ
	
	public double nTotalWordsToTopic;//��������topic���ܵ�������
	public ArrayList<Double> nWordsToTopic;//��������topic�ĸ���term�ĵ�������,�Ǹ�����
	public ArrayList<Double> posterior_log_prob_words;//�����û��ô��,����ճ��洢��ʵ������count(w)+\eta/count(\vec{n})+\eta*V
	
	public double nTotalDocumentToTopic;//��������topic�����ĵ�����
	public double log_nTotalDocumentToTopic;//��������topic�����ĵ�������logֵ,�����汣��һ��
	int id;//topic�Ķ�һ�޶���id��,��0��ʼ��ţ����ǲ���һ���Ǵ�С�����˳��,��Ϊ
	
	/*
	 * ��ʼ��һ������,���ҳ�ʼ��һ��topic
	 */
	public Topic(int nterms,int level,Topic parent,int nextTopicId,Vector<Double> eta)
	{
		
		this.level=level;
		this.nchilds=0;
		this.childs=new ArrayList<Topic>();//��ʼ��,��û�к���,Ҳ��֪�����м�������//��������ǵ�һ��Ļ�,�ǿ���PriorTreeNode,����ҪԤ���ռ��,����,����һ��,���ǾͲ��ܵ���ArrayList.size��,����û��,������nChilds
		if(this.level<=0)//level=0�Ļ�,����priorTreeNode,��Ӧ���޸������
		{
			for(int i=0;i<2;++i)
			{
				this.childs.add(null);//���ϲ��removeҲҪ��,�������Ǻ�
			}
		}
		
		this.parent=parent;//�趨���ڵ�
		this.topicInit(nterms,nextTopicId,eta,level);
	}
	
	/*
	 * ��topic�����Ϣ�ĳ�ʼ��
	 */
	private void topicInit(int nterms,int id,java.util.Vector<Double> eta,int level)
	{
		this.nTotalWordsToTopic=0;
		this.nWordsToTopic=new ArrayList<Double>(8192);//��ʼ�����趨Ϊ8192��ʵ�Ͽ����б�8192��ĵ���
		this.posterior_log_prob_words=new ArrayList<Double>(8192);
		
		for(int i=0;i<nterms;++i)//�����������!��Ϊ�ҵ��ù��ü���posterior_log_prob_words.size(),Ӧ����nterms��ʼ��
		{
			this.nWordsToTopic.add(0.0);
			this.posterior_log_prob_words.add(null);
		}
		
		this.nTotalDocumentToTopic=0;
		this.log_nTotalDocumentToTopic=0;//��ʼ����������topic�����ĵ�������logֵ,0�ǲ�����ȡ����
		
		this.id=id;
		
		//��ʼ��log_prob_word,��ǰ����Symmetric Dirichlet��,��Ȼ���ĳ�ʼֵΪlog(1/V)
		double value_eta=eta.get(level);
		double value_posterior_log_p_w=java.lang.Math.log(value_eta)-java.lang.Math.log(value_eta*nterms);
		for(int i=0;i<nWordsToTopic.size();++i)
		{
			this.posterior_log_prob_words.set(i, value_posterior_log_p_w);
		}
	}
	
	/*
	 * ���·�������topic��document������,topic.nTotalDocumentToTopic+=update,ͬʱ����log_probability
	 */
	public void topic_update_nDocumentsToTopic(int update)
	{
		this.nTotalDocumentToTopic+=update;
		this.log_nTotalDocumentToTopic=java.lang.Math.log(this.nTotalDocumentToTopic);
	}
	
	/*
	 * topic.nWordsToTopic[word]+=update,ͬʱ����topic.posterior_log_prob_word[word],topic.nTotalWordsToTopicҲ����
	 */
	public void topic_update_nWordsToTopic(int word,int update,int topic_level,Vector<Double> eta)
	{
		//����this.nWordsToTopic[word]
		double nword=this.nWordsToTopic.get(word);
		nword=nword+update;
		this.nWordsToTopic.set(word,nword);
		
		//����nTotalWordsToTopic
		this.nTotalWordsToTopic+=update;
		
		//����topic.posterior_log_prob_word[word]
		double val_eta=eta.get(topic_level);
		double posterior_log_prob_word=Math.log(this.nWordsToTopic.get(word)+val_eta)-Math.log(this.nTotalWordsToTopic+this.nWordsToTopic.size()*val_eta);
		this.posterior_log_prob_words.set(word,posterior_log_prob_word);
	}
	

	//�ָ��� ���������ڹ���node�ķ���
	/*
	 * �ڱ�CRPTreeNode,��topic��childs����ĩβ��һ��child,������child������,ע���׽����crp���
	 */
	public Topic CRPAsParentAddChild(int nterms,int nextTopicId,Vector<Double> eta)
	{
		this.nchilds++;//���Ӻ��ӽ��ļ���
		Topic newChild=new Topic(nterms,this.level+1,this,nextTopicId,eta);
		this.childs.add(newChild);//��ĩβ��һ�����ӽ��
		return newChild;
	}
	
	/*
	 * �׽����PriorTreeNode
	 */
	public Topic priorAsParentaddChild(int nterms,int nextTopicId,Vector<Double> eta,int idx)
	{
		this.nchilds++;//���Ӻ��ӽ��ļ���
		Topic newChild=new Topic(nterms,this.level+1,this,nextTopicId,eta);
		this.childs.set(idx, newChild);//��ָ��λ������һ�����ӽ��,��Ϊ��slot��new��ʱ���Ѿ�׼������
		return newChild;
	}
}
