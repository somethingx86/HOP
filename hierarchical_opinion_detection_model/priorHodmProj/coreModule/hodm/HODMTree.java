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
 * CRP��
 * @author ZhuLixing
 *
 */
public class HODMTree{
	public Topic root;//blei�е�root��ÿ��treenodeָ����Լ���Ӧ��topic
	public int depth;//�����
	public double gamma;//NCRP�еĳ���gamma,�����ƹ�,��ÿ��CRPTreeNode,ÿ��topic�е�gamma����һ��
	public double lambda=0.5;//�ڶ���ĳ���,�Ǹ�����,��Ϊ��0.4�ĸ�����֧�ֵ�
	public int new_topic_id;//topic������,ͬʱҲ����һ��topic�ı��
	//public int topics
	public HODMTree(int depth,int nterms,Vector<Double> eta,double gamma)
	{
		this.new_topic_id=0;
		root=new Topic(nterms,0,null,this.new_topic_id,eta);//�����µ����ڵ�,������һ���µ�topic
		this.new_topic_id+=1;
		this.depth=depth;
		this.gamma=gamma;
	}
	
	/*
	 * �����ӽ���ְ���·��,���������,���˸���һ�������,���˸�������
	 * ����initGibbsState�б�����һ��,����Ϊ��ʼ��ʹ��,����Ҳ�Ǳ�Ҫ��,��Ϊ��ϵ���²����topic,ǣһ������ȫ��
	 */
	public Topic priorTreeFillNodeInInitState(Topic parent,int nterms,Vector<Double> eta)
	{
		/*
		 * �ڶ�������1���,����ڶ�����2�������������,���������Ժ��һ��·��
		 * ��������Ļ���Ҫ�����ĵ�����������²�����
		 */
		if(parent.nchilds<2)//�����������2��,��childs��ͷ��ʼ����,����һ��null�ͼ���
		{
			for(int c=0;c<2;++c)
			{
				if(parent.childs.get(c)==null)//һ��������null,֮��deleteʱset��Ҳ��null
				{
					Topic newChild=parent.priorAsParentaddChild(nterms, this.new_topic_id, eta, c);
					this.new_topic_id+=1;
					return CRPTreeFillNode(newChild,nterms,eta);
				}
			}
			System.out.println("Error: root.nchilds<2 while no root.childs[i]==null");
			System.exit(0);
			return null;
		}
		else//���2ѡһ����²�·��
		{
			int randomi=(int)(Math.random()*2);
			Topic parentNode=parent.childs.get(randomi);//��ѡһ�ɾͲ����������!
			return CRPTreeFillNode(parentNode,nterms,eta);
		}
	}
	
	/*
	 * �����ӽ���ְ���·��,���������,���˸���һ�������,���˸�������
	 * ����initGibbsState�б�����һ��,����Ϊ��ʼ��ʹ��,����Ҳ�Ǳ�Ҫ��,��Ϊ��ϵ���²����topic,ǣһ������ȫ��
	 */
	public Topic priorTreeFillNodeInInitState_left(Topic parent,int nterms,Vector<Double> eta)
	{
		/*
		 * �ڶ�������1���,����ڶ�����2�������������,���������Ժ��һ��·��
		 * ��������Ļ���Ҫ�����ĵ�����������²�����
		 */
		int c=0;
		if(parent.childs.get(c)==null)
		{
			Topic newChild = parent.priorAsParentaddChild(nterms, this.new_topic_id, eta, c);
			this.new_topic_id+=1;
			return CRPTreeFillNode(newChild,nterms,eta);
		}
		else
		{
			Topic parentNode=parent.childs.get(c);//��ѡһ�ɾͲ����������!
			return CRPTreeFillNode(parentNode,nterms,eta);
		}
	}
	
	/*
	 * �����ӽ���ְ���·��,���������,���˸���һ�������,���˸�������
	 * ����initGibbsState�б�����һ��,����Ϊ��ʼ��ʹ��,����Ҳ�Ǳ�Ҫ��,��Ϊ��ϵ���²����topic,ǣһ������ȫ��
	 */
	public Topic priorTreeFillNodeInInitState_right(Topic parent,int nterms,Vector<Double> eta)
	{
		/*
		 * �ڶ�������1���,����ڶ�����2�������������,���������Ժ��һ��·��
		 * ��������Ļ���Ҫ�����ĵ�����������²�����
		 */
		int c=1;
		if(parent.childs.get(c)==null)//һ��������null,֮��deleteʱset��Ҳ��null
		{
			Topic newChild=parent.priorAsParentaddChild(nterms, this.new_topic_id, eta, c);
			this.new_topic_id+=1;
			return CRPTreeFillNode(newChild,nterms,eta);
		}
		else//���2ѡһ����²�·��
		{
			Topic parentNode=parent.childs.get(c);//��ѡһ�ɾͲ����������!
			return CRPTreeFillNode(parentNode,nterms,eta);
		}
	}
	
	/*
	 * ��Ȼ���õ���hodmTreeFillNode,����ζ�ű�Ȼ���ӽ��,����,�����һ�������Ѿ���2�����Ļ�,Ӧ�ñ���
	 * treeFillֻ�������ӽ���������Ϊ��㰲��·��,�ڳ�ʼʱ���ּ����ӽ����Ϊ��㰲��·���ķ���,�亯���ǵ��е�(��crp��ͬ,crp�ǿ��Լ����ӽ���ְ���·����)
	 * idx ��level1���½��λ��
	 * 
	 * ��priorTreeFillNodeInInitState��ƽ����ϵ
	 * ��priorTreeFillNodeInInitState��֮ͬ�������������Ƿ���Ϊnull������,���Ǿ��㷢��nullҲҪ����
	 * 
	 * ��������������parent.childs��������������null,����ֻ��һƪ�ĵ�
	 * ����ֻ��һƪ�ĵ�,treeSamplePath_Withlevel0ProbSlotҲ�ᱣ֤����root,����Ҫ������һ���������
	 * 
	 * ���priorTreeFillNodeInInitState�ǿ��Ա�������������,���ǿ��ǲ�����ʱ��ɱ�,init�в����ÿյ����β�,���ԲŶ�������
	 */
	public Topic priorTreeFillNode(Topic parent,int nterms,Vector<Double> eta,int idx)
	{
		if(parent.nchilds>=2)
		{
			System.out.println("Error: root.nchilds>=2 while still call the priorTreeFillNode");
			System.exit(0);
		}
		
		Topic newChild=parent.priorAsParentaddChild(nterms,this.new_topic_id,eta,idx);
		this.new_topic_id+=1;
		return CRPTreeFillNode(newChild,nterms,eta);
	}
	
