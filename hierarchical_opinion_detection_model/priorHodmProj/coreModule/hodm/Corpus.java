package hodm;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.Scanner;
public class Corpus {
	public int ndocs;//���Ͽ��е��ĵ�����
	public int nterms;//���Ͽ��еĵ�����������
	public ArrayList<Document> docs;

	public Corpus(String corpusFileName,int depth){
		
		//����̨�����־
		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String outlogContent=String.format("READING CORPUS FROM %s", corpusFileName);
		String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
		
		//��ʼ����
		try{
			int flag_stance;//�ĵ��Ƿ���stance�Լ������stance
			int nunique;//һƪ�ĵ����ʵ�������
			int count;//һƪ�ĵ���һ�ֵ��ʵĸ���,�Ǹ���ʱ����
			int word;//һƪ�ĵ���һ�ֵ��ʺ�,�Ǹ���ʱ����
			int flag_word_stance;//�����Ƿ���stance,�Ǹ���ʱ����
			int n,i;//��������,n�൱��w_{d,n}�е�n;i������forѭ�������,û����ȷ����
			int total=0;//�����ĵ����ܵ�������,�����Ͽ�ĵ�������
			Document doc;//doc��ʱ����,����������doc��
			
			this.nterms=0;
			this.ndocs=0;
			this.docs=new ArrayList<Document>(4096);//��ʼcapacity=4096
			
			File fileCorpus=new File(corpusFileName);
			BufferedReader bfCorpus=new BufferedReader(new InputStreamReader(new FileInputStream(fileCorpus)));
			Scanner scanner=new Scanner(bfCorpus);
			
			while(scanner.hasNext()){
				//һƪ�ĵ��Ƿ�stance·���Ѿ�ȷ��
				flag_stance=scanner.nextInt();//�ĵ���stance
				
				nunique=scanner.nextInt();//һƪ�ĵ��еĵ�������
				
				this.ndocs=this.ndocs+1;
				
				if(this.ndocs%100==0)//�������̨log
				{
					outlogContent=String.format("read document %d", this.ndocs);
					outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
					System.out.printf("%s\n",outlog);
				}
				
				doc=new Document();
				this.docs.add(doc);//����˳�����һƪdocument��β��
				doc.id=this.ndocs-1;//�ĵ������ű���¼,��0��ʼ���
				doc.stance_flag = flag_stance;//�ĵ�stance����¼
				doc.words=new java.util.ArrayList<Integer>(64);//��Ϊһƪ�ĵ���64������
				doc.words_stance_flag=new java.util.ArrayList<Integer>(64);//��Ϊһƪ�ĵ���64������
				
				scanner.useDelimiter(" |\r\n|:");//��Ϊ":"Ҳ�Ƿָ���
				
				//System.out.println(nunique);
				for(n=0;n<nunique;++n)
				{
					word=scanner.nextInt();//��õ���
					count=scanner.nextInt();//��õ�����
					flag_word_stance=scanner.nextInt();//���word_stance_flag
					//System.out.println(word+":"+count);
					total+=count;//���Ͽ��ܵ���������
					
					if(word>=this.nterms)//word�Ǵ�0��ʼ��ŵ�,ntermsҪ�ȱ������word��1
					{
						this.nterms=word+1;
					}
					for(i=0;i<count;++i)
					{
						doc.words.add(word);//�ѵ��ʼ��뵽�ĵ�����������
						doc.words_stance_flag.add(flag_word_stance);//�ѵ��ʵ�stance���뵽�ĵ�����flag������
					}
				}
				
				//�趨(����)����˹����Ҫ�õ��ı���,���ǻ�û�и�ֵ,ֻ�ǳ�ʼ��Ϊnull
				doc.levels=new java.util.ArrayList<Integer>(doc.words.size());
				doc.path=new java.util.ArrayList<Topic>(depth);//������Ҫ��vector����,Ԥ�ȳ�ʼ��
				doc.tot_levels=new java.util.ArrayList<Double>(depth);//������Ҫ��vector����,Ԥ�ȳ�ʼ��
				doc.log_p_levels=new java.util.ArrayList<Double>(depth);//������Ҫ��vector����,Ԥ�ȳ�ʼ��
				for(n=0;n<depth;++n)
				{
					doc.path.add(null);//��ArrayList��Ԥ���ռ�
					doc.tot_levels.add(null);//��ArrayList��Ԥ���ռ�
					doc.log_p_levels.add(null);//��ArrayList��Ԥ���ռ�
				}
				
				for(n=0;n<doc.words.size();++n)//ÿ�����ʷ���Ĳ��ʼ��,Ϊ-1,��ʾ��û��ʼ����
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
	 * ��docs���������.log�ļ�,��������ĵ�,ÿƪ�ĵ��ĵ��ʺ�ÿ�����������Ĳ�,ֻ���ڰ���gibbsScore���treeStructure,docPathAssignment,docWordLevelsʱ�Ż��õ�
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
					if(n>0)//����ǵ�һ������,��ҪԤ�����һ������ǰ�Ŀո��
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
