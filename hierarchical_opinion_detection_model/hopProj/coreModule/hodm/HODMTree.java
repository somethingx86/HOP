package hodm;

import java.util.ArrayList;
import java.util.Vector;
import funcUtil.Dirich;
import java.lang.Math;
import funcUtil.Sums;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * CRP树
 * @author ZhuLixing
 *
 */
public class HODMTree{
	public Topic root;//blei中的root是每个treenode指向的自己对应的topic
	public int depth;//树深度
	public double gamma;//NCRP中的常数gamma,由树掌管,和每个CRPTreeNode,每个topic中的gamma保持一致
	public double lambda=0.5;//第二层的超参,是个常量,认为有0.4的概率是支持的
	public int new_topic_id;//topic的数量,同时也是下一个topic的编号
	//public int topics
	public HODMTree(int depth,int nkeys,Vector<Double> eta,double gamma)
	{
		this.new_topic_id=0;
		root=new Topic(nkeys,0,null,this.new_topic_id,eta);//生成新的树节点,即生成一个新的topic
		this.new_topic_id+=1;
		this.depth=depth;
		this.gamma=gamma;
	}
	
	/*
	 * 既增加结点又安排路径,和下面相比,多了个第一层结点控制,少了个报错功能
	 */
	public Topic priorTreeFillNodeInInitState(Topic parent,int nkeys,Vector<Double> eta)
	{
		/*
		 * 第二层新增1结点,如果第二层有2个结点则不予增加,第三层至以后加一条路径
		 */
		if(parent.nchilds<2)//如果孩子少于2个,则childs从头开始遍历,发现一个null就加入
		{
			for(int c=0;c<2;++c)
			{
				if(parent.childs.get(c)==null)//一上来都是null,之后delete时set后也是null
				{
					Topic newChild=parent.priorAsParentaddChild(nkeys, this.new_topic_id, eta, c);
					this.new_topic_id+=1;
					return CRPTreeFillNode(newChild,nkeys,eta);
				}
			}
			System.out.println("Error: root.nchilds<2 while no root.childs[i]==null");
			System.exit(0);
			return null;
		}
		else//随机2选一添加下层路径
		{
			int randomi=(int)(Math.random()*2);
			Topic parentNode=parent.childs.get(randomi);//二选一可就不新增结点了!
			return CRPTreeFillNode(parentNode,nkeys,eta);
		}
	}
	
	/*
	 * 既然调用到了hodmTreeFillNode,就意味着必然增加结点,不过,如果第一层下面已经有2个结点的话,应该报错
	 * treeFill只负责增加结点而不负责为结点安排路径,在初始时那种既增加结点又为结点安排路径的方法,其函数是单列的(与crp不同,crp是可以即增加结点又安排路径的)
	 * idx 是level1的新结点位置
	 */
	public Topic priorTreeFillNode(Topic parent,int nkeys,Vector<Double> eta,int idx)
	{
		if(parent.nchilds>=2)
		{
			System.out.println("Error: root.nchilds>=2 while still call the priorTreeFillNode");
			System.exit(0);
		}
		
		Topic newChild=parent.priorAsParentaddChild(nkeys,this.new_topic_id,eta,idx);
		this.new_topic_id+=1;
		return CRPTreeFillNode(newChild,nkeys,eta);
	}
	
	/*
	 * 输入一个node,作为要新增子结点的parent,然后为这个node新增一个子结点,并且会为新增的子节点增加子节点,更新树的结构
	 * 
	 * 这是个尾递归,如果有空,我会改成迭代
	 */
	public Topic CRPTreeFillNode(Topic parent,int nkeys,Vector<Double> eta)
	{	
		if(parent.level<this.depth-1)//如果是倒数第二层(level=depth-2)
		{
			Topic newChild=parent.CRPAsParentAddChild(nkeys, this.new_topic_id, eta);
			this.new_topic_id+=1;//总topic数又多了1
			return CRPTreeFillNode(newChild,nkeys,eta);//返回新创建的子节点的topic
		}
		else//如果是倒数第一层(level=depth-1)
		{
			return parent;//返回父亲结点
		}
	}
	
