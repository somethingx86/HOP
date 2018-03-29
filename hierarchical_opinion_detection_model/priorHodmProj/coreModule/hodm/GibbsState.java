package hodm;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.Collections;
import java.util.Date;

/**
 * 一个GibbsState,它拥有自己的log存放路径,拥有自己的iterate方法,可以定义多个GibbsState,以多线程进行吉布斯采样工作.
 * @author ZhuLixing
 *
 */
public class GibbsState {
	//一个GibbsState有一个Document库和Tree,其中Document库掌管隐变量,Tree辅助采样
	private Corpus corpus;
	private HODMTree hodmTree;
	
	//运行中logFile的存放路径
	public String run_dir;//在当前目录存logFile的路径
	private BufferedWriter tree_structure_log_bw;//存放树结构的BufferedWriter
	private BufferedWriter score_log_bw;//存放score的log,这里是为了应付格式,从而能够使用tree.py,实际上并无意义
	
	//变量
    private int iter;//迭代次数
    private Vector<Double> eta;//吉布斯采样中的超参eta,是个常量
    private Vector<Double> alpha;//吉布斯采样中的超参alpha,是个常量
	private final int output_lag=100;//吉布斯采样中每隔output_lag行输出到文件中,是个常量
    
	//创建一个新的GibbsState,一个corpus是关联一个语料库的,所以这个方法仅在开始寻找最优初始gibbsState时被调用100次
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
			Scanner scanner=new Scanner(brSettings);//注意：空格是分隔符
			scanner.next(Pattern.compile("DEPTH"));
			depth=scanner.nextInt();//读取深度
			scanner.next("ETA");
			
			eta=new Vector<Double>(depth);//读取ETA//为什么eta是一个与层有关的标量呢?正常情况下应该是向量,或者是与层有关的nterms维的向量的啊,或者说,depth*nterms,是这样子,这里作者认为topic是关于eta的symmertic分布
			eta.setSize(depth);//size和capacity不是一回事
			for(int i=0;i<depth;++i){
				double v=scanner.nextDouble();
				eta.setElementAt(v,i);//关键是这个Vector长度虽然是depth,但是此时是空的,要先加对象,故不能用set
			}
			scanner.next("GAM");
			gamma=scanner.nextDouble();//读取GAMMA
			scanner.next("ALPHA");
			alpha=new Vector<Double>(depth);//读取ALPHA
			alpha.setSize(depth);
			for(int i=0;i<depth;++i){
				double v=scanner.nextDouble();
				alpha.setElementAt(v,i);
			}
			
			scanner.close();
			brSettings.close();
			fis.close();
			
			this.iter=0;
			this.corpus=new Corpus(corpusFileName,depth);//读语料库
			
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
	 * 构造函数gibbsState的补充,只需用到已赋值的成员变量
	 * 添加先验时，一定还要注意一下初始化的时候!
	 */
	public void initGibbsState()
	{
		Collections.shuffle(this.corpus.docs);//随机序列化docs,其实就相当于换一种顺序读文件
		int i,j;
		for(i=0;i<this.corpus.ndocs;++i)
		{
			Document doc_i=this.corpus.docs.get(i);
			for(j=0;j<doc_i.tot_levels.size();++j)
			{
				doc_i.tot_levels.set(j, 0.0);
				doc_i.log_p_levels.set(j, 0.0);
			}
			Collections.shuffle(doc_i.words);//把单词的语序打乱
			
			int flag_doc_stance = doc_i.stance_flag;
			Topic buttomChildNode=null;
			if(flag_doc_stance == 0)
			{
				buttomChildNode=this.hodmTree.priorTreeFillNodeInInitState(this.hodmTree.root,this.corpus.nterms,this.eta);//在获得最底端的孩子结点的同时,整个树的一条path已经产生
				doc_i.path.set(this.hodmTree.depth-1, buttomChildNode);//注意,此时path也就最底端有值,其余都是null,要在下面赋值
			}
			else 
			{
				if(flag_doc_stance == -1)//从左到右-1 1
				{
					buttomChildNode=this.hodmTree.priorTreeFillNodeInInitState_left(this.hodmTree.root,this.corpus.nterms,this.eta);//在获得最底端的孩子结点的同时,整个树的一条path已经产生
					doc_i.path.set(this.hodmTree.depth-1, buttomChildNode);//注意,此时path也就最底端有值,其余都是null,要在下面赋值
				}
				else
				{
					buttomChildNode=this.hodmTree.priorTreeFillNodeInInitState_right(this.hodmTree.root,this.corpus.nterms,this.eta);//在获得最底端的孩子结点的同时,整个树的一条path已经产生
					doc_i.path.set(this.hodmTree.depth-1, buttomChildNode);//注意,此时path也就最底端有值,其余都是null,要在下面赋值
				}
			}
			buttomChildNode.topic_update_nDocumentsToTopic(1);//给这个底端的topic分配的文档数+1
			for(j=this.hodmTree.depth-2;j>=0;--j)
			{
				doc_i.path.set(j,doc_i.path.get(j+1).parent);//这边设置为parent就出问题了
				doc_i.path.get(j).topic_update_nDocumentsToTopic(1);//此时文档的path都关联上crpTreeNode了
			}
			
			doc_i.sample_doc_levels_withThisDoc(this.hodmTree.depth, this.alpha, this.eta);
			if(i>0)//i=0的时候为什么不sample？因为i=0的时候的tree_sample_doc_path其实已经在topic初始化之中了,用的是常数概率,而且还在doc_sample_levels之前。如果执行tree_sample_doc_path(tr, d, 1, 0);,就相当于再new一次topic,没有意义
			{
				this.hodmTree.treeSampleDocPathRemoveThisDoc(doc_i,this.eta);
			}
			doc_i.sample_doc_levels_removeThisDoc(this.hodmTree.depth, this.alpha,this.eta);
			
			//System.out.println("当前主题数量"+this.crpTree.new_topic_id);
		}
		
	}
	
