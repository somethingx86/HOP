package hodm;

import java.util.ArrayList;//ArrayList是不允许随机初始化的
import java.util.Vector;//vector的setsize,里面的元素会被初始化为Null,允许随机初始化
import java.lang.Math;
import funcUtil.Sums;
import funcUtil.SampleFromProb;

/**
 * 
 * @author ZhuLixing
 *
 */
public class Document {
	public ArrayList<Integer> words;//每个单词,用数字表示,就是w_{m,n}
	public ArrayList<Integer> levels;//每个单词归属的level,就是z_{m,n}
	public ArrayList<Integer> words_stance_flag;//若单词被标记为1,则是情感词,则必然归属第一层
	
	public int id;//d_{m}中的m,表示第id篇文档,从0开始
	public ArrayList<Topic> path;//对应一个CRP树的path
	public int stance_flag;//该文档第二层是否被标记,若为0,则第二层概率按原样来算,若为1,则第二层概率有所变化,必然左,若为-1,则必然右
	
	//下面是吉布斯采样时才用到的临时变量
	public ArrayList<Double> tot_levels;//吉布斯采样时才用到的临时变量,一篇文档中每层分配的单词数量
	public ArrayList<Double> log_p_levels;//吉布斯采样时才用到的临时变量,是p71 <*>式在z_{d,n}取各个值时的分布
	
	/*
	 * 带有这篇文档的采样,仅在gibbsState初始化时使用一次,带有这篇文档的采样,对topic的nWordsToTopic而言是只增不减的,因为
	 */
	public void sample_doc_levels_withThisDoc(int depth,Vector<Double> alpha,Vector<Double> eta)
	{
		int i;
		Vector<Double> vec_log_prob=new Vector<Double>(depth);//p70 <**>式在z_{d,n}取各个值时的分布
		vec_log_prob.setSize(depth);//p70 <**>式在z_{d,n}取各个值时的分布
		
		//为每个word来sample level
		for(i=0;i<this.words.size();++i)
		{
			
			int tag_whether_stance_word=this.words_stance_flag.get(i);//z_{d,n}的word对应的stance
			int new_zdn=-1;//新采样的z_{d,n}
			int word=-1;//这个word对应的编号
			
			
			if(tag_whether_stance_word==0)//不是情感词
			{
				word=this.words.get(i);//想采样z_{d,n}的word
			
				//计算p70 <**>式在z_{d,n}取各个值时的分布
				compute_vec_log_prob_level(depth,alpha);//计算p71 <*>式在z_{d,n}取各个值时的分布,储存在this.log_p_levels里
				int l;//临时变量,代表level
				for(l=0;l<depth;++l)
				{
					vec_log_prob.set(l,this.log_p_levels.get(l)+this.path.get(l).posterior_log_prob_words.get(word));//posterior_log_prob_word.get(word)在初始化的时候其值其实是1/V
				}
			
				//采样,更新,得到new z_{d,n},根据vec_log_prob
				new_zdn=SampleFromProb.sample_from_log_prob(vec_log_prob);//采样//log_p_levels已被赋值,进行采样
				Topic topicToUpdate=this.path.get(new_zdn);
				topicToUpdate.topic_update_nWordsToTopic(word, 1,new_zdn,eta);//每个topic下辖的单词数要更新
				this.levels.set(i,new_zdn);//对应单词归属的level即z当然要更新
				document_update_level(new_zdn,1);//对应层分配到的单词数量当然要更新,不过CRPNode中的数据可不需要更新//纵观全局,存储数据的部分有DocumentArray和CRPNode以及topics,有了这些数据确实可以重塑一个语料库
			}
			else//是情感词
			{
				word=this.words.get(i);//想采样z_{d,n}的word
				
				new_zdn=1;
				//更新
				Topic topicToUpdate=this.path.get(new_zdn);
				topicToUpdate.topic_update_nWordsToTopic(word, 1, new_zdn,eta);//每个topic下辖的单词数要更新
				this.levels.set(i,new_zdn);//对应单词归属的level即z当然要更新
				document_update_level(new_zdn,1);//对应层分配到的单词数量当然要更新,不过CRPNode中的数据可不需要更新//纵观全局,存储数据的部分有DocumentArray和CRPNode以及topics,有了这些数据确实可以重塑一个语料库				
			}
		}
		vec_log_prob=null;//显式释放
	}
	