	/*
	 * ����һ��node,��ΪҪ�����ӽ���parent,Ȼ��Ϊ���node����һ���ӽ��,���һ�Ϊ�������ӽڵ������ӽڵ�,�������Ľṹ
	 * 
	 * ���Ǹ�β�ݹ�,����п�,�һ�ĳɵ���
	 */
	public Topic CRPTreeFillNode(Topic parent,int nterms,Vector<Double> eta)
	{	
		if(parent.level<this.depth-1)//����ǵ����ڶ���(level=depth-2)
		{
			Topic newChild=parent.CRPAsParentAddChild(nterms, this.new_topic_id, eta);
			this.new_topic_id+=1;//��topic���ֶ���1
			return CRPTreeFillNode(newChild,nterms,eta);//�����´������ӽڵ��topic
		}
		else//����ǵ�����һ��(level=depth-1)
		{
			return parent;//���ظ��׽��
		}
	}
	
	/*
	 * ��������ƪ�ĵ��Ĳ���
	 */
	public void treeSampleDocPathRemoveThisDoc(Document doc,Vector<Double> veceta)
	{
		//��߲�����һ��������ַ�,��dfs�ݹ�ķ�ʽ�������,�����ɸ��ʱ��ʽ������//�������
		double logsum[]=new double[]{0.0};//��һ������,���н��Ĺ�һ��,ÿ�������ʵĹ�һ��,����Ǵ����õĶ����Ǵ���
		boolean logsumFirstFlag[]=new boolean[]{true};//�ж��ǲ��ǵ�һ��,�����,Ҫ�Ը�ֵ����ʽ��logsum��ֵ
		double path_prob[]=new double[this.depth];//��ʱ����,�����ʼ��Ĭ��Ϊ0
		int flag_doc_stance=doc.stance_flag;//�ĵ��Ƿ��Ѿ�����ע
		
		treeRemoveDocFromPath(doc,veceta);//����ƪ�ĵ��������Ƴ�
		
		//������ȴӸ���㿪ʼ����,���ӽ�㿪ʼΪnew���ĸ���,������ÿ������prob��
		//��һ����Ҫ��������,��Ϊ��һ�����������Ϊnull�������ȴ���ұ�,��˵�һ����Ҫһ��ʵ����һ������,ʵ����ʾ��һ���������ĸ���,������ʾ��һ���Ǹ������Ҫ�������.��Ӧ��,��һ���ArrayList ChildsҲӦ���滻Ϊһ����άVector Childs
		//��߲��õķ����ǵ�һ���ArrayList ChildsҲʹ��,���Ǹ��ʲ������Ҷ��ӽ���,��������˸�����,��ʵ��ȫ�Ƕ��һ��,�п��һ�ĵ�,��
		//������ķ�����:��һ���õ�һ��Ľ��,��������<Vector> Childs ��¼���ӽ��, double prob ��¼�˽��new���ӵĸ���, int child_idx ��¼new�Ķ������ı� -1���ʾ���������
		//��߾���<ArrayList> Childs ��¼���ӽ��,double prob��int child_idx���� ת��ArrayList<Double> levelProbSlot,�ĸ��и��ʲ�Ϊnull��ʾ�ĸ�Ҫ�������  ȫΪnull��ʾ�����������
		//<ArrayList> ChildsҲ��������set��,������Childs(0)==null �������,set(1,new Topic())
		
		//�ܽ�һ��,���Ȳ���priorTreeFillNodeǰ��ע���������ķ���,priorTreeFillNodeInInitState��priorTreeFillNode�ֿ�,��ʡ��ʼ���ĳ�������ʱ��
		//����ǲ������ǰ�������ķ���,��<ArrayList> Childs ��¼���ӽ��,double prob��int child_idx���� ת��ArrayList<Double> levelProbSlot,�ĸ��и��ʲ�Ϊnull��ʾ�ĸ�Ҫ�������  ȫΪnull��ʾ�����������
		Vector<Double> level0ProbSlot=new Vector<Double>(2);//�ĸ���Ϊnull�ĸ�Ҫ�������
		level0ProbSlot.setSize(2);//2��null��Ա
		
		
		
		//-------------debug��
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
		//-------------debug�� ���ָ��ʼ���û������
		
		if(flag_doc_stance == 0)//�ĵ�û��ע
		{
			calculate_node_prob_andlevel0ProbSlot(level0ProbSlot,doc.path.get(0),doc,logsum,logsumFirstFlag,path_prob,veceta);
			
			//�����͸���
			int retLevel1NewNodeIdx[]=new int[]{-1};
			Topic new_path_end_node=treeSamplePath_Withlevel0ProbSlot(logsum[0],level0ProbSlot,retLevel1NewNodeIdx);//�����������Ϊ·��ĩ�˵Ľ��,��������Ķ���Ϊ������
			
			//Debug��
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
			//Debug��
			
			int nterms=new_path_end_node.posterior_log_prob_words.size();//ΪʲôpathendNode��level��2��,��˵�����pathendNode�Ķ��ӽ���Ҫ����������
			
			if(retLevel1NewNodeIdx[0]==-1)//��1�㲻���������
			{
				//���²�����·����ground_level��һ�β��䵽����ȥ
				new_path_end_node=CRPTreeFillNode(new_path_end_node,nterms,veceta);
			}
			else//��1�㿪������·��
			{
				//Debug��
				//System.out.println("created a new path in level 1");
				//Debug��
				new_path_end_node=priorTreeFillNode(new_path_end_node,nterms,veceta,retLevel1NewNodeIdx[0]);
			}
			
			//Debug��
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
			//Debug��		
			
			//���ĵ���ӵ�·��,�������ĵ�����,�ĵ�����ӵ�����·��
			treeAddDocToPath(new_path_end_node,doc,veceta);
		}
		else //�ĵ��������,����������ʵֻ����ߵĻ���,������ұ���ʵֻ���ұߵĻ���
		{
			switch(flag_doc_stance)
			{
			case -1:
				calculate_node_prob_andlevel0ProbSlot_flag_left(level0ProbSlot,doc.path.get(0),doc,logsum,logsumFirstFlag,path_prob,veceta);
				
				//�����͸���
				int retLevel1NewNodeIdxCase0[]=new int[]{-1};
				Topic new_path_end_node_case0=treeSamplePath_Withlevel0ProbSlot_flag_left(logsum[0],level0ProbSlot,retLevel1NewNodeIdxCase0);//�����������Ϊ·��ĩ�˵Ľ��,��������Ķ���Ϊ������
				
				int nterms_case0=new_path_end_node_case0.posterior_log_prob_words.size();//ΪʲôpathendNode��level��2��,��˵�����pathendNode�Ķ��ӽ���Ҫ����������
				
				if(retLevel1NewNodeIdxCase0[0]==-1)//��1�㲻���������
				{
					//���²�����·����ground_level��һ�β��䵽����ȥ
					new_path_end_node_case0=CRPTreeFillNode(new_path_end_node_case0,nterms_case0,veceta);
				}
				else//��1�㿪������·��
				{
					new_path_end_node_case0=priorTreeFillNode(new_path_end_node_case0,nterms_case0,veceta,retLevel1NewNodeIdxCase0[0]);
				}
				//���ĵ���ӵ�·��,�������ĵ�����,�ĵ�����ӵ�����·��
				treeAddDocToPath(new_path_end_node_case0,doc,veceta);
				break;
			case 1:
				calculate_node_prob_andlevel0ProbSlot_flag_right(level0ProbSlot,doc.path.get(0),doc,logsum,logsumFirstFlag,path_prob,veceta);
				
				//�����͸���
				int retLevel1NewNodeIdxCase1[]=new int[]{-1};
				Topic new_path_end_node_case1=treeSamplePath_Withlevel0ProbSlot_flag_right(logsum[0],level0ProbSlot,retLevel1NewNodeIdxCase1);//�����������Ϊ·��ĩ�˵Ľ��,��������Ķ���Ϊ������
				
				int nterms_case1=new_path_end_node_case1.posterior_log_prob_words.size();//ΪʲôpathendNode��level��2��,��˵�����pathendNode�Ķ��ӽ���Ҫ����������
				
				if(retLevel1NewNodeIdxCase1[0]==-1)//��1�㲻���������
				{
					//���²�����·����ground_level��һ�β��䵽����ȥ
					new_path_end_node_case1=CRPTreeFillNode(new_path_end_node_case1,nterms_case1,veceta);
				}
				else//��1�㿪������·��
				{
					new_path_end_node_case1=priorTreeFillNode(new_path_end_node_case1,nterms_case1,veceta,retLevel1NewNodeIdxCase1[0]);
				}
				//���ĵ���ӵ�·��,�������ĵ�����,�ĵ�����ӵ�����·��
				treeAddDocToPath(new_path_end_node_case1,doc,veceta);
				break;
			default:
				System.err.println("Error: abnormal flag_doc_stance value!");
				break;
			}
		}
	}

