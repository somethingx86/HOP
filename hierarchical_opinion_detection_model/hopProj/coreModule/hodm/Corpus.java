package hodm;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.Scanner;
public class Corpus {
	public int ndocs;//���Ͽ��е��ĵ�����
	public int nkeys;//���Ͽ��е�phrase��������
	public ArrayList<Document> docs;

	public Corpus(String corpusFileName,int depth){
		
		//����̨�����־
		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String outlogContent=String.format("READING CORPUS FROM %s", corpusFileName);
		String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
		
		//��ʼ����
		try{
			int nunique;//һƪ�ĵ�phrase��������
			int count;//һƪ�ĵ���һ��phrase�ĸ���,�Ǹ���ʱ����
			int phrase;//һƪ�ĵ���һ��phrase��,��key��ʶ,�Ǹ���ʱ����
			int n,i;//��������,n�൱��w_{d,n}�е�n;i������forѭ�������,û����ȷ����
			int total=0;//�����ĵ�����phrase����,�����Ͽ��phrase����
			Document doc;//doc��ʱ����,����������doc��
			
			this.nkeys=0;
			this.ndocs=0;
			this.docs=new ArrayList<Document>(4096);//��ʼcapacity=4096
			
			File fileCorpus=new File(corpusFileName);
			BufferedReader bfCorpus=new BufferedReader(new InputStreamReader(new FileInputStream(fileCorpus)));
			Scanner scanner=new Scanner(bfCorpus);
			
			while(scanner.hasNext()){
				nunique=scanner.nextInt();//һƪ�ĵ��е�phrase����
				
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
				doc.phrases=new java.util.ArrayList<Integer>(64);//��Ϊһƪ�ĵ���64��phrase
				
				scanner.useDelimiter(" |\r\n|:");//��Ϊ":"Ҳ�Ƿָ���
				
				//System.out.println(nunique);
				for(n=0;n<nunique;++n)
				{
					phrase=scanner.nextInt();//���phrase��
					count=scanner.nextInt();//���phrase��
					//System.out.println(word+":"+count);
					total+=count;//���Ͽ���phrase������
					
					if(phrase>=this.nkeys)//phrase�Ǵ�0��ʼ��ŵ�,nkeysҪ�ȱ������phrase��1
					{
						this.nkeys=phrase+1;
					}
					for(i=0;i<count;++i)
					{
						doc.phrases.add(phrase);//��phrase���뵽�ĵ�phrase������
					}
				}
				
				//�趨(����)����˹����Ҫ�õ��ı���,���ǻ�û�и�ֵ,ֻ�ǳ�ʼ��Ϊ0
				doc.levels=new java.util.ArrayList<Integer>(doc.phrases.size());
				doc.path=new java.util.ArrayList<Topic>(depth);//������Ҫ��vector����,Ԥ�ȳ�ʼ��
				doc.tot_levels=new java.util.ArrayList<Double>(depth);//������Ҫ��vector����,Ԥ�ȳ�ʼ��
				doc.log_p_levels=new java.util.ArrayList<Double>(depth);//������Ҫ��vector����,Ԥ�ȳ�ʼ��
				for(n=0;n<depth;++n)
				{
					doc.path.add(null);//��ArrayList��Ԥ���ռ�
					doc.tot_levels.add(null);//��ArrayList��Ԥ���ռ�
					doc.log_p_levels.add(null);//��ArrayList��Ԥ���ռ�
				}
				
				for(n=0;n<doc.phrases.size();++n)//ÿ��phrase����Ĳ��ʼ��,Ϊ-1,��ʾ��û��ʼ����
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
	 * ��docs���������.log�ļ�,��������ĵ�,ÿƪ�ĵ���phrase��ÿ��phrase�����Ĳ�,ֻ���ڰ���gibbsScore���treeStructure,docPathAssignment,docWordLevelsʱ�Ż��õ�
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
					if(n>0)//����ǵ�һ��phrase,��ҪԤ�����һ��phraseǰ�Ŀո��
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