	/*
	 * 不包含这篇文档的采样
	 */
	public void treeSampleDocPathRemoveThisDoc(Document doc,Vector<Double> veceta)
	{
		//这边采用了一种特殊的手法,用dfs递归的方式计算概率,这是由概率表达式决定的//计算概率
		double logsum[]=new double[]{0.0};//归一化概率,所有结点的归一化,每个结点概率的归一化,这个是传引用的而不是传参
		boolean logsumFirstFlag[]=new boolean[]{true};//判断是不是第一个,如果是,要以赋值的形式给logsum赋值
		double path_prob[]=new double[this.depth];//临时变量,数组初始化默认为0
		
		treeRemoveDocFromPath(doc,veceta);//把这篇文档从树上移除
		
		//深度优先从根结点开始计算,儿子结点开始为new结点的概率,保存在每个结点的prob中
		
		Vector<Double> level0ProbSlot=new Vector<Double>(2);
		level0ProbSlot.setSize(2);//2个null成员
		
		calculate_node_prob_andlevel0ProbSlot(level0ProbSlot,doc.path.get(0),doc,logsum,logsumFirstFlag,path_prob,veceta);
		
		//-------------debug用
		//if(level0ProbSlot.get(0)==null&&level0ProbSlot.get(1)==null)
		//{
		//	double lsum0[]=new double[]{0.0};
		//	boolean isFirst0[]=new boolean[]{true};
		//	double lsum1[]=new double[]{0.0};
		//	boolean isFirst1[]=new boolean[]{true};
		//	this.dfs_debug_whole_prob(this.root.childs.get(0),lsum0, isFirst0);
		//	this.dfs_debug_whole_prob(this.root.childs.get(1),lsum1,isFirst1);
		//	if(lsum0[0]>lsum1[0])
		//	{
		//		System.out.println("00000000 is larger");
		//	}
		//	else
		//	{
		//		System.out.println("11111111 is larger");
		//	}
		//}
		//-------------debug用 发现概率计算没有问题
		
		
		//采样和更新
		int retLevel1NewNodeIdx[]=new int[]{-1};
		Topic new_path_end_node=treeSamplePath_Withlevel0ProbSlot(logsum[0],level0ProbSlot,retLevel1NewNodeIdx);//获得以这个结点为路径末端的结点,即这个结点的儿子为新桌子
		
		//Debug用
		//Topic pTopic=new_path_end_node;
		//while(pTopic.parent!=null)
		//{
		//	if(pTopic.level==1)
		//	{
		//		int c;
		//	
		//		for(c=0;c<pTopic.parent.nchilds;++c)
		//		{
		//			if(pTopic.parent.childs.get(c)==pTopic)
		//			{
		//				break;
		//			}
		//		}
		//		if(c==0)
		//		{
		//			System.out.println("passed 0 in level 1");
		//		}
		//		if(c==1)
		//		{
		//			System.out.println("passed 1 in level 1");
		//		}
		//	}
		//	pTopic=pTopic.parent;
		//}
		//Debug用
		
		int nkeys=new_path_end_node.posterior_log_prob_phrases.size();//为什么pathendNode的level是2啊,这说明这个pathendNode的儿子结点层要新增桌子了
		
		if(retLevel1NewNodeIdx[0]==-1)//第1层不用新增结点
		{
			//把新采样的路径到ground_level这一段补充到树中去
			new_path_end_node=CRPTreeFillNode(new_path_end_node,nkeys,veceta);
		}
		else//第1层开了条新路径
		{
			//Debug用
			//System.out.println("created a new path in level 1");
			//Debug用
			new_path_end_node=priorTreeFillNode(new_path_end_node,nkeys,veceta,retLevel1NewNodeIdx[0]);
		}
		
		//Debug用
		//Topic pTopic=new_path_end_node;
		//while(pTopic.parent!=null)
		//{
		//	if(pTopic.level==1)
		//	{
		//		int c;
		//	
		//		for(c=0;c<pTopic.parent.nchilds;++c)
		//		{
		//			if(pTopic.parent.childs.get(c)==pTopic)
		//			{
		//				break;
		//			}
		//		}
		//		if(c==0)
		//		{
		//			System.out.println("passed 0 in level 1");
		//		}
		//		if(c==1)
		//		{
		//			System.out.println("passed 1 in level 1");
		//		}
		//	}
		//	pTopic=pTopic.parent;
		//}
		//Debug用		
		
		//把文档添加到路径,即更新文档数据,文档被添加到这条路径
		treeAddDocToPath(new_path_end_node,doc,veceta);
	}