	/*
	 * ������ƪ�ĵ��Ĳ���
	 */
	public void treeSampleDocPathWithThisDoc(Document doc)
	{
		
	}
	
	/*
	 * ���������2��·����,��ôlevel0ProbSlot����Ч,����,��Ч,��Ӧ�ķǿճ�Ա�ǲ����½��ĸ���
	 * ��DFS�ķ�������,�������һ����3���������ĺ��ӽ��,����Ҳ���ȵ�һ��,�ٵڶ���,�ٵ�����
	 * ֻ����ͷ������һ��
	 * ����Ҫ��֤��һ�θ�logsum��ֵ���Ը���ֵ����ʽ������һ�����ͼ�
	 */
	private void calculate_node_prob_andlevel0ProbSlot(Vector<Double> level0ProbSlot,Topic node,Document doc,double logsum[],boolean logsumFirstFlag[],double path_prob[],Vector<Double> veceta)
	{
		
		int currLevel=node.level;
		path_prob[currLevel]=level_log_gamma_ratio(doc,node,currLevel,veceta);//�����ҲҪ���������
		
		int nullCount=0;
		for(int c=0;c<2;++c)
		{
			if(node.childs.get(c)==null)//������ӽ��Ϊ��,��Ҫ�������
			{
				++nullCount;
				
				switch(c)
				{
				case 0://��ߺ��ӽ��Ϊ��
					if(currLevel<this.depth-1)//����ǵײ��level,�����Ķ��Ӳ��ǲ����ܲ����½���
					{
						int nterms=node.posterior_log_prob_words.size();
						for(int l=currLevel+1;l<this.depth;++l)//�ڶ�����Ϊ�µ�,����,��Ϊ�ڶ��������ʱ,l=currLevel+2��ʼ��Ϊ
						{
							double eta=veceta.get(l);
							path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nterms);
						}
						//ֻ��Ҫ��һ�� gamma/n+gamma;����ʵ�ı��ʽ����Ҳ��������,�ڵ�l������½��Ļ���ֻ���ڵ�l�����һ��gamma,Ȼ����l+1�����¶����ó�gamma��ֻ���level_log_gamma_ratio_new
						path_prob[currLevel+1]+=Math.log(this.lambda);//�µ�,��������ı�
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
					
				case 1://�ұߺ��ӽ��Ϊ��
					if(currLevel<this.depth-1)//����ǵײ��level,�����Ķ��Ӳ��ǲ����ܲ����½���
					{
						int nterms=node.posterior_log_prob_words.size();
						for(int l=currLevel+1;l<this.depth;++l)//�ڶ�����Ϊ�µ�,����,��Ϊ�ڶ��������ʱ,l=currLevel+2��ʼ��Ϊ
						{
							double eta=veceta.get(l);
							path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nterms);
						}
						//ֻ��Ҫ��һ�� gamma/n+gamma;����ʵ�ı��ʽ����Ҳ��������,�ڵ�l������½��Ļ���ֻ���ڵ�l�����һ��gamma,Ȼ����l+1�����¶����ó�gamma��ֻ���level_log_gamma_ratio_new
						path_prob[currLevel+1]+=Math.log(1-this.lambda);//�µ�,��������ı�
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
					System.err.println("Error: In calculate_node_prob_andlevel0ProbSlot, childnum>2, it is impossible for the root node");
					System.exit(0);
				}
				
			}
			else//���ӽ�����,����CRP�����������,��ʱlevel0ProbSlot��2����Ա,��Ӧ�Ǹ����ĳ�Ա��Ϊnull
			{
				calculate_node_prob_dfs(node.childs.get(c),doc,logsum,logsumFirstFlag,path_prob,veceta);//����Ѿ���֤��node.childs.get(c)!=null
			}
		}
		if(nullCount==2)//��2��null,ֻ�����������:һƪ�ĵ�,���2ƪ�������ĵ������������,���ǳ���д����
		{
			System.err.println("Error: ��2��null,ֻ�����������:һƪ�ĵ�,���2ƪ�������ĵ������������,���ǳ���д����");
			System.exit(0);
		}	
		
	}
	
