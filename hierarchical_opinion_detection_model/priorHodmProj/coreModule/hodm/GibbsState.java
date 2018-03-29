package hodm;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.Collections;
import java.util.Date;

/**
 * һ��GibbsState,��ӵ���Լ���log���·��,ӵ���Լ���iterate����,���Զ�����GibbsState,�Զ��߳̽��м���˹��������.
 * @author ZhuLixing
 *
 */
public class GibbsState {
	//һ��GibbsState��һ��Document���Tree,����Document���ƹ�������,Tree��������
	private Corpus corpus;
	private HODMTree hodmTree;
	
	//������logFile�Ĵ��·��
	public String run_dir;//�ڵ�ǰĿ¼��logFile��·��
	private BufferedWriter tree_structure_log_bw;//������ṹ��BufferedWriter
	private BufferedWriter score_log_bw;//���score��log,������Ϊ��Ӧ����ʽ,�Ӷ��ܹ�ʹ��tree.py,ʵ���ϲ�������
	
	//����
    private int iter;//��������
    private Vector<Double> eta;//����˹�����еĳ���eta,�Ǹ�����
    private Vector<Double> alpha;//����˹�����еĳ���alpha,�Ǹ�����
	private final int output_lag=100;//����˹������ÿ��output_lag��������ļ���,�Ǹ�����
    