	/*
	 * 除去this这篇文档的采样
	 */
	public void sample_doc_levels_removeThisDoc(int depth,Vector<Double> alpha,Vector<Double> eta)
	{
		int i;
		Vector<Double> vec_log_prob=new Vector<Double>(depth);//p70 <**>式在z_{d,n}取各个值时的分布
		vec_log_prob.setSize(depth);//p70 <**>式在z_{d,n}取各个值时的分布
		
		//为每个word来sample level,如果该word是stance word,则不进行采样,自动归为第1层(下标为1,实际为第2层)
		for(i=0;i<this.words.size();++i)
		{
			int tag_whether_stance_word=this.words_stance_flag.get(i);//z_{d,n}的word对应的stance
			int new_zdn=-1;//新采样的z_{d,n}
			int word=-1;//这个word对应的编号
			
			//如果是stance_word,则要采样,否则,不采样
			if(tag_whether_stance_word==0)//不是情感词
			{
			
				word=this.words.get(i);//想采样z_{d,n}的word
				
				//它的作用就是排除某个word的z来进行采样,但是这个topic任然是存在的,只不过对应的topic的wordcount在某个term上可能为0,对应的topic的wordcount也可能为0.在gibbsIterate中,do_remove=1是常态
	            int doc_level_l = this.levels.get(i);
	            document_update_level(doc_level_l, -1);//第level层分配到的topic数量-1
	            Topic node_level_l=this.path.get(doc_level_l);
	            node_level_l.topic_update_nWordsToTopic(word, -1, doc_level_l,eta);//第level层对应的topic分配到的单词数量-1
				
				//计算p70 <**>式在z_{d,n}取各个值时的分布
				compute_vec_log_prob_level(depth,alpha);//计算p71 <*>式在z_{d,n}取各个值时的分布,储存在this.log_p_levels里
				int l;//临时变量,代表level
				for(l=0;l<depth;++l)
				{
					vec_log_prob.set(l,this.log_p_levels.get(l)+this.path.get(l).posterior_log_prob_words.get(word));//posterior_log_prob_word.get(word)在初始化的时候其值其实是1/V
				}
			
				//采样,更新,得到new z_{d,n},根据vec_log_prob
				new_zdn=SampleFromProb.sample_from_log_prob(vec_log_prob);//采样//log_p_levels已被赋值,进行采样
				
				//更新
				Topic topicToUpdate=this.path.get(new_zdn);
				topicToUpdate.topic_update_nWordsToTopic(word, 1, new_zdn,eta);//每个topic下辖的单词数要更新
				this.levels.set(i,new_zdn);//对应单词归属的level即z当然要更新
				document_update_level(new_zdn,1);//对应层分配到的单词数量当然要更新,不过CRPNode中的数据可不需要更新//纵观全局,存储数据的部分有DocumentArray和CRPNode以及topics,有了这些数据确实可以重塑一个语料库
			}
			else//是情感词
			{
				word=this.words.get(i);//想采样z_{d,n}的word
				
				//它的作用就是排除某个word的z来进行采样,但是这个topic任然是存在的,只不过对应的topic的wordcount在某个term上可能为0,对应的topic的wordcount也可能为0.在gibbsIterate中,do_remove=1是常态
	            int doc_level_l = this.levels.get(i);
	            if(doc_level_l!=1)
	            {
	            	System.err.print("Error: Stance word not belong to level 1");
	            }
	            
	            //document_update_level(doc_level_l, -1);//第level层分配到的topic数量-1
	            //Topic node_level_l=this.path.get(doc_level_l);
	            //node_level_l.topic_update_nWordsToTopic(word, -1, doc_level_l,eta);//第level层对应的topic分配到的单词数量-1
				
				//new_zdn=1;//固定在第二层
			}
			
		}
		vec_log_prob=null;//显式释放
		
	}

	
	/*
	 * 计算p71 <*>式在z_{d,n}取各个值时的分布
	 * 计算完后log_p_levels被赋值
	 */
	private void compute_vec_log_prob_level(int depth,Vector<Double> alpha)
	{
		int l;
		double levels_sum;//每层分配的单词的数量之和,自从gibbs_init_state调用后,应该和word.size()相等
		
		levels_sum=0;
		for(l=0;l<depth;++l)
		{
			levels_sum+=this.tot_levels.get(l);//这个,在gibbs_init_state调用的时候也应该是0
		}
		
		for(l=0;l<depth;++l)//因为是dir分布而不是gem分布,所以里面没有小循环
		{
			double log_p_level=Math.log(this.tot_levels.get(l)+alpha.get(l))-Math.log(levels_sum+depth*alpha.get(l));//初始值因为在initGibbsState中被初始化为0的关系,所以就是1/L,其中L就是depth
			this.log_p_levels.set(l,log_p_level);
		}
	}
	
	/*
	 * 类似topic.topic_update_nWordsToTopic
	 * document.tot_levels[level]+=update,但不更新document.log_p_levels[level],因为这里是log_p_levels而不是log_levels
	 */
	private void document_update_level(int level,int update)
	{
		double newlevelval=this.tot_levels.get(level);
		newlevelval+=update;
		this.tot_levels.set(level,newlevelval);
	}
	
}