	/*
	 * ���doc��stance flag !=0 ������,���
	 * ���������2��·����,��ôlevel0ProbSlot����Ч,����,��Ч,��Ӧ�ķǿճ�Ա�ǲ����½��ĸ���
	 * ��DFS�ķ�������,�������һ����3���������ĺ��ӽ��,����Ҳ���ȵ�һ��,�ٵڶ���,�ٵ�����
	 * ֻ����ͷ������һ��
	 * ����Ҫ��֤��һ�θ�logsum��ֵ���Ը���ֵ����ʽ������һ�����ͼ�
	 */
	private void calculate_node_prob_andlevel0ProbSlot_flag_left(Vector<Double> level0ProbSlot,Topic node,Document doc,double logsum[],boolean logsumFirstFlag[],double path_prob[],Vector<Double> veceta)
	{
		
		int currLevel=node.level;
		path_prob[currLevel]=level_log_gamma_ratio(doc,node,currLevel,veceta);//�����ҲҪ���������
		
		if(node.childs.get(0)==null)//�����ߺ��ӽ��Ϊ��,��ֻ�����Ҫ�������,�ұ߲���,��ʵ��߾��㲻�������Ҳ����
		{
			if(currLevel<this.depth-1)//����ǵײ��level,�����Ķ��Ӳ��ǲ����ܲ����½���
			{
				int nterms=node.posterior_log_prob_words.size();
				for(int l=currLevel+1;l<this.depth;++l)//�ڶ�����Ϊ�µ�,����,��Ϊ�ڶ��������ʱ,l=currLevel+2��ʼ��Ϊ
				{
					double eta=veceta.get(l);
					path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nterms);
				}
				//ֻ��Ҫ��һ�� gamma/n+gamma;����ʵ�ı��ʽ����Ҳ��������,�ڵ�l������½��Ļ���ֻ���ڵ�l�����һ��gamma,Ȼ����l+1�����¶����ó�gamma��ֻ���level_log_gamma_ratio_new
				path_prob[currLevel+1]+=Math.log(this.lambda);//�µ�,��������ı�
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
		
		}
		else//���ӽ�����,����CRP�����������,��ʱlevel0ProbSlot��2����Ա,��Ӧ�Ǹ����ĳ�Ա��Ϊnull
		{
			calculate_node_prob_dfs(node.childs.get(0),doc,logsum,logsumFirstFlag,path_prob,veceta);//����Ѿ���֤��node.childs.get(c)!=null
		}
	}