	/*
	 * 设定吉布斯采样中途状态输出目录,创建这个目录,设定树日志的目录文件的名称
	 */
	public void setUpDirectories(String out_dir)
	{
		try {
			this.run_dir=out_dir;
			int id=0;//directory的id
	
			this.run_dir=String.format("%s/run%03d", out_dir, id);
			
		    while (directoryExist(this.run_dir))
		    {
		        ++id;
		        this.run_dir=String.format("%s/run%03d", out_dir, id);//自动按序寻找一个可用于输出的目录
		    }
		    File directoryForRun=new File(this.run_dir);
		    directoryForRun.mkdir();
		    //创建各种输出文件
		    String filename;
		    filename=String.format("%s/tree.log", this.run_dir);//树的log文件,为了在iterate state中输出
		    this.tree_structure_log_bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)),"utf-8"));
		    filename=String.format("%s/score.log", this.run_dir);//score的log文件,为了在iterate state中输出
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
	 * 判断一个目录是否存在,存在返回true
	 */
	private boolean directoryExist(String dir_name)
	{
		File directoryForCheck=new File(dir_name);//java中文件和文件夹是同一个概念,只不过有isDirectory的说法
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
	 * 把这个gibbsState初始状态输出到out_filename的指定file中
	 */
	public void writeGibbsState(String out_filename)
	{
		//CRPTree tr=this.crpTree;
		//Corpus corp=this.corpus;
		double score=0.0;//我们这个版本是不计算GibbsScore的
		
		//SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		//String outlogContent=String.format("initializing state");
		//String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		//System.out.printf("%s\n",outlog);
		
		//String topic_filename=String.format("%s.topics", out_filename);//输出topic的filename
		
		try{
			
			String docAssignFilename=String.format("%s.assign", out_filename);
			File docAssignFile=new File(docAssignFilename);//这个file是用来输出当前每篇文档分配的topic id号的路径
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
	 * 把gibbsState的tree中的每条路径出到指定log文件中,便于每次迭代后实时监视,因为涉及到iterate过程的输出,所以bw是常开着的
	 * 同时每隔一段iterate调用一下前面的writeGibbsState
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
		if(this.run_dir!=null)//每间隔一段迭代期输出一次一棵树的结构
		{
	        String filename;//filename,其中filename被用来输出树信息,filename.assign被用来输出doc信息,但是这些细节由writeGibbsState函数完成
	        if(this.output_lag>0 && (this.iter%this.output_lag)==0)//每隔1000输出一次
	        {
	        	filename=String.format( "%s/iter=%06d", this.run_dir, this.iter);
	        	writeGibbsState(filename);
	        }
	        
    		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//控制台输出的日期格式
    		String outlogContent;
    		String outlog;
    		
	        if(this.output_lag>0 && (this.iter%this.output_lag)==0)//也每隔1000输出一次mod 
	        {
	        	outlogContent=String.format("mode at iteration %04d", this.iter);
	    		outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
	    		System.out.printf("%s\n",outlog);
	        	
	        	filename=String.format("%s/mode", this.run_dir);//score最棒的mod
	        	writeGibbsState(filename);
	        	
	        	filename=String.format("%s/mode.levels", this.run_dir);//score最棒的mod.levels
	        	BufferedWriter corpusBw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)),"utf-8"));
	        	//把doc按level输出到.log文件
	        	this.corpus.writeCorpusLevels(corpusBw);;//只有按score计分的才需要输出每篇doc中每个单词对应的level
	        	corpusBw.close();
	        }
	        
		}
	}
	
	/*
	 * 输出当前每篇文档分配的topic id号的路径
	 */
	private void writeCorpusAssignment(BufferedWriter docbw) throws IOException
	{
		String alineToWrite;
		for(int d=0;d<this.corpus.ndocs;++d)
		{
			alineToWrite=String.format("%d", this.corpus.docs.get(d).id);
			docbw.write(alineToWrite);
			alineToWrite=String.format(" %1.9e",0.0);//score的输出,这里我们一律输出0.0
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
	 * 吉布斯函数迭代
	 */
	public void iterateGibbsState()
	{
		this.iter+=1;//更新迭代号
		
		//控制台输出日志
		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String outlogContent=String.format("iteration %04d (%04d topics)",this.iter,this.hodmTree.ntopicsInTree());
		String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
		
		//路径的采样
		//看论文和.c程序我们可以得知，其实这两个顺序倒没什么关系
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
	 * 关闭这两个文件流,并把引用设置为null
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
