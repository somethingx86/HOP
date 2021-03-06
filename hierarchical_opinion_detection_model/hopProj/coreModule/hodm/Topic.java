package hodm;

import java.util.ArrayList;
import java.util.Vector;
import java.lang.Math;
import funcUtil.SampleFromProb;

public class Topic {
	
	//treeNode相关信息
	public ArrayList<Topic> childs;
	public Topic parent;//指向父结点
	
	public int level;//该树结点所在的层
	public int nchilds;//该树结点拥有的孩子结点数量 
	
	public double prob;//从该节点的儿子开始为new的概率,归一化直到depth,仅在sample_document_path中用到
	
	//分水岭topic相关信息
	
	public double nTotalPhrasesToTopic;//分配给这个topic的总单词数量
	public ArrayList<Double> nPhrasesToTopic;//分配给这个topic的各个term的单词数量,是个向量
	public ArrayList<Double> posterior_log_prob_phrases;//这个可没这么简单,这个日常存储的实际上是count(w)+\eta/count(\vec{n})+\eta*V
	
	public double nTotalDocumentToTopic;//分配给这个topic的总文档数量
	public double log_nTotalDocumentToTopic;//分配给这个topic的总文档数量的log值,和上面保持一致
	int id;//topic的独一无二的id号,从0开始编号，但是并不一定是从小到大的顺序,因为
	
	/*
	 * 初始化一棵树根,并且初始化一下topic
	 */
	public Topic(int nkeys,int level,Topic parent,int nextTopicId,Vector<Double> eta)
	{
		
		this.level=level;
		this.nchilds=0;
		this.childs=new ArrayList<Topic>();//初始化,还没有孩子,也不知道会有几个孩子//但是如果是第一层的话,那可是PriorTreeNode,是需要预留空间的,不过,这样一来,我们就不能调用ArrayList.size了,不过没事,我们有nChilds
		if(this.level<=0)//level=0的话,就是priorTreeNode,是应该修改先验的
		{
			for(int i=0;i<2;++i)
			{
				this.childs.add(null);//对上层的remove也要改,不过这是后话
			}
		}
		
		this.parent=parent;//设定父节点
		this.topicInit(nkeys,nextTopicId,eta,level);
	}
	
	/*
	 * 和topic相关信息的初始化
	 */
	private void topicInit(int nkeys,int id,java.util.Vector<Double> eta,int level)
	{
		this.nTotalPhrasesToTopic=0;
		this.nPhrasesToTopic=new ArrayList<Double>(8192);//初始容量设定为8192事实上可能有比8192多的Phrase Keys
		this.posterior_log_prob_phrases=new ArrayList<Double>(8192);
		
		for(int i=0;i<nkeys;++i)//这样会出问题!因为我调用过好几次posterior_log_prob_phrases.size(),应该用nkeys初始化
		{
			this.nPhrasesToTopic.add(0.0);
			this.posterior_log_prob_phrases.add(null);
		}
		
		this.nTotalDocumentToTopic=0;
		this.log_nTotalDocumentToTopic=0;//初始化分配给这个topic的总文档数量的log值,0是不可能取到的
		
		this.id=id;
		
		//初始化log_prob_word,当前不是Symmetric Dirichlet的,虽然它的初始值为log(1/V)
		double value_eta=eta.get(level);
		double value_posterior_log_p_w=java.lang.Math.log(value_eta)-java.lang.Math.log(value_eta*nkeys);
		for(int i=0;i<nPhrasesToTopic.size();++i)
		{
			this.posterior_log_prob_phrases.set(i, value_posterior_log_p_w);
		}
	}
	
	/*
	 * 更新分配给这个topic的document的数量,topic.nTotalDocumentToTopic+=update,同时更新log_probability
	 */
	public void topic_update_nDocumentsToTopic(int update)
	{
		this.nTotalDocumentToTopic+=update;
		this.log_nTotalDocumentToTopic=java.lang.Math.log(this.nTotalDocumentToTopic);
	}
	
	/*
	 * topic.nPhrasesToTopic[phrase]+=update,同时更新topic.posterior_log_prob_Phrase[phrase],topic.nTotalPhrasesToTopic也更新
	 */
	public void topic_update_nPhrasesToTopic(int phrase,int update,int topic_level,Vector<Double> eta)
	{
		//更新this.nPhrasesToTopic[phrase]
		double nphrase=this.nPhrasesToTopic.get(phrase);
		nphrase=nphrase+update;
		this.nPhrasesToTopic.set(phrase,nphrase);
		
		//更新nTotalPhrasesToTopic
		this.nTotalPhrasesToTopic+=update;
		
		//更新topic.posterior_log_prob_phrase[phrase]
		double val_eta=eta.get(topic_level);
		double posterior_log_prob_phrase=Math.log(this.nPhrasesToTopic.get(phrase)+val_eta)-Math.log(this.nTotalPhrasesToTopic+this.nPhrasesToTopic.size()*val_eta);
		this.posterior_log_prob_phrases.set(phrase,posterior_log_prob_phrase);
	}
	

	//分割线 下面是属于关于node的方法
	/*
	 * 在本CRPTreeNode,即topic的childs数组末尾加一个child,返回新child的引用,注意亲结点是crp结点
	 */
	public Topic CRPAsParentAddChild(int nkeys,int nextTopicId,Vector<Double> eta)
	{
		this.nchilds++;//增加孩子结点的计数
		Topic newChild=new Topic(nkeys,this.level+1,this,nextTopicId,eta);
		this.childs.add(newChild);//在末尾加一个孩子结点
		return newChild;
	}
	
	/*
	 * 亲结点是PriorTreeNode
	 */
	public Topic priorAsParentaddChild(int nkeys,int nextTopicId,Vector<Double> eta,int idx)
	{
		this.nchilds++;//增加孩子结点的计数
		Topic newChild=new Topic(nkeys,this.level+1,this,nextTopicId,eta);
		this.childs.set(idx, newChild);//在指定位置设置一个孩子结点,因为槽slot在new的时候已经准备好了
		return newChild;
	}
}