	/*
	 * ���doc��stance flag !=0 ������,�ұ�
	 * ���������2��·����,��ôlevel0ProbSlot����Ч,����,��Ч,��Ӧ�ķǿճ�Ա�ǲ����½��ĸ���
	 * ��DFS�ķ�������,�������һ����3���������ĺ��ӽ��,����Ҳ���ȵ�һ��,�ٵڶ���,�ٵ�����
	 * ֻ����ͷ������һ��
	 * ����Ҫ��֤��һ�θ�logsum��ֵ���Ը���ֵ����ʽ������һ�����ͼ�
	 */
	private void calculate_node_prob_andlevel0ProbSlot_flag_right(Vector<Double> level0ProbSlot,Topic node,Document doc,double logsum[],boolean logsumFirstFlag[],double path_prob[],Vector<Double> veceta)
	{
		
		int currLevel=node.level;
		path_prob[currLevel]=level_log_gamma_ratio(doc,node,currLevel,veceta);//�����ҲҪ���������
		
		if(node.childs.get(1)==null)//����ұߺ��ӽ��Ϊ��,��ֻ���ұ�Ҫ�������,��߲���,��ʵ�ұ߾��㲻�������Ҳ����
		{
			if(currLevel<this.depth-1)//����ǵײ��level,�����Ķ��Ӳ��ǲ����ܲ����½���
			{
				int nterms=node.posterior_log_prob_words.size();
				for(int l=currLevel+1;l<this.depth;++l)//�ڶ�����Ϊ�µ�,����,��Ϊ�ڶ��������ʱ,l=currLevel+2��ʼ��Ϊ
				{
					double eta=veceta.get(l);
					path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nterms);
				}
				//ֻ��Ҫ��һ�� gamma/n+gamma;����ʵ�ı��ʽ����Ҳ��������,�ڵ�l������½��Ļ���ֻ���ڵ�l�����һ��gamma,Ȼ����l+1�����¶����ó�gamma��ֻ���level_log_gamma_ratio_new
				path_prob[currLevel+1]+=Math.log(this.lambda);//�µ�,��������ı�
			}
			double probToSet_case2=0;
			for(int l=0;l<this.depth;++l)
			{
				probToSet_case2+=path_prob[l];
			}
			
			level0ProbSlot.set(1,probToSet_case2);
			
			if(logsumFirstFlag[0]==true)
			{
				logsumFirstFlag[0]=false;
				logsum[0]=probToSet_case2;
			}
			else
			{
				logsum[0]=Sums.log_sum(logsum[0],probToSet_case2);
			}
		
		}
		else//���ӽ�����,����CRP�����������,��ʱlevel0ProbSlot��2����Ա,��Ӧ�Ǹ����ĳ�Ա��Ϊnull
		{
			calculate_node_prob_dfs(node.childs.get(1),doc,logsum,logsumFirstFlag,path_prob,veceta);//����Ѿ���֤��node.childs.get(c)!=null
		}
		
	}
	
	/*
	 * debug��
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
	 * ����������ȵķ���������ÿ�����Ϊĩ�˵�·���ĸ���,�м���ĸ���ָ�Ӹýڵ㿪ʼnew ���ĸ���
	 * ��ʵ���Ҳ��ÿ��·���ĸ���(һ�� new �˾�һ· new ���׶���),������Ļ�,ֻҪ������ߵĸ������������
	 * ����rootlevel,������������ʵĹ�����,rootlevel�Ǳ��ֲ����,��ֻ�Ǹ��ο�ϵ,�������ǰ���ȥ����,��Ϊrootlevel��Ϊ0
	 */
	private void calculate_node_prob_dfs(Topic node,Document doc,double logsum[],boolean logsumFirstFlag[],double path_prob[],Vector<Double> veceta)
	{
		int currLevel=node.level;
		path_prob[currLevel]=level_log_gamma_ratio(doc,node,currLevel,veceta);//�Ƿֲ���p75 <***>ʽ��,����Ϊʲô����������,�ʼǱ�����������
		
		double log_total_consumer_num=0.0;
		
		if(currLevel>0)//level=0ʱ���ü������,ѡ�����topic�ĸ��ʾ���1//�����Ѿ�������//��ߵĸ�����currLevel==1��ʱ��Ҫ�޸�
		{
			if(currLevel==1)//����ǵڶ���//�ǵÿ����ߵ�����һ��·��
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
				case 0://��1
					//ע�������˺������,������Ժ���Ҫ��һ����,�����ȻҪ�����,��Ҳ��ζ���ұߵĸ���ȫ��0
					path_prob[currLevel]+=Math.log(this.lambda);
					break;
				case 1://��2
					//ע�������˺������,������Ժ���Ҫ��һ����,�����ȻҪ�����,��Ҳ��ζ���ұߵĸ���ȫ��0
					path_prob[currLevel]+=Math.log(1-this.lambda);
					break;
				default:
					System.err.println("Error:�ý�㲻����,ȴcalculate_node_prob_dfs��������������");
					System.exit(0);
				}
				
			}
			else
			{
				log_total_consumer_num=Math.log(node.parent.nTotalDocumentToTopic+this.gamma);//Ϊʲôû��-1,����Ϊ��ƪ�ĵ��Ѿ���remove����
				path_prob[currLevel]+=Math.log(node.nTotalDocumentToTopic)-log_total_consumer_num;
			}
		}
		
		//�����������Ķ��ӿ�ʼΪ�½��,������Ǵ����������prob�е�
		if(currLevel<this.depth-1)//����ǵײ��level,�����Ķ��Ӳ��ǲ����ܲ����½��ģ�������ǵײ��level�Ͳ������ĸ�����
		{
			int nterms=node.posterior_log_prob_words.size();
			for(int l=currLevel+1;l<this.depth;++l)//�ڶ�����Ϊ�µ�,����,��Ϊ�ڶ��������ʱ,l=currLevel+2��ʼ��Ϊ
			{
				double eta=veceta.get(l);
				path_prob[l]=level_log_gamma_ratio_new(doc,l,eta,nterms);
			}
			//ֻ��Ҫ��һ�� gamma/n+gamma;����ʵ�ı��ʽ����Ҳ��������,�ڵ�l������½��Ļ���ֻ���ڵ�l�����һ��gamma,Ȼ����l+1�����¶����ó�gamma��ֻ���level_log_gamma_ratio_new
			path_prob[currLevel+1]+=Math.log(this.gamma);//�µ�,��������ı�
			path_prob[currLevel+1]-=Math.log(node.nTotalDocumentToTopic+this.gamma);
		}
		
		//������node�Ķ��ӿ�ʼΪ�½��ĸ���
		
		node.prob=0;
		for(int l=0;l<this.depth;++l)
		{
			node.prob+=path_prob[l];
		}
		
		//�����һ������,����
		if(currLevel==0)
		{
			//ʵ��������������ǲ����ܳ��ֵ�
			System.err.println("Error: �������calculate_node_prob_dfs�б�ִ��");
			System.exit(0);
			logsum[0]=node.prob;//����Ǹ����Ļ�,ֻ�����һ��log_gamma_ratio,��ȻҲֻ�������︳ֵһ��
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
		
		//DFS�ݹ�

		for(int c=0;c<node.nchilds;++c)
		{
			calculate_node_prob_dfs(node.childs.get(c),doc,logsum,logsumFirstFlag,path_prob,veceta);
		}
		
		//return logsum[0];//Debug��
	}
	
	/*
	 * ����p75<***>ʽ,level�ǵ�ǰʽ���������,��Ӧ������p18��ͷ��ʽ���е���������˺�
	 */
	double level_log_gamma_ratio(Document doc,Topic node,int level,Vector<Double> veceta)
	{
		int nterms=node.posterior_log_prob_words.size();
		int nwords=doc.words.size();
		//System.out.println("The nterms is "+nterms);
		//System.out.println("The word size is "+nwords);
		int n;
		int count[]=new int[nterms];//�ֵ��С
		double result;
		for(n=0;n<nwords;++n)
		{
			count[doc.words.get(n)]=0;//��ʼ��
		}
		for(n=0;n<nwords;++n)
		{
			if(doc.levels.get(n)==level)
			{
				++count[doc.words.get(n)];//ͳ����ƪ�ĵ��г��ֵĸ��ֵ��ʵ���Ŀ,δ���ֵ���0��//z=l
			}
		}
		
		double eta=veceta.get(node.level);
		result=Dirich.logGammaLanczos(node.nTotalWordsToTopic+nterms*eta);
		result-=Dirich.logGammaLanczos(node.nTotalWordsToTopic+doc.tot_levels.get(level)+nterms*eta);
		
		for(n=0;n<nwords;++n)//p75 <**>ʽ,��Ӧ��������p18��ͷʽ���ڲ������˺�
		{
	        int wd = doc.words.get(n);
	        if (count[wd] > 0)//���count==0�أ�count==0�Ļ�,�Ǿ�ֱ��Լ����
	        {
	            result -= Dirich.logGammaLanczos(node.nWordsToTopic.get(wd)+eta);
	            result += Dirich.logGammaLanczos(node.nWordsToTopic.get(wd)+count[wd]+eta);
	            count[wd] = 0;//������,��ֹ�ظ�����,�����ڶ�������ͬ����ĴʾͲ����ټӽ�ȥ��
	        }
		}
		return result;
	}
	
	/*
	 * ����p75<***>ʽ,level��level+1:depth,��ʾ�������㿪ʼΪnew
	 */
	double level_log_gamma_ratio_new(Document doc,int level,double eta,int nterms)
	{
		int n;
		int count[]=new int[nterms];
		double result;
		int nwords=doc.words.size();
		for(n=0;n<nwords;++n)
		{
			count[doc.words.get(n)]=0;
		}
		for(n=0;n<nwords;++n)
		{
			if(doc.levels.get(n)==level)
			{
				++count[doc.words.get(n)];
			}
		}
		
		result=Dirich.logGammaLanczos(nterms*eta);
		result-=Dirich.logGammaLanczos(doc.tot_levels.get(level)+nterms*eta);
		
		for(n=0;n<nwords;++n)
		{
			int wd=doc.words.get(n);
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
	 * ����������Ƴ���ƪ�ĵ�ռ�е�·��
	 */
	private void treeRemoveDocFromPath(Document doc,Vector<Double> veceta)
	{
		treeUpdateForDoc(doc,-1,veceta);
		treeUpdateForTreeBranch(doc.path.get(this.depth-1));//�����ڶ���,������û�н��,
	}
	
	/*
	 * �Ƴ�һ��·����,ÿƪ�ĵ�������·���ᷢ���仯,Ȼ�����topic��������word���ᷢ���仯
	 * ֮�����ȿ����ĵ���ص����ݵĸ���,����Ϊ����ȸ���·��,��ô�������topic���,��treeUpdateForDoc�лᱨ��
	 * ���ĵ���ص�topic�ĵ�����-1,��ÿ��doc�漰���ĵ���
	 */
	private void treeUpdateForDoc(Document doc,int update,Vector<Double> veceta)
	{
		int depth=this.depth;
		int nwords=doc.words.size();
		
		int n;
		
	    for (n=0;n<nwords;++n)
	    {
	        int level =doc.levels.get(n);
	        if (level > 0)//Ϊʲô��һ�㲻���¼���?��Ϊ�������Ǵӵڶ��㿪ʼ�ɵ�,��һ���topic������Զ��1
	        {
	            doc.path.get(level).topic_update_nWordsToTopic(doc.words.get(n),update,level,veceta);
	        }
	    }
	    for (n=1;n<depth;++n)//Ϊʲô��һ�㲻���¼���?��������ʱ�ò�����һ��ĸ���,��һ��ĸ��ʸ����Ͳ��洢,��һ���prob�������һ��Ϊ�½��ĸ���
	    {
	    	doc.path.get(n).topic_update_nDocumentsToTopic(update);
	    }
	}
	
	/*
	 * �Ƴ�һ��·����,����tree�������(CRPTreeNode�е�����),�Ե�����
	 */
	private void treeUpdateForTreeBranch(Topic node)
	{
		Topic parent=node.parent;
		if(node.nTotalDocumentToTopic==0)
		{
			//System.out.println("��Ҫ�Ƴ�topic���");//debug����û��topic��㱻�Ƴ�
			treeDeleteNode(node);//����ζ��һƪ�ĵ����������,��Ϊֻ��һƪ�ĵ�ʱ,���ڵ㽫�ᱻɾ��,�޷��洢����
			if(parent!=null)//����һ��,��Ϊ���չ�root��㣿root���nTotalDocumentToTopic������=0������Ի���Ƕ����,��Ϊ���ڵ�node.nTotalDocumentToTopic==0��Զ���ᷢ��
			//ԭ���ĳ�����Ϊ���չ�һƪ�ĵ������,��ʵ��һƪ�ĵ������ɲ�����,��Ϊ����һ�������ͱ�remove����,˭���洢�ڶ��㿪ʼ�����½��ĸ��ʣ����Ժ��������޸���,GibbsInit������if(i>0),��������һƪ�ĵ��Ϳ�ʼ����
			{
				treeUpdateForTreeBranch(parent);
			}
			else
			{
				System.out.println("Error: �ĵ���==1�����������,���ĵ���>1������³�����root.nTotalDocumentToTopic==0�����");
				System.exit(0);
			}
		}
		
	}
	
	/*
	 * ����ɾ��һ�����
	 * ���Ҫ����ɾ�������ĸ����,����ǵڶ���Ľ��,(��һ��Ľ����Զ����ɾ��),�Ǹ��µķ�ʽ�ɾͲ�һ����
	 */
	private void treeDeleteNode(Topic node)
	{
		//���к��ӽ��ɾ��
		for(int c=0;c<node.nchilds;++c)
		{
			treeDeleteNode(node.childs.get(c));
		}
		if(node.level>1)//��CRP���
		{
			//����node����parent����е�����
			int nc=node.parent.nchilds;
			for(int c=0;c<nc;++c)
			{
				if(node.parent.childs.get(c)==node)//���õ����//ע��,����и���������,����ֻ�����һ��,���㲻�ٻ����,��Ϊtopic�Ķ�һ�޶���
				{
					Topic nc_1node=node.parent.childs.get(nc-1);
					node.parent.childs.set(c,nc_1node);
					node.parent.childs.remove(nc-1);//��C�в�����ʽɾ��,��Ϊ�����ǹ̶���,������java�����鳤�ȿɱ�,�����ʽɾ��,��Ϊ��java��,������treeFill���������ʹ��add��childsβ�������.
					--node.parent.nchilds;
					--nc;//������ᷢ��topic����û�б�����������,��Ϊ"=="ֻ�ᷢ��һ��,���緢����,��Ŀ���Ѵﵽ,����break��,Ҳ����nc--,����û����,��ôҲ���ᷢ��nc--����Ϊ
				}
			}
			//��2�ֿ���,һ����λ��nc-1�Ľ��ȷʵnWordsToTopic��Ϊnull,����һ�ֿ��ܾ���node��ָ��λ��Ϊc�Ľ��(�µ�),����nWordsToTopic=null
			
			//�ͷ��ڴ�
			node.nWordsToTopic.clear();
			node.nWordsToTopic=null;
			node.posterior_log_prob_words.clear();
			node.posterior_log_prob_words=null;
			node.childs.clear();
			node.childs=null;
			node=null;//ע�Ⱑ,���������docpath�л��Ǵ��ڵİ�,ֻ����childs�м���clear,������childs�в�������,��ʵ����node=nullû��ʲôʵ��������
		}
		else//��PriorTree�����һ��Ľ��//ע��,level
		{
			if(node.level==0)//��Ӧ����ͷ���,ͷ��㲻Ӧ��ɾ��,ֻ��2�������ִ�����,һ��ֻ��һƪ�ĵ�,���ǳ���������
			{
				System.err.println("Error:�ĵ���==1����������⣬");
				System.exit(0);
			}
			//����node����parent����е�����
			
			//��ִ�е�����˵��node.level==1
			int nc=node.parent.nchilds;
			for(int c=0;c<nc;++c)
			{
				if(node.parent.childs.get(c)==node)//���õ����//ע��,����и���������,����ֻ�����һ��,���㲻�ٻ����,��Ϊtopic�Ķ�һ�޶���
				{
					node.parent.childs.set(c,null);//��Ϊnull,��ζ���������Ѿ����ͷ���,��Ȼnode.parent.childs.size()��Ȼ==2
					--node.parent.nchilds;
				}
			}
			node.nWordsToTopic.clear();
			node.nWordsToTopic=null;
			node.posterior_log_prob_words.clear();
			node.posterior_log_prob_words=null;
			node.childs.clear();
			node.childs=null;
			node=null;
		}
	}
	
	/*
	 * ����level0ProbSlot�����������ڵ���level0ProbSlot
	 */
	private Topic treeSamplePath_Withlevel0ProbSlot(double logsum,Vector<Double> level0ProbSlot,int retLevel1NewNodeIdx[])
	{
		double runningsum[]=new double[]{0.0};//ֻ��һ��,û��ϵ,Ҫ����2������,����ÿһ�㶼Ҫ��һ������
		double rand=Math.random();
		int nullCount=0;
		for(int c=0;c<2;++c)//һ��DFS�Ĺ���
		{
			if(level0ProbSlot.get(c)!=null)//�ĸ���Ϊnull�ĸ���Ҫ�������
			{
				++nullCount;
				runningsum[0]+=Math.exp(level0ProbSlot.get(c)-logsum);
				if(runningsum[0]>=rand)
				{
					retLevel1NewNodeIdx[0]=c;
					return this.root;//this.root����Ҫ���½����,���������ʾ�ӵ�����һ����?һ�ַ����ǽ�probһ��,����������̫��,�����retLevel1NewNodeIdx��һ��
				}
				//else//��һ�����,���ǵ�1��(��0���)���û���,�����ұ��н��,��߲�ȡ�½��,ֻ�ܴ��ұ߿�ʼ����,����,�ұ�������forѭ����
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
		if(nullCount==2)//��2��null,ֻ�����������:һƪ�ĵ�,���2ƪ�������ĵ������������,���ǳ���д����
		{
			System.err.println("Error: ��2��null,ֻ�����������:һƪ�ĵ�,���2ƪ�������ĵ������������,���ǳ���д����");
			System.exit(0);
		}
		System.err.println("Error: û�в��������,���ʼ���������");
		return null;
	}
	
	/*
	 * ����level0ProbSlot�����������ڵ���level0ProbSlot
	 * ����stance flag
	 * ��ʵ����һ�ַ���������Ƿ�Ϊnull,�����null,����left,��ֱ�Ӳ�,���ʶ�������
	 * ����ֻ��ŵ�һ��������,һ������stance_flag����switch,Ȼ��鿴stance_flag��Ӧ��slot�Ƿ�Ϊnull
	 */
	private Topic treeSamplePath_Withlevel0ProbSlot_flag_left(double logsum,Vector<Double> level0ProbSlot,int retLevel1NewNodeIdx[])
	{
		double runningsum[]=new double[]{0.0};//ֻ��һ��,û��ϵ,Ҫ����2������,����ÿһ�㶼Ҫ��һ������
		double rand=Math.random();
		
		if(level0ProbSlot.get(0)!=null)
		{
			runningsum[0]+=Math.exp(level0ProbSlot.get(0)-logsum);
			//�ؼ����ұ�û���˽�����,��Ȼ����߿�ʼ
			retLevel1NewNodeIdx[0]=0;
			return this.root;//this.root����Ҫ���½����,���������ʾ�ӵ�����һ����?һ�ַ����ǽ�probһ��,����������̫��,�����retLevel1NewNodeIdx��һ��
		}
		else
		{
			Topic varnode=treeSampleDFS(rand,this.root.childs.get(0),runningsum,logsum);//����߲�
			if(varnode!=null)
			{
				return varnode;
			}
		}
		System.err.println("Error: û�в��������,���ʼ���������");
		return null;
	}

	/*
	 * ����level0ProbSlot�����������ڵ���level0ProbSlot
	 * ����stance flag
	 * ��ʵ����һ�ַ������ұ��Ƿ�Ϊnull,�����null,����right,��ֱ�Ӳ�,���ʶ�������
	 * ����ֻ��ŵ�һ��������,һ������stance_flag����switch,Ȼ��鿴stance_flag��Ӧ��slot�Ƿ�Ϊnull
	 */
	private Topic treeSamplePath_Withlevel0ProbSlot_flag_right(double logsum,Vector<Double> level0ProbSlot,int retLevel1NewNodeIdx[])
	{
		double runningsum[]=new double[]{0.0};//ֻ��һ��,û��ϵ,Ҫ����2������,����ÿһ�㶼Ҫ��һ������
		double rand=Math.random();
		
		if(level0ProbSlot.get(1)!=null)
		{
			runningsum[0]+=Math.exp(level0ProbSlot.get(1)-logsum);
			//�ؼ������û���˽�����,��Ȼ����߿�ʼ
			retLevel1NewNodeIdx[0]=1;
			return this.root;//this.root����Ҫ���½����,���������ʾ�ӵ�����һ����?һ�ַ����ǽ�probһ��,����������̫��,�����retLevel1NewNodeIdx��һ��
		}
		else
		{
			Topic varnode=treeSampleDFS(rand,this.root.childs.get(1),runningsum,logsum);//���ұ߲�
			if(varnode!=null)
			{
				return varnode;
			}
		}
		System.err.println("Error: û�в��������,���ʼ���������");
		return null;
	}

	
	///*
	// * ��֪ÿ�������Ϊĩ��·���ĸ��ʷֲ�,sampleһ�����
	// */
	//private Topic treeSamplePath(double logsum)
	//{
	//	double runningsum[]=new double[]{0.0};//����dfs,ÿ����һ�����ͼ��Ͻ��ĸ���,��ʵ�����Կ���ƽ�̵�,ֻ�ǰ���DFSȥ��������
	//	double rand=Math.random();
	//	return treeSampleDFS(rand,this.root,runningsum,logsum);
	//}
	
	/*
	 * ����DFS�ķ������ز������Ľ��,����һ���ݹ�
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
				if(varnode!=null)//Ϊʲô��ֱ��return?��Ϊ���ֱ��return,��ô�ڵ�һ��·������ʱ�ͻ���ֹforѭ��
				{
					return varnode;
				}
			}
		}
		return null;//���·�������˵׶�,�ͻ����������
	}
	
	/*
	 * ���ĵ���ӵ�nodeΪβ��·��,�����ĵ�����
	 */
	private void treeAddDocToPath(Topic node,Document doc,Vector<Double> veceta)
	{
		int depth=this.depth;
		int l=depth-1;//��Զ��depth-1�Ľ�㴢��,ÿ��·��������Ȳ�һ,ʵ���ϴ洢ʱ��ȶ�Ϊdepth-1
		do
		{
			doc.path.set(l,node);
			node=node.parent;
			--l;
		}while(l>=0);
		
		//�ĵ����ʹ�������topic,����
		treeUpdateForDoc(doc,1,veceta);
	}
	
	/*
	 * ����������ļ�
	 */
	public void writeTree(BufferedWriter treebw) throws IOException
	{
		String alineToWrite=String.format("%-6s %-6s %-6s %-6s  %-9s %-6s\n","ID", "PARENT", "NDOCS", "NWORDS", "SCALE", "WD_CNT");
		treebw.write(alineToWrite);
		writeTreeDFS(this.root,treebw);
	}
	
	
	/*
	 * ��������ȵķ������һ����
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
		alineToWrite=String.format(" %06.0f",node.nTotalWordsToTopic);
		treebw.write(alineToWrite);
		alineToWrite=String.format(" %06.3e",this.gamma);
		treebw.write(alineToWrite);

		for(int i=0;i<node.nWordsToTopic.size();++i)
		{
			alineToWrite=String.format(" %6.0f",node.nWordsToTopic.get(i));
			treebw.write(alineToWrite);
		}
		treebw.write("\n");

		for(int i=0;i<node.nchilds;++i)
		{
			writeTreeDFS(node.childs.get(i),treebw);
		}
		
	}
	
	/*
	 * ����������е���������
	 */
	public int ntopicsInTree()
	{
		return ntopicsInTreeDFS(this.root);
	}
	
	/*
	 * ��dfs����������е���������
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
