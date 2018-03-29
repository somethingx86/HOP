package hodm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 吉布斯采样
 * @author ZhuLixing
 *
 */
public class GibbsSampling {
	private GibbsState currGibbsState;
	private final int NINITREP=100;//找100次最优吉布斯初始状态
	public GibbsSampling(String corpusFileName,String settingsFileName,String out_dir)
	{
		//控制台输出日志
		SimpleDateFormat dataformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		String outlogContent=String.format("initializing state");
		String outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
		
//		GibbsState bestGibbsState;
//		double best_score=0;
//		int i;
//		for(i=0;i<this.NINITREP;++i)
//		{
//			GibbsState gibbsState=new GibbsState(corpusFileName,settingsFileName);
//		}
		
		currGibbsState=new GibbsState(corpusFileName,settingsFileName);
		currGibbsState.initGibbsState();
		
		//创建目录以及日志目录以及文件
		if(out_dir!=null)
		{
			currGibbsState.setUpDirectories(out_dir);
			String filename_in_dir_to_write=String.format("%s/initial",currGibbsState.run_dir);
			currGibbsState.writeGibbsState(filename_in_dir_to_write);
			filename_in_dir_to_write=String.format("%s/mode",currGibbsState.run_dir);
			currGibbsState.writeGibbsState(filename_in_dir_to_write);
		}
				
		outlogContent=String.format("done initializing state");//输出信息到控制台
		outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
	}
	
	/*
	 * 让currState迭代采样iterateTimes次
	 * 迭代完后释放directory,并结束
	 */
	public void GibbsSamplingConduct(int iterateTimes)
	{
		for(int i=0;i<iterateTimes;++i)
		{
			this.currGibbsState.iterateGibbsState();
		}
		this.currGibbsState.treelogbwAndscorelogbwClose();
	}
}
