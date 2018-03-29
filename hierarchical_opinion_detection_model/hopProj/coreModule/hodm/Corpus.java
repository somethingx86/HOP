package hodm;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.Scanner;
public class Corpus {
	public int ndocs;//语料库中的文档数量
	public int nkeys;//语料库中的phrase种类数量
	public ArrayList<Document> docs;

	public Corpus(String corpusFileName,int depth){
		
		//控制台输出日志
		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String outlogContent=String.format("READING CORPUS FROM %s", corpusFileName);
		String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
		
		//开始读入
		try{
			int nunique;//一篇文档phrase的种类数
			int count;//一篇文档中一种phrase的个数,是个临时变量
			int phrase;//一篇文档中一种phrase号,即key标识,是个临时变量
			int n,i;//迭代变量,n相当于w_{d,n}中的n;i是用在for循环里面的,没有明确意义
			int total=0;//所有文档的总phrase数量,即语料库的phrase数量
			Document doc;//doc临时变量,用来申请新doc用
			
			this.nkeys=0;
			this.ndocs=0;
			this.docs=new ArrayList<Document>(4096);//初始capacity=4096
			
			File fileCorpus=new File(corpusFileName);
			BufferedReader bfCorpus=new BufferedReader(new InputStreamReader(new FileInputStream(fileCorpus)));
			Scanner scanner=new Scanner(bfCorpus);
			
			while(scanner.hasNext()){
				nunique=scanner.nextInt();//一篇文档中的phrase类数
				
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
				doc.phrases=new java.util.ArrayList<Integer>(64);//认为一篇文档有64个phrase
				
				scanner.useDelimiter(" |\r\n|:");//认为":"也是分隔符
				
				//System.out.println(nunique);
				for(n=0;n<nunique;++n)
				{
					phrase=scanner.nextInt();//获得phrase号
					count=scanner.nextInt();//获得phrase数
					//System.out.println(word+":"+count);
					total+=count;//语料库总phrase数增加
					
					if(phrase>=this.nkeys)//phrase是从0开始编号的,nkeys要比编号最大的phrase大1
					{
						this.nkeys=phrase+1;
					}
					for(i=0;i<count;++i)
					{
						doc.phrases.add(phrase);//把phrase加入到文档phrase向量中
					}
				}
				
				//设定(申请)吉布斯采样要用到的变量,但是还没有赋值,只是初始化为0
				doc.levels=new java.util.ArrayList<Integer>(doc.phrases.size());
				doc.path=new java.util.ArrayList<Topic>(depth);//这个今后要当vector来用,预先初始化
				doc.tot_levels=new java.util.ArrayList<Double>(depth);//这个今后要当vector来用,预先初始化
				doc.log_p_levels=new java.util.ArrayList<Double>(depth);//这个今后要当vector来用,预先初始化
				for(n=0;n<depth;++n)
				{
					doc.path.add(null);//在ArrayList中预留空间
					doc.tot_levels.add(null);//在ArrayList中预留空间
					doc.log_p_levels.add(null);//在ArrayList中预留空间
				}
				
				for(n=0;n<doc.phrases.size();++n)//每个phrase分配的层初始化,为-1,表示还没开始采样
				{
					doc.levels.add(-1);
				}
				
				outlogContent=String.format("number of docs    : %d", this.ndocs);
				outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
				System.out.printf("%s\n",outlog);
				
				outlogContent=String.format("number of keys   : %d", this.nkeys);
				outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
				System.out.printf("%s\n",outlog);
				
				outlogContent=String.format("total phrase count  : %d",total);
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
	 * 把docs按层输出到.log文件,输出所有文档,每篇文档的phrase和每个phrase所属的层,只有在按照gibbsScore输出treeStructure,docPathAssignment,docWordLevels时才会用到
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
				for(int n=0;n<this.docs.get(d).phrases.size();++n)
				{
					if(n>0)//如果是第一个phrase,是要预先输出一个phrase前的空格的
					{
						corpusBw.write(" ");
						alineToWrite=String.format("%d:%d",this.docs.get(d).phrases.get(n),this.docs.get(d).levels.get(n));
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