	//����һ���µ�GibbsState,һ��corpus�ǹ���һ�����Ͽ��,��������������ڿ�ʼѰ�����ų�ʼgibbsStateʱ������100��
	public GibbsState(String corpusFileName,String settingsFileName)
	{
		int depth;
		Vector<Double> eta;
		double gamma;
		Vector<Double> alpha;
		try{
			File settingsFile=new File(settingsFileName);
			FileInputStream fis=new FileInputStream(settingsFile);
			BufferedReader brSettings=new BufferedReader(new InputStreamReader(fis,"utf-8"));
			Scanner scanner=new Scanner(brSettings);//ע�⣺�ո��Ƿָ���
			scanner.next(Pattern.compile("DEPTH"));
			depth=scanner.nextInt();//��ȡ���
			scanner.next("ETA");
			
			eta=new Vector<Double>(depth);//��ȡETA//Ϊʲôeta��һ������йصı�����?���������Ӧ��������,����������йص�ntermsά�������İ�,����˵,depth*nterms,��������,����������Ϊtopic�ǹ���eta��symmertic�ֲ�
			eta.setSize(depth);//size��capacity����һ����
			for(int i=0;i<depth;++i){
				double v=scanner.nextDouble();
				eta.setElementAt(v,i);//�ؼ������Vector������Ȼ��depth,���Ǵ�ʱ�ǿյ�,Ҫ�ȼӶ���,�ʲ�����set
			}
			scanner.next("GAM");
			gamma=scanner.nextDouble();//��ȡGAMMA
			scanner.next("ALPHA");
			alpha=new Vector<Double>(depth);//��ȡALPHA
			alpha.setSize(depth);
			for(int i=0;i<depth;++i){
				double v=scanner.nextDouble();
				alpha.setElementAt(v,i);
			}
			
			scanner.close();
			brSettings.close();
			fis.close();
			
			this.iter=0;
			this.corpus=new Corpus(corpusFileName,depth);//�����Ͽ�
			
			this.eta=eta;
			this.alpha=alpha;
			
			this.hodmTree=new HODMTree(depth,this.corpus.nterms,eta,gamma);
			this.run_dir=null;
			this.tree_structure_log_bw=null;
			this.score_log_bw=null;
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	/*
	 * ���캯��gibbsState�Ĳ���,ֻ���õ��Ѹ�ֵ�ĳ�Ա����
	 * �������ʱ��һ����Ҫע��һ�³�ʼ����ʱ��!
	 */
	public void initGibbsState()
	{
		Collections.shuffle(this.corpus.docs);//������л�docs,��ʵ���൱�ڻ�һ��˳����ļ�
		int i,j;
		for(i=0;i<this.corpus.ndocs;++i)
		{
			Document doc_i=this.corpus.docs.get(i);
			for(j=0;j<doc_i.tot_levels.size();++j)
			{
				doc_i.tot_levels.set(j, 0.0);
				doc_i.log_p_levels.set(j, 0.0);
			}
			Collections.shuffle(doc_i.words);//�ѵ��ʵ��������
			
			int flag_doc_stance = doc_i.stance_flag;
			Topic buttomChildNode=null;
			if(flag_doc_stance == 0)
			{
				buttomChildNode=this.hodmTree.priorTreeFillNodeInInitState(this.hodmTree.root,this.corpus.nterms,this.eta);//�ڻ����׶˵ĺ��ӽ���ͬʱ,��������һ��path�Ѿ�����
				doc_i.path.set(this.hodmTree.depth-1, buttomChildNode);//ע��,��ʱpathҲ����׶���ֵ,���඼��null,Ҫ�����渳ֵ
			}
			else 
			{
				if(flag_doc_stance == -1)//������-1 1
				{
					buttomChildNode=this.hodmTree.priorTreeFillNodeInInitState_left(this.hodmTree.root,this.corpus.nterms,this.eta);//�ڻ����׶˵ĺ��ӽ���ͬʱ,��������һ��path�Ѿ�����
					doc_i.path.set(this.hodmTree.depth-1, buttomChildNode);//ע��,��ʱpathҲ����׶���ֵ,���඼��null,Ҫ�����渳ֵ
				}
				else
				{
					buttomChildNode=this.hodmTree.priorTreeFillNodeInInitState_right(this.hodmTree.root,this.corpus.nterms,this.eta);//�ڻ����׶˵ĺ��ӽ���ͬʱ,��������һ��path�Ѿ�����
					doc_i.path.set(this.hodmTree.depth-1, buttomChildNode);//ע��,��ʱpathҲ����׶���ֵ,���඼��null,Ҫ�����渳ֵ
				}
			}
			buttomChildNode.topic_update_nDocumentsToTopic(1);//������׶˵�topic������ĵ���+1
			for(j=this.hodmTree.depth-2;j>=0;--j)
			{
				doc_i.path.set(j,doc_i.path.get(j+1).parent);//�������Ϊparent�ͳ�������
				doc_i.path.get(j).topic_update_nDocumentsToTopic(1);//��ʱ�ĵ���path��������crpTreeNode��
			}
			
			doc_i.sample_doc_levels_withThisDoc(this.hodmTree.depth, this.alpha, this.eta);
			if(i>0)//i=0��ʱ��Ϊʲô��sample����Ϊi=0��ʱ���tree_sample_doc_path��ʵ�Ѿ���topic��ʼ��֮����,�õ��ǳ�������,���һ���doc_sample_levels֮ǰ�����ִ��tree_sample_doc_path(tr, d, 1, 0);,���൱����newһ��topic,û������
			{
				this.hodmTree.treeSampleDocPathRemoveThisDoc(doc_i,this.eta);
			}
			doc_i.sample_doc_levels_removeThisDoc(this.hodmTree.depth, this.alpha,this.eta);
			
			//System.out.println("��ǰ��������"+this.crpTree.new_topic_id);
		}
		
	}
	
	/*
	 * �趨����˹������;״̬���Ŀ¼,�������Ŀ¼,�趨����־��Ŀ¼�ļ�������
	 */
	public void setUpDirectories(String out_dir)
	{
		try {
			this.run_dir=out_dir;
			int id=0;//directory��id
	
			this.run_dir=String.format("%s/run%03d", out_dir, id);
			
		    while (directoryExist(this.run_dir))
		    {
		        ++id;
		        this.run_dir=String.format("%s/run%03d", out_dir, id);//�Զ�����Ѱ��һ�������������Ŀ¼
		    }
		    File directoryForRun=new File(this.run_dir);
		    directoryForRun.mkdir();
		    //������������ļ�
		    String filename;
		    filename=String.format("%s/tree.log", this.run_dir);//����log�ļ�,Ϊ����iterate state�����
		    this.tree_structure_log_bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)),"utf-8"));
		    filename=String.format("%s/score.log", this.run_dir);//score��log�ļ�,Ϊ����iterate state�����
		    this.score_log_bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)),"utf-8"));
	
		    String alineToWrite;
		    alineToWrite=String.format("%6s %14s %14s %14s %14s %10s %10s",
		            "iter", "gem.score", "eta.score", "gamma.score",
		            "total.score", "gem.mean", "gem.scale");
		    this.score_log_bw.write(alineToWrite);
		    for (int l = 0; l < this.hodmTree.depth-1; ++l)
		    {
		    	alineToWrite=String.format(" %8s.%d", "gamma", l);
		    	this.score_log_bw.write(alineToWrite);
		    }
		    for (int l = 0; l < this.hodmTree.depth; ++l)
		    {
		    	alineToWrite=String.format(" %8s.%d", "eta", l);
		    	this.score_log_bw.write(alineToWrite);
		    }
		    this.score_log_bw.write("\n");
		    this.score_log_bw.flush();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * �ж�һ��Ŀ¼�Ƿ����,���ڷ���true
	 */
	private boolean directoryExist(String dir_name)
	{
		File directoryForCheck=new File(dir_name);//java���ļ����ļ�����ͬһ������,ֻ������isDirectory��˵��
		if(directoryForCheck.exists()&&directoryForCheck.isDirectory())
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	/*
	 * �����gibbsState��ʼ״̬�����out_filename��ָ��file��
	 */
	public void writeGibbsState(String out_filename)
	{
		//CRPTree tr=this.crpTree;
		//Corpus corp=this.corpus;
		double score=0.0;//��������汾�ǲ�����GibbsScore��
		
		//SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		//String outlogContent=String.format("initializing state");
		//String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		//System.out.printf("%s\n",outlog);
		
		//String topic_filename=String.format("%s.topics", out_filename);//���topic��filename
		
		try{
			
			String docAssignFilename=String.format("%s.assign", out_filename);
			File docAssignFile=new File(docAssignFilename);//���file�����������ǰÿƪ�ĵ������topic id�ŵ�·��
			FileOutputStream docAssignFos=new FileOutputStream(docAssignFile);
			BufferedWriter docAssignBw=new BufferedWriter(new OutputStreamWriter(docAssignFos,"utf-8"));
			writeCorpusAssignment(docAssignBw);
			docAssignBw.close();
			docAssignFos.close();
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	/*
	 * ��gibbsState��tree�е�ÿ��·������ָ��log�ļ���,����ÿ�ε�����ʵʱ����,��Ϊ�漰��iterate���̵����,����bw�ǳ����ŵ�
	 * ͬʱÿ��һ��iterate����һ��ǰ���writeGibbsState
	 */
	private void writeGibbsOutput() throws IOException
	{
		String alineToWrite;
		if(this.score_log_bw!=null)
		{
			alineToWrite=String.format("%06d %14.3f %14.3f %14.3f %14.3f %7.4e %7.4e",
	                this.iter, 0.0, 0.0,
	                0.0, 0.0,
	                0.0, 0.0);
			this.score_log_bw.write(alineToWrite);
	        for (int l = 0; l < this.hodmTree.depth - 1; ++l)
	        {
	        	alineToWrite=String.format(" %7.4e", this.hodmTree.gamma);
	        	this.score_log_bw.write(alineToWrite);
	        }
	        for (int l = 0; l < this.hodmTree.depth; ++l)
	        {
	        	alineToWrite=String.format(" %7.4e", this.eta.get(l));
	        	this.score_log_bw.write(alineToWrite);
	        }
	        this.score_log_bw.write("\n");
	        this.score_log_bw.flush();
		}
		
		if(this.tree_structure_log_bw!=null)
		{
			this.hodmTree.writeTreeLevels(this.tree_structure_log_bw);
		}
		if(this.run_dir!=null)//ÿ���һ�ε��������һ��һ�����Ľṹ
		{
	        String filename;//filename,����filename�������������Ϣ,filename.assign���������doc��Ϣ,������Щϸ����writeGibbsState�������
	        if(this.output_lag>0 && (this.iter%this.output_lag)==0)//ÿ��1000���һ��
	        {
	        	filename=String.format( "%s/iter=%06d", this.run_dir, this.iter);
	        	writeGibbsState(filename);
	        }
	        
    		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//����̨��������ڸ�ʽ
    		String outlogContent;
    		String outlog;
    		
	        if(this.output_lag>0 && (this.iter%this.output_lag)==0)//Ҳÿ��1000���һ��mod 
	        {
	        	outlogContent=String.format("mode at iteration %04d", this.iter);
	    		outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
	    		System.out.printf("%s\n",outlog);
	        	
	        	filename=String.format("%s/mode", this.run_dir);//score�����mod
	        	writeGibbsState(filename);
	        	
	        	filename=String.format("%s/mode.levels", this.run_dir);//score�����mod.levels
	        	BufferedWriter corpusBw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)),"utf-8"));
	        	//��doc��level�����.log�ļ�
	        	this.corpus.writeCorpusLevels(corpusBw);;//ֻ�а�score�ƷֵĲ���Ҫ���ÿƪdoc��ÿ�����ʶ�Ӧ��level
	        	corpusBw.close();
	        }
	        
		}
	}
	
	/*
	 * �����ǰÿƪ�ĵ������topic id�ŵ�·��
	 */
	private void writeCorpusAssignment(BufferedWriter docbw) throws IOException
	{
		String alineToWrite;
		for(int d=0;d<this.corpus.ndocs;++d)
		{
			alineToWrite=String.format("%d", this.corpus.docs.get(d).id);
			docbw.write(alineToWrite);
			alineToWrite=String.format(" %1.9e",0.0);//score�����,��������һ�����0.0
			docbw.write(alineToWrite);			
			
			for(int l=0;l<this.hodmTree.depth;++l)
			{
				alineToWrite=String.format(" %d", this.corpus.docs.get(d).path.get(l).id);
				docbw.write(alineToWrite);
			}
			docbw.write("\n");
			
		}
	}
	
	/*
	 * ����˹��������
	 */
	public void iterateGibbsState()
	{
		this.iter+=1;//���µ�����
		
		//����̨�����־
		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String outlogContent=String.format("iteration %04d (%04d topics)",this.iter,this.hodmTree.ntopicsInTree());
		String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
		
		//·���Ĳ���
		//�����ĺ�.c�������ǿ��Ե�֪����ʵ������˳��ûʲô��ϵ
		for(int d=0;d<this.corpus.ndocs;++d)
		{
			this.hodmTree.treeSampleDocPathRemoveThisDoc(this.corpus.docs.get(d),this.eta);
		}
		for(int d=0;d<this.corpus.ndocs;++d)
		{
			this.corpus.docs.get(d).sample_doc_levels_removeThisDoc(this.hodmTree.depth, this.alpha,this.eta);
		}
		
		try{
			writeGibbsOutput();
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	/*
	 * �ر��������ļ���,������������Ϊnull
	 */
	public void treelogbwAndscorelogbwClose()
	{
		try {
			this.tree_structure_log_bw.close();
			this.score_log_bw.close();
			this.tree_structure_log_bw=null;
			this.score_log_bw=null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