	/*
	 * 包含这篇文档的采样
	 */
	public void treeSampleDocPathWithThisDoc(Document doc)
	{
		
	}
	
	/*
	 * 如果下面有2条路径了,那么level0ProbSlot就无效,否则,有效,对应的非空成员是产生新结点的概率
	 * 用DFS的方法计算,即就算第一层有3个先验树的孩子结点,我们也是先第一列,再第二列,再第三列
	 * 只会在头结点调用一次
	 * 必须要保证第一次给logsum赋值是以赋初值的形式而不是一上来就加
	 */
	private void calculate_node_prob_andlevel0ProbSlot(Vector<Double> level0ProbSlot,Topic node,Document doc,double logsum[],boolean logsumFirstFlag[],double path_prob[],Vector<Double> veceta)
	{
		
		int currLevel=node.level;
		path_prob[currLevel]=level_log_gamma_ratio(doc,node,currLevel,veceta);//根结点也要参与算概率
		
		int nullCount=0;
		for(int c=0;c<2;++c)
		{
			if(node.childs.get(c)==null)//如果孩子结点为空,则要计算概率
			{
				++nullCount;
				
				switch(c)
				{
				case 0://左边孩子结点为空
					if(currLevel<this.depth-1)//如果是底层的level,则它的儿子层是不可能产生新结点的
					{
						int nkeys=node.posterior_log_prob_phrases.size();
						for(int l=currLevel+1;l<this.depth;++l)//第二层结点为新的,现在,在为第二层结点计算时,l=currLevel+2开始的为
						{
							double eta=veceta.get(l);
							path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nkeys);
						}
						//只需要乘一次 gamma/n+gamma;在真实的表达式里面也是这样的,在第l层产生新结点的话，只需在第l层乘以一次gamma,然后在l+1层以下都不用乘gamma而只需乘level_log_gamma_ratio_new
						path_prob[currLevel+1]+=Math.log(this.lambda);//新的,这边有所改变
					}
					double probToSet_case1=0;
					for(int l=0;l<this.depth;++l)
					{
						probToSet_case1+=path_prob[l];
					}
					
					level0ProbSlot.set(0,probToSet_case1);
					
					if(logsumFirstFlag[0]==true)
					{
						logsumFirstFlag[0]=false;
						logsum[0]=probToSet_case1;
					}
					else
					{
						logsum[0]=Sums.log_sum(logsum[0],probToSet_case1);
					}
					
					break;
					
				case 1://右边孩子结点为空
					if(currLevel<this.depth-1)//如果是底层的level,则它的儿子层是不可能产生新结点的
					{
						int nkeys=node.posterior_log_prob_phrases.size();
						for(int l=currLevel+1;l<this.depth;++l)//第二层结点为新的,现在,在为第二层结点计算时,l=currLevel+2开始的为
						{
							double eta=veceta.get(l);
							path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nkeys);
						}
						//只需要乘一次 gamma/n+gamma;在真实的表达式里面也是这样的,在第l层产生新结点的话，只需在第l层乘以一次gamma,然后在l+1层以下都不用乘gamma而只需乘level_log_gamma_ratio_new
						path_prob[currLevel+1]+=Math.log(1-this.lambda);//新的,这边有所改变
					}
					double probToSet_case2=0;
					for(int l=0;l<this.depth;++l)
					{
						probToSet_case2+=path_prob[l];
					}
					
					level0ProbSlot.set(1,probToSet_case2);
					
					if(logsumFirstFlag[0])
					{
						logsumFirstFlag[0]=false;
						logsum[0]=probToSet_case2;
					}
					else
					{
						logsum[0]=Sums.log_sum(logsum[0],probToSet_case2);
					}
					
					break;
				default:
					System.err.println("Error: In calculate_node_prob_andlevel0ProbSlot");
					System.exit(0);
				}
				
			}
			else//孩子结点存在,按照CRP方法计算概率,这时level0ProbSlot的2个成员,对应那个结点的成员不为null
			{
				calculate_node_prob_dfs(node.childs.get(c),doc,logsum,logsumFirstFlag,path_prob,veceta);//这边已经保证了node.childs.get(c)!=null
			}
		}
		if(nullCount==2)//有2个null,只可能这种情况:一篇文档,如果2篇及以上文档还有这个出现,就是程序写错了
		{
			System.err.println("Error: 有2个null,只可能这种情况:一篇文档,如果2篇及以上文档还有这个出现,就是程序写错了");
			System.exit(0);
		}	
		
	}
	
	/*
	 * debug用
	 */
	private void dfs_debug_whole_prob(Topic node,double logsum[],boolean isFirst[])
	{
		if(isFirst[0]==true)
		{
			logsum[0]=node.prob;
			isFirst[0]=false;
			for(int c=0;c<node.nchilds;++c)
			{
				dfs_debug_whole_prob(node.childs.get(c),logsum,isFirst);
			}
		}
		else
		{
			logsum[0]=Sums.log_sum(logsum[0], node.prob);			
			for(int c=0;c<node.nchilds;++c)
			{
				dfs_debug_whole_prob(node.childs.get(c),logsum,isFirst);
			}
		}
		
	}
	
	
	/*
	 * 采用深度优先的方法计算以每个结点为末端的路径的概率,中间结点的概率指从该节点开始new 结点的概率
	 * 关于rootlevel,在整个计算概率的过程中,rootlevel是保持不变的,它只是个参考系,这里我们把它去掉了,认为rootlevel恒为0
	 */
	private void calculate_node_prob_dfs(Topic node,Document doc,double logsum[],boolean logsumFirstFlag[],double path_prob[],Vector<Double> veceta)
	{
		int currLevel=node.level;
		path_prob[currLevel]=level_log_gamma_ratio(doc,node,currLevel,veceta);//是分层算p75 <***>式的,至于为什么可以这样算,笔记本后面有论述
		
		double log_total_consumer_num=0.0;
		
		if(currLevel>0)//level=0时不用计算概率,选择这个topic的概率就是1//桌子已经有人了//这边的概率在currLevel==1的时候要修改
		{
			if(currLevel==1)//如果是第二层//那得看它走的是哪一条路径
			{
				int path_idx;
				for(path_idx=0;path_idx<2;++path_idx)
				{
					if(node.parent.childs.get(path_idx)==node)
					{
						break;
					}
				}
				switch(path_idx)
				{
				case 0://左1
					path_prob[currLevel]+=Math.log(this.lambda);
					break;
				case 1://左2
					path_prob[currLevel]+=Math.log(1-this.lambda);
					break;
				default:
					System.err.println("Error:该结点不存在,却calculate_node_prob_dfs到这个结点来采样");
					System.exit(0);
				}
				
			}
			else
			{
				log_total_consumer_num=Math.log(node.parent.nTotalDocumentToTopic+this.gamma);//为什么没有-1,是因为这篇文档已经被remove掉了
				path_prob[currLevel]+=Math.log(node.nTotalDocumentToTopic)-log_total_consumer_num;
			}
		}
		
		//如果从这个结点的儿子开始为新结点,则概率是存在这个结点的prob中的
		if(currLevel<this.depth-1)//如果是底层的level,则它的儿子层是不可能产生新结点的
		{
			int nkeys=node.posterior_log_prob_phrases.size();
			for(int l=currLevel+1;l<this.depth;++l)//第二层结点为新的,现在,在为第二层结点计算时,l=currLevel+2开始的为
			{
				double eta=veceta.get(l);
				path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nkeys);
			}
			//只需要乘一次 gamma/n+gamma;在真实的表达式里面也是这样的,在第l层产生新结点的话，只需在第l层乘以一次gamma,然后在l+1层以下都不用乘gamma而只需乘level_log_gamma_ratio_new
			path_prob[currLevel+1]+=Math.log(this.gamma);//新的,这边有所改变
			path_prob[currLevel+1]-=Math.log(node.nTotalDocumentToTopic+this.gamma);
		}
		
		//会从这个node的儿子开始为新结点的概率
		
		node.prob=0;
		for(int l=0;l<this.depth;++l)
		{
			node.prob+=path_prob[l];
		}
		
		//计算归一化概率,更新
		if(currLevel==0)
		{
			//实际上这这种情况是不可能出现的
			System.err.println("Error: 根结点在calculate_node_prob_dfs中被执行");
			System.exit(0);
			logsum[0]=node.prob;//如果是根结点的话,只会计算一次log_gamma_ratio,当然也只会在这里赋值一次
		}
		else
		{
			if(logsumFirstFlag[0]==true)
			{
				logsumFirstFlag[0]=false;
				logsum[0]=node.prob;
			}
			else
			{
				logsum[0]=Sums.log_sum(logsum[0],node.prob);
			}
		}
		
		//DFS递归

		for(int c=0;c<node.nchilds;++c)
		{
			calculate_node_prob_dfs(node.childs.get(c),doc,logsum,logsumFirstFlag,path_prob,veceta);
		}
		
		//return logsum[0];//Debug用
	}
	
	/*
	 * 计算p75<***>式,level是当前式中连乘组件
	 */
	double level_log_gamma_ratio(Document doc,Topic node,int level,Vector<Double> veceta)
	{
		int nkeys=node.posterior_log_prob_phrases.size();
		int nphrases=doc.phrases.size();
		//System.out.println("The nkeys is "+nkeys);
		//System.out.println("The phrase size is "+nphrases);
		int n;
		int count[]=new int[nkeys];//字典大小
		double result;
		for(n=0;n<nphrases;++n)
		{
			count[doc.phrases.get(n)]=0;//初始化
		}
		for(n=0;n<nphrases;++n)
		{
			if(doc.levels.get(n)==level)
			{
				++count[doc.phrases.get(n)];//统计这篇文档中出现的各种phrase的数目,未出现的以0计//z=l
			}
		}
		
		double eta=veceta.get(node.level);
		result=Dirich.logGammaLanczos(node.nTotalPhrasesToTopic+nkeys*eta);
		result-=Dirich.logGammaLanczos(node.nTotalPhrasesToTopic+doc.tot_levels.get(level)+nkeys*eta);
		
		for(n=0;n<nphrases;++n)//p75 <**>式
		{
	        int wd = doc.phrases.get(n);
	        if (count[wd] > 0)//如果count==0呢？count==0的话,那就直接约掉了
	        {
	            result -= Dirich.logGammaLanczos(node.nPhrasesToTopic.get(wd)+eta);
	            result += Dirich.logGammaLanczos(node.nPhrasesToTopic.get(wd)+count[wd]+eta);
	            count[wd] = 0;//不多余,防止重复遍历,这样第二次遇到同种类的词就不会再加进去了
	        }
		}
		return result;
	}
	
	/*
	 * 计算p75<***>式,level是level+1:depth,表示从这个结点开始为new
	 */
	double level_log_gamma_ratio_new(Document doc,int level,double eta,int nkeys)
	{
		int n;
		int count[]=new int[nkeys];
		double result;
		int nphrases=doc.phrases.size();
		for(n=0;n<nphrases;++n)
		{
			count[doc.phrases.get(n)]=0;
		}
		for(n=0;n<nphrases;++n)
		{
			if(doc.levels.get(n)==level)
			{
				++count[doc.phrases.get(n)];
			}
		}
		
		result=Dirich.logGammaLanczos(nkeys*eta);
		result-=Dirich.logGammaLanczos(doc.tot_levels.get(level)+nkeys*eta);
		
		for(n=0;n<nphrases;++n)
		{
			int wd=doc.phrases.get(n);
			if(count[wd]>0)
			{
				result-=Dirich.logGammaLanczos(eta);
				result+=Dirich.logGammaLanczos(count[wd]+eta);
				count[wd]=0;
			}
		}
		
		return result;
	}
	
	/*
	 * 从这棵树上移除这篇文档占有的路径
	 */
	private void treeRemoveDocFromPath(Document doc,Vector<Double> veceta)
	{
		treeUpdateForDoc(doc,-1,veceta);
		treeUpdateForTreeBranch(doc.path.get(this.depth-1));//倒数第二层,不管有没有结点,
	}
	
	/*
	 * 移除一条路径后,每篇文档关联的路径会发生变化,然后相关topic关联到的phrase数会发生变化
	 * 之所以先考虑文档相关的数据的更新,是因为如果先更新路径,那么会产生空topic结点,在treeUpdateForDoc中会报错
	 * 和文档相关的topic的phrase数-1,对每个doc涉及到的phrase
	 */
	private void treeUpdateForDoc(Document doc,int update,Vector<Double> veceta)
	{
		int depth=this.depth;
		int nphrases=doc.phrases.size();
		
		int n;
		
	    for (n=0;n<nphrases;++n)
	    {
	        int level =doc.levels.get(n);
	        if (level > 0)//为什么第一层不更新计数?因为它采样是从第二层开始采的,第一层的topic概率永远是1
	        {
	            doc.path.get(level).topic_update_nPhrasesToTopic(doc.phrases.get(n),update,level,veceta);
	        }
	    }
	    for (n=1;n<depth;++n)//为什么第一层不更新计数?你后面采样时用不到第一层的概率,第一层的概率根本就不存储,第一层的prob存的是下一层为新结点的概率
	    {
	    	doc.path.get(n).topic_update_nDocumentsToTopic(update);
	    }
	}
	
	/*
	 * 移除一条路径后,更新tree相关数据(CRPTreeNode中的数据),自底向上
	 */
	private void treeUpdateForTreeBranch(Topic node)
	{
		Topic parent=node.parent;
		if(node.nTotalDocumentToTopic==0)
		{
			//System.out.println("需要移除topic结点");//debug后发现没有topic结点被移除
			treeDeleteNode(node);//这意味着一篇文档不允许采样,因为只有一篇文档时,根节点将会被删除,无法存储概率
			if(parent!=null)//加这一句,是为了照顾root结点？root结点nTotalDocumentToTopic不可能=0啊。答曰：是多余的,因为根节点node.nTotalDocumentToTopic==0永远不会发生
			//原本的初衷是为了照顾一篇文档的情况,事实上一篇文档根本采不了样,因为这样一来根结点就被remove掉了,谁来存储第二层开始增加新结点的概率？所以后来程序修改了,GibbsInit中有了if(i>0),即不允许一篇文档就开始采样
			{
				treeUpdateForTreeBranch(parent);
			}
			else
			{
				System.out.println("Error: 文档数==1或程序有问题,在文档数>1的情况下出现了root.nTotalDocumentToTopic==0的情况");
				System.exit(0);
			}
		}
		
	}
	
	/*
	 * 树上删掉一个结点
	 * 这个要看你删掉的是哪个结点,如果是第二层的结点,(第一层的结点永远不会删掉),那更新的方式可就不一样了
	 */
	private void treeDeleteNode(Topic node)
	{
		//所有孩子结点删除
		for(int c=0;c<node.nchilds;++c)
		{
			treeDeleteNode(node.childs.get(c));
		}
		if(node.level>1)//是CRP结点
		{
			//更新node本身parent结点中的数据
			int nc=node.parent.nchilds;
			for(int c=0;c<nc;++c)
			{
				if(node.parent.childs.get(c)==node)//引用的相等//注意,这个有个固有性质,就是只会相等一次,其后便不再会相等,因为topic的独一无二性
				{
					Topic nc_1node=node.parent.childs.get(nc-1);
					node.parent.childs.set(c,nc_1node);
					node.parent.childs.remove(nc-1);//在C中不用显式删除,因为长度是固定的,但是在java中数组长度可变,最好显式删除,因为在java种,我们在treeFill中新增结点使用add从childs尾部加入的.
					--node.parent.nchilds;
					--nc;//这个不会发生topic跳过没有遍历到的问题,因为"=="只会发生一次,假如发生了,则目的已达到,可以break了,也可以nc--,假如没发生,那么也不会发生nc--的行为
				}
			}
			//有2种可能,一种是位于nc-1的结点确实nPhrasesToTopic都为null,还有一种可能就是node还指向位置为c的结点(新的),导致nPhrasesToTopic=null
			
			//释放内存
			node.nPhrasesToTopic.clear();
			node.nPhrasesToTopic=null;
			node.posterior_log_prob_phrases.clear();
			node.posterior_log_prob_phrases=null;
			node.childs.clear();
			node.childs=null;
			node=null;//注意啊,这个引用在docpath中还是存在的啊,只不过childs中加了clear,引用在childs中不存在了,其实这里node=null没有什么实质性意义
		}
		else//是PriorTree结点下一层的结点//注意,level
		{
			if(node.level==0)//不应该是头结点,头结点不应该删除,只有2种情况会执行这句,一是只有一篇文档,二是程序有问题
			{
				System.err.println("Error:文档数==1或程序有问题，");
				System.exit(0);
			}
			//更新node本身parent结点中的数据
			
			//能执行到这里说明node.level==1
			int nc=node.parent.nchilds;
			for(int c=0;c<nc;++c)
			{
				if(node.parent.childs.get(c)==node)//引用的相等//注意,这个有个固有性质,就是只会相等一次,其后便不再会相等,因为topic的独一无二性
				{
					node.parent.childs.set(c,null);//设为null,意味着这个结点已经被释放了,虽然node.parent.childs.size()任然==2
					--node.parent.nchilds;
				}
			}
			node.nPhrasesToTopic.clear();
			node.nPhrasesToTopic=null;
			node.posterior_log_prob_phrases.clear();
			node.posterior_log_prob_phrases=null;
			node.childs.clear();
			node.childs=null;
			node=null;
		}
	}
	
	/*
	 * 带着level0ProbSlot来采样，根节点用level0ProbSlot
	 */
	private Topic treeSamplePath_Withlevel0ProbSlot(double logsum,Vector<Double> level0ProbSlot,int retLevel1NewNodeIdx[])
	{
		double runningsum[]=new double[]{0.0};//只有一层,没关系,要是有2层先验,那样每一层都要有一个函数
		double rand=Math.random();
		int nullCount=0;
		for(int c=0;c<2;++c)//一个DFS的过程
		{
			if(level0ProbSlot.get(c)!=null)
			{
				++nullCount;
				runningsum[0]+=Math.exp(level0ProbSlot.get(c)-logsum);
				if(runningsum[0]>=rand)
				{
					retLevel1NewNodeIdx[0]=c;
					return this.root;//this.root后面要加新结点啦,但是如何提示加的是哪一家呢?一种方法是借prob一用,不过这样不太好,这里借retLevel1NewNodeIdx传一下
				}
				//else//有一种情况,就是第1层(从0起记)左边没结点,但是右边有结点,左边不取新结点,只能从右边开始算了,不过,右边是留给for循环的
				//{
				//	return treeSampleDFS(logsum[0],);
				//}
			}
			else
			{
				Topic varnode=treeSampleDFS(rand,this.root.childs.get(c),runningsum,logsum);
				if(varnode!=null)
				{
					return varnode;
				}
			}
		}
		if(nullCount==2)//有2个null,只可能这种情况:一篇文档,如果2篇及以上文档还有这个出现,就是程序写错了
		{
			System.err.println("Error: 有2个null,只可能这种情况:一篇文档,如果2篇及以上文档还有这个出现,就是程序写错了");
			System.exit(0);
		}
		System.err.println("Error: 没有采样到结点,概率计算有问题");
		return null;
	}
	
	///*
	// * 已知每个结点作为末端路径的概率分布,sample一个结点
	// */
	//private Topic treeSamplePath(double logsum)
	//{
	//	double runningsum[]=new double[]{0.0};//按照dfs,每经过一个结点就加上结点的概率,其实结点可以看成平铺的,只是按照DFS去遍历而已
	//	double rand=Math.random();
	//	return treeSampleDFS(rand,this.root,runningsum,logsum);
	//}
	
	/*
	 * 按照DFS的方法返回采样到的结点,这是一个递归
	 */
	private Topic treeSampleDFS(double rand,Topic node,double runningsum[],double logsum)
	{
		runningsum[0]=runningsum[0]+Math.exp(node.prob-logsum);
		
		if(runningsum[0]>=rand)
		{
			return node;
		}
		else
		{
			for(int i=0;i<node.nchilds;++i)
			{
				Topic varnode=treeSampleDFS(rand,node.childs.get(i),runningsum,logsum);
				if(varnode!=null)//为什么不直接return?因为如果直接return,那么在第一个路径到底时就会终止for循环
				{
					return varnode;
				}
			}
		}
		return null;//如果路径到达了底端,就会用这个返回
	}
	
	/*
	 * 把文档添加到node为尾的路径,更新文档数据
	 */
	private void treeAddDocToPath(Topic node,Document doc,Vector<Double> veceta)
	{
		int depth=this.depth;
		int l=depth-1;//永远有depth-1的结点储备,每条路表面上深度不一,实际上存储时深度都为depth-1
		do
		{
			doc.path.set(l,node);
			node=node.parent;
			--l;
		}while(l>=0);
		
		//文档单词关联到的topic,更新
		treeUpdateForDoc(doc,1,veceta);
	}
	
	/*
	 * 把树输出到文件
	 */
	public void writeTree(BufferedWriter treebw) throws IOException
	{
		String alineToWrite=String.format("%-6s %-6s %-6s %-6s  %-9s %-6s\n","ID", "PARENT", "NDOCS", "NWORDS", "SCALE", "WD_CNT");
		treebw.write(alineToWrite);
		writeTreeDFS(this.root,treebw);
	}
	
	
	/*
	 * 用深度优先的方法输出一棵树
	 */
	private void writeTreeDFS(Topic node,BufferedWriter treebw) throws IOException
	{
		String alineToWrite=String.format("%-6d",node.id);
		treebw.write(alineToWrite);

		if(node.parent!=null)
		{
			alineToWrite=String.format(" %-6d", node.parent.id);
			treebw.write(alineToWrite);
		}
		else
		{
			alineToWrite=String.format(" %-6d", -1);
			treebw.write(alineToWrite);
		}

		alineToWrite=String.format(" %06.0f",node.nTotalDocumentToTopic);
		treebw.write(alineToWrite);
		alineToWrite=String.format(" %06.0f",node.nTotalPhrasesToTopic);
		treebw.write(alineToWrite);
		alineToWrite=String.format(" %06.3e",this.gamma);
		treebw.write(alineToWrite);

		for(int i=0;i<node.nPhrasesToTopic.size();++i)
		{
			alineToWrite=String.format(" %6.0f",node.nPhrasesToTopic.get(i));
			treebw.write(alineToWrite);
		}
		treebw.write("\n");

		for(int i=0;i<node.nchilds;++i)
		{
			writeTreeDFS(node.childs.get(i),treebw);
		}
		
	}
	
	/*
	 * 返回这棵树中的主题数量
	 */
	public int ntopicsInTree()
	{
		return ntopicsInTreeDFS(this.root);
	}
	
	/*
	 * 用dfs遍历获得树中的主题数量
	 */
	private int ntopicsInTreeDFS(Topic parent)
	{
		int ntopicsBelow=0;
		int c;
		for(c=0;c<parent.nchilds;++c)
		{
			ntopicsBelow+=ntopicsInTreeDFS(parent.childs.get(c));
		}
		return parent.nchilds+ntopicsBelow;
	}
	
	/*
	 * writeTreeLevels
	 */
	public void writeTreeLevels(BufferedWriter treeLogBw)
	{
		try {
			writeTreeLevelDFS(this.root,treeLogBw);
			treeLogBw.write("\n");
			treeLogBw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*
	 * writeTreeLevels in dfs
	 */
	private void writeTreeLevelDFS(Topic parent,BufferedWriter treeLogBw) throws IOException
	{
		String alineToWrite;
		if(parent.parent==null)
		{
			alineToWrite=String.format("%d", parent.level);
			treeLogBw.write(alineToWrite);
		}
		else
		{
			alineToWrite=String.format(" %d", parent.level);
			treeLogBw.write(alineToWrite);
		}
		
	    for (int c = 0; c <parent.nchilds; ++c)
	    {
	    	writeTreeLevelDFS(parent.childs.get(c), treeLogBw);
	    }
	}
	
	
}
