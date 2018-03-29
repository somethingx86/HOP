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
	
	public double nTotalPhrasesToTopic;//��������topic���ܵ�������
	public ArrayList<Double> nPhrasesToTopic;//��������topic�ĸ���term�ĵ�������,�Ǹ�����
	public ArrayList<Double> posterior_log_prob_phrases;//�����û��ô��,����ճ��洢��ʵ������count(w)+\eta/count(\vec{n})+\eta*V
	
	public double nTotalDocumentToTopic;//��������topic�����ĵ�����
	public double log_nTotalDocumentToTopic;//��������topic�����ĵ�������logֵ,�����汣��һ��
	int id;//topic�Ķ�һ�޶���id��,��0��ʼ��ţ����ǲ���һ���Ǵ�С�����˳��,��Ϊ
	
	/*
	 * ��ʼ��һ������,���ҳ�ʼ��һ��topic
	 */
	public Topic(int nkeys,int level,Topic parent,int nextTopicId,Vector<Double> eta)
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
		this.topicInit(nkeys,nextTopicId,eta,level);
	}
	
	/*
	 * ��topic�����Ϣ�ĳ�ʼ��
	 */
	private void topicInit(int nkeys,int id,java.util.Vector<Double> eta,int level)
	{
		this.nTotalPhrasesToTopic=0;
		this.nPhrasesToTopic=new ArrayList<Double>(8192);//��ʼ�����趨Ϊ8192��ʵ�Ͽ����б�8192���Phrase Keys
		this.posterior_log_prob_phrases=new ArrayList<Double>(8192);
		
		for(int i=0;i<nkeys;++i)//�����������!��Ϊ�ҵ��ù��ü���posterior_log_prob_phrases.size(),Ӧ����nkeys��ʼ��
		{
			this.nPhrasesToTopic.add(0.0);
			this.posterior_log_prob_phrases.add(null);
		}
		
		this.nTotalDocumentToTopic=0;
		this.log_nTotalDocumentToTopic=0;//��ʼ����������topic�����ĵ�������logֵ,0�ǲ�����ȡ����
		
		this.id=id;
		
		//��ʼ��log_prob_word,��ǰ����Symmetric Dirichlet��,��Ȼ���ĳ�ʼֵΪlog(1/V)
		double value_eta=eta.get(level);
		double value_posterior_log_p_w=java.lang.Math.log(value_eta)-java.lang.Math.log(value_eta*nkeys);
		for(int i=0;i<nPhrasesToTopic.size();++i)
		{
			this.posterior_log_prob_phrases.set(i, value_posterior_log_p_w);
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
	 * topic.nPhrasesToTopic[phrase]+=update,ͬʱ����topic.posterior_log_prob_Phrase[phrase],topic.nTotalPhrasesToTopicҲ����
	 */
	public void topic_update_nPhrasesToTopic(int phrase,int update,int topic_level,Vector<Double> eta)
	{
		//����this.nPhrasesToTopic[phrase]
		double nphrase=this.nPhrasesToTopic.get(phrase);
		nphrase=nphrase+update;
		this.nPhrasesToTopic.set(phrase,nphrase);
		
		//����nTotalPhrasesToTopic
		this.nTotalPhrasesToTopic+=update;
		
		//����topic.posterior_log_prob_phrase[phrase]
		double val_eta=eta.get(topic_level);
		double posterior_log_prob_phrase=Math.log(this.nPhrasesToTopic.get(phrase)+val_eta)-Math.log(this.nTotalPhrasesToTopic+this.nPhrasesToTopic.size()*val_eta);
		this.posterior_log_prob_phrases.set(phrase,posterior_log_prob_phrase);
	}
	

	//�ָ��� ���������ڹ���node�ķ���
	/*
	 * �ڱ�CRPTreeNode,��topic��childs����ĩβ��һ��child,������child������,ע���׽����crp���
	 */
	public Topic CRPAsParentAddChild(int nkeys,int nextTopicId,Vector<Double> eta)
	{
		this.nchilds++;//���Ӻ��ӽ��ļ���
		Topic newChild=new Topic(nkeys,this.level+1,this,nextTopicId,eta);
		this.childs.add(newChild);//��ĩβ��һ�����ӽ��
		return newChild;
	}
	
	/*
	 * �׽����PriorTreeNode
	 */
	public Topic priorAsParentaddChild(int nkeys,int nextTopicId,Vector<Double> eta,int idx)
	{
		this.nchilds++;//���Ӻ��ӽ��ļ���
		Topic newChild=new Topic(nkeys,this.level+1,this,nextTopicId,eta);
		this.childs.set(idx, newChild);//��ָ��λ������һ�����ӽ��,��Ϊ��slot��new��ʱ���Ѿ�׼������
		return newChild;
	}
}
