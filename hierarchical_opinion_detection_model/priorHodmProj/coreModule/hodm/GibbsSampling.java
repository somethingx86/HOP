package hodm;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ����˹����
 * @author ZhuLixing
 *
 */
public class GibbsSampling {
	private GibbsState currGibbsState;
	private final int NINITREP=100;//��100�����ż���˹��ʼ״̬
	public GibbsSampling(String corpusFileName,String settingsFileName,String out_dir)
	{
		//����̨�����־
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
		
		//����Ŀ¼�Լ���־Ŀ¼�Լ��ļ�
		if(out_dir!=null)
		{
			currGibbsState.setUpDirectories(out_dir);
			String filename_in_dir_to_write=String.format("%s/initial",currGibbsState.run_dir);
			currGibbsState.writeGibbsState(filename_in_dir_to_write);
			filename_in_dir_to_write=String.format("%s/mode",currGibbsState.run_dir);
			currGibbsState.writeGibbsState(filename_in_dir_to_write);
		}
				
		outlogContent=String.format("done initializing state");//�����Ϣ������̨
		outlog=String.format("%s %s",dataformat.format(new Date()),outlogContent);
		System.out.printf("%s\n",outlog);
	}
	
	/*
	 * ��currState��������iterateTimes��
	 * ��������ͷ�directory,������
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
