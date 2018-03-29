package mainpkg;
import funcUtil.Dirich;

import java.util.ArrayList;
import java.util.Vector;
import funcUtil.SampleFromProb;
import hodm.*;

import java.lang.Math;

public class Main {
	public static void main(String argv[])
	{	
		GibbsSampling gsamp=new GibbsSampling("BrexitTweets.phrased.ldatrain","settings-d4.txt","outdir_7_27");
		gsamp.GibbsSamplingConduct(10000);//迭代10000次
		//--------------------------------------------------测试部分
		
		//Integer it=10;
		//System.out.println(it.doubleValue());
		//System.out.println((double)it.doubleValue());
		
		
		//ArrayList<tt> al=new ArrayList<tt>(20);
		//tt t=new tt();
		//t.a=1;
		//t.b=2;
		//al.add(t);
		//t.a=10;
		//t.b=20;
		//tt t2=al.get(0);
		//System.out.println(t2.a);//是引用的问题,可能是samplePath时add进去的结点是引用*/
		
		//ArrayList<tt> a1=new ArrayList<tt>(20);
		//tt t1=new tt(1,1);
		//tt t2=new tt(2,2);
		//a1.add(t1);
		//a1.add(t2);
		//tt i1=a1.get(0);
		//a1.set(0, t2);
		//System.out.println(i1.a);
		
		
		
//		alteredHLDA.AlteredHLDA ahlda=new alteredHLDA.AlteredHLDA();
//		String args[]=new String[]{"C:\\Download\\sample-data\\sample-data\\web\\en\\hawes.txt",""};
//		try {
//			File f=new File(args[0]);
//			System.out.println(f.exists());
			//InstanceList instances = InstanceList.load(f);
//			ObjectInputStream ois = new ObjectInputStream (new BufferedInputStream(new FileInputStream (f)));

			//InstanceList ilist = (InstanceList) ois.readObject();
//			ois.close();
			
//			//System.out.println(instances.size());
//			//InstanceList testing = InstanceList.load(new File(args[1]));
			
//			//AlteredHLDA sampler = new AlteredHLDA();
//			//sampler.initialize(instances, testing, 5, new Randoms());
//			//sampler.estimate(250);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
//		try{
//			File f=new File("C:\\Download\\sample-data\\sample-data\\web\\en\\hawes.txt");
//			System.out.println(f.exists());
//		}catch(Exception e){
//			e.printStackTrace();
//		}
		
		
		
		//System.out.print(Dirich.logGammaStirling(1.3));
		//System.out.println();
//		System.out.print(Dirich.logGammaLanczos(1.01));
//		System.out.println();
//		System.out.print(Dirich.logGammaStirling(1.01));
//		System.out.println();
//		System.out.print(Dirich.logGammaNemes(1.01));
		
		
		//java关于引用传值的一点心得:java中的double是没有assign和set函数的,还有一点,如果你在函数里面想改变成员的值,需要用set而不是单纯的引用赋值
		//--------------------------------------------------测试部分
	}
}

class tt
{
	public int a;
	public int b;
	public tt(int a,int b)
	{
		this.a=a;
		this.b=b;
	}
}
