package hodm;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.Scanner;
public class Corpus {
	public int ndocs;//语料库中的文档数量
	public int nterms;//语料库中的单词种类数量
	public ArrayList<Document> docs;

	public Corpus(String corpusFileName,int depth){
		
		//控制台输出日志
		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String outlogContent=String.format("READING CORPUS FROM %s", corpusFileName);
		String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
		
		//开始读入
		try{
			int flag_stance;//文档是否有stance以及具体的stance
			int nunique;//一篇文档单词的种类数
			int count;//一篇文档中一种单词的个数,是个临时变量
			int word;//一篇文档中一种单词号,是个临时变量
			int flag_word_stance;//单词是否有stance,是个临时变量
			int n,i;//迭代变量,n相当于w_{d,n}中的n;i是用在for循环里面的,没有明确意义
			int total=0;//所有文档的总单词数量,即语料库的单词数量
			Document doc;//doc临时变量,用来申请新doc用
			
			this.nterms=0;
			this.ndocs=0;
			this.docs=new ArrayList<Document>(4096);//初始capacity=4096
			
			File fileCorpus=new File(corpusFileName);
			BufferedReader bfCorpus=new BufferedReader(new InputStreamReader(new FileInputStream(fileCorpus)));
			Scanner scanner=new Scanner(bfCorpus);
			
			while(scanner.hasNext()){
				//一篇文档是否stance路径已经确定
				flag_stance=scanner.nextInt();//文档的stance
				
				nunique=scanner.nextInt();//一篇文档中的单词类数
				
				this.ndocs=this.ndocs+1;
				
				if(this.ndocs%100==0)//输出控制台log
				{
					outlogContent=String.format("read document %d", this.ndocs);
					outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
					System.out.printf("%s\n",outlog);
				}
				
				doc=new Document();
				this.docs.add(doc);//按照顺序加入一篇document到尾部
				doc.id=this.ndocs-1;//文档本身编号被记录,从0开始编号
				doc.stance_flag = flag_stance;//文档stance被记录
				doc.words=new java.util.ArrayList<Integer>(64);//认为一篇文档有64个单词
				doc.words_stance_flag=new java.util.ArrayList<Integer>(64);//认为一篇文档有64个单词
				
				scanner.useDelimiter(" |\r\n|:");//认为":"也是分隔符
				
				//System.out.println(nunique);
				for(n=0;n<nunique;++n)
				{
					word=scanner.nextInt();//获得单词
					count=scanner.nextInt();//获得单词数
					flag_word_stance=scanner.nextInt();//获得word_stance_flag
					//System.out.println(word+":"+count);
					total+=count;//语料库总单词数增加
					
					if(word>=this.nterms)//word是从0开始编号的,nterms要比编号最大的word大1
					{
						this.nterms=word+1;
					}
					for(i=0;i<count;++i)
					{
						doc.words.add(word);//把单词加入到文档单词向量中
						doc.words_stance_flag.add(flag_word_stance);//把单词的stance加入到文档单词flag向量中
					}
				}
				
				//设定(申请)吉布斯采样要用到的变量,但是还没有赋值,只是初始化为null
				doc.levels=new java.util.ArrayList<Integer>(doc.words.size());
				doc.path=new java.util.ArrayList<Topic>(depth);//这个今后要当vector来用,预先初始化
				doc.tot_levels=new java.util.ArrayList<Double>(depth);//这个今后要当vector来用,预先初始化
				doc.log_p_levels=new java.util.ArrayList<Double>(depth);//这个今后要当vector来用,预先初始化
				for(n=0;n<depth;++n)
				{
					doc.path.add(null);//在ArrayList中预留空间
					doc.tot_levels.add(null);//在ArrayList中预留空间
					doc.log_p_levels.add(null);//在ArrayList中预留空间
				}
				
				for(n=0;n<doc.words.size();++n)//每个单词分配的层初始化,为-1,表示还没开始采样
				{
					doc.levels.add(-1);
				}
				
				outlogContent=String.format("number of docs    : %d", this.ndocs);
				outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
				System.out.printf("%s\n",outlog);
				
				outlogContent=String.format("number of terms   : %d", this.nterms);
				outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
				System.out.printf("%s\n",outlog);
				
				outlogContent=String.format("total word count  : %d",total);
				outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
				System.out.printf("%s\n",outlog);
				
			}
			scanner.close();
			bfCorpus.close();
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	/*
	 * 把docs按层输出到.log文件,输出所有文档,每篇文档的单词和每个单词所属的层,只有在按照gibbsScore输出treeStructure,docPathAssignment,docWordLevels时才会用到
	 */
	public void writeCorpusLevels(BufferedWriter corpusBw)
	{
		try
		{
			SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			String outlogContent=String.format("writing all corpus level variables");
			String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
			System.out.printf("%s\n",outlog);
			
			String alineToWrite;
			
			for(int d=0;d<this.ndocs;++d)
			{
				for(int n=0;n<this.docs.get(d).words.size();++n)
				{
					if(n>0)//如果是第一个单词,是要预先输出一个单词前的空格的
					{
						corpusBw.write(" ");
						alineToWrite=String.format("%d:%d",this.docs.get(d).words.get(n),this.docs.get(d).levels.get(n));
						corpusBw.write(alineToWrite);
					}
				}
				corpusBw.write("\n");
			}
			
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
