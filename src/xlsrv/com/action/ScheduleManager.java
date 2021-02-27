package xlsrv.com.action;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import xlsrv.com.action.ScheduleManager;
import xlsrv.com.action.SxService;

public class ScheduleManager {
	private ScheduledThreadPoolExecutor m_schedule=null;
	private Logger log = Logger.getLogger(ScheduleManager.class);
	private Map<String, Object> paramMap = new HashMap<String, Object>();
	public static final int SCHEDULE_TYPE_LONG=6;
	public static final int SCHEDULE_TYPE_NORMAL=7;
	public static final int SCHEDULE_TYPE_MINUTELY=8;
	public static final int SCHEDULE_TYPE_HOURLY=9;
	public static final int SCHEDULE_TYPE_DAILY=10;
    public static final int SCHEDULE_TYPE_WEEKLY=11;
    public static final int SCHEDULE_TYPE_MONTHLY=12;
    private Document ConfigFile=null;
    private String ConfigFileName="";
    private Node ConfigNode=null;
    private Boolean CfgReload=true;
	
	public ScheduleManager() {
        Calendar calendar = Calendar.getInstance();

        //打印首次执行时间
		calendar.add(Calendar.SECOND, 10);       // 控制秒  
		Date date = calendar.getTime();  
		SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		log.info("首次执行时间为 "+sim.format(date));
		System.out.println("首次执行时间为 "+sim.format(date));
		
		//创建线程池
		m_schedule = new ScheduledThreadPoolExecutor(5);
		//初始化执行对象
		SxService task_long = new SxService(SCHEDULE_TYPE_LONG,this);
		SxService task_normal = new SxService(SCHEDULE_TYPE_NORMAL,this);
		m_schedule.scheduleAtFixedRate(task_long, 1000*0, 10000,TimeUnit.MILLISECONDS);	//安排指定的任务在指定的时间开始进行重复的固定延迟执行。
		m_schedule.scheduleAtFixedRate(task_normal, 1000*5, 10000,TimeUnit.MILLISECONDS);	
		
		//初始化Day|Week|Month对象----- ly schedule
		addDWHTask(SCHEDULE_TYPE_DAILY);
		addDWHTask(SCHEDULE_TYPE_WEEKLY);
		addDWHTask(SCHEDULE_TYPE_MONTHLY);
		addDWHTask(SCHEDULE_TYPE_HOURLY);
		addDWHTask(SCHEDULE_TYPE_MINUTELY);
    }
	
	public boolean addDWHTask(int taskType){//非时循环，需要关联对象，自行再启动。ly schedule
		//Do
		SxService task_obj = new SxService(taskType,this);
		m_schedule.schedule(task_obj, Delay(taskType),TimeUnit.MILLISECONDS);	
		
		return true;
	}
	

	public boolean ReloadDWHTask(int taskType,SxService obj){//非时循环，需要关联对象，自行再启动。ly schedule
		//Do
		m_schedule.schedule(obj, Delay(taskType),TimeUnit.MILLISECONDS);	
		
		return true;
	}
	
	public long Delay(int taskType) {
		//执行天任务 比如说今天8点38分执行天任务 ly schedule
		Integer day_m=1, day_w=1, hour_s=17, minute_s=57;
        Calendar calender_schedule = Calendar.getInstance();//当前日历类并设置定时时间
        calender_schedule.clear();
        calender_schedule.setTime(new Date()); //取得当前时间
        
        //System.out.println(GetTime(calender_schedule));
        if (taskType==SCHEDULE_TYPE_MONTHLY) {
        		Integer day=getConfigNodeAttrInt(transScheduleNode(taskType),"Day");
    			calender_schedule.set(Calendar.DAY_OF_MONTH, day==-1 ? day_m : day); //对于日期型，每天8点，执行。
        }
        if (taskType==SCHEDULE_TYPE_WEEKLY) {
	    		Integer day=getConfigNodeAttrInt(transScheduleNode(taskType),"Day");
			calender_schedule.set(Calendar.DAY_OF_WEEK, day==-1 ? day_w : day); //对于日期型，每天8点，执行。
	    }
        if (taskType!=SCHEDULE_TYPE_HOURLY && taskType!=SCHEDULE_TYPE_MINUTELY) {
        		Integer hour=getConfigNodeAttrInt(transScheduleNode(taskType),"Hour");
        		calender_schedule.set(Calendar.HOUR_OF_DAY, hour==-1 ? hour_s : hour); //对于日期型，每天8点，执行。
        }
        if (taskType!=SCHEDULE_TYPE_MINUTELY) {
        		Integer minute=getConfigNodeAttrInt(transScheduleNode(taskType),"Minute");
        		calender_schedule.set(Calendar.MINUTE, minute==-1 ? minute_s : minute); //对于所有类型，为每小时的38分钟执行
        }
        
        //测试
        //System.out.println(GetTime(calender_schedule));
        
        return Delay(calender_schedule,taskType);
	}
	
	public String GetTime(Calendar calender) {
	      SimpleDateFormat sim = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	      return sim.format(calender.getTime());
	}
	
	public long Delay(Calendar calender_schedule,int taskType) {
		long delay=0;
         
        Date date_now=new Date();
        Calendar calendar_now = Calendar.getInstance();//当前日历类并设置当前时间
        calendar_now.setTime(date_now);
        long now = calendar_now.getTimeInMillis()+2000;//获取当前时间的毫秒值; 确保下一次循环不重复，需要+2秒
        System.out.println(GetTime(calendar_now));
         
     
        long shedule=calender_schedule.getTimeInMillis();
        long period=0;//间隔（多少毫秒后第一次执行）
        /*long oneminute=60*1000;//一小时多少毫秒
        long onehour=60*60*1000;//一小时多少毫秒
        long oneday=24*60*60*1000;//一天多少毫秒
        long oneweek=7*oneday;//一周多少毫秒
       
        //下个月这个号
        Calendar nextMonthThisDay= (Calendar) calendar_now.clone();
        nextMonthThisDay.set(Calendar.MONTH, calendar_now.get(Calendar.MONTH)+1);
        long onemonth=nextMonthThisDay.getTimeInMillis()-calendar_now.getTimeInMillis();*/
       
        if(taskType==SCHEDULE_TYPE_MINUTELY){
            //period=oneminute;
			delay=shedule-now;
			if(delay<0){//如果延时为负（说明已经超过了执行的时间  加一后再执行）
			      calender_schedule.set(Calendar.MINUTE, calender_schedule.get(Calendar.MINUTE)+1);
			      shedule=calender_schedule.getTimeInMillis();
			      delay=shedule-now;
			 } 
			System.out.println("Task Reload at(MINUTELY):"+GetTime(calender_schedule));
       }else if(taskType==SCHEDULE_TYPE_HOURLY){
            //period=onehour;
			delay=shedule-now;
			if(delay<0){//如果延时为负（说明已经超过了执行的时间  加一后再执行）
			      calender_schedule.set(Calendar.HOUR, calender_schedule.get(Calendar.HOUR)+1);
			      shedule=calender_schedule.getTimeInMillis();
			      delay=shedule-now;//
			 } 
			System.out.println("Task Reload at(HOURLY):"+GetTime(calender_schedule));
       }else if(taskType==SCHEDULE_TYPE_DAILY){
             //period=oneday;
			 delay=shedule-now;
			 if(delay<0){//如果延时为负（说明已经超过了执行的时间  加一后再执行）
			       calender_schedule.set(Calendar.DAY_OF_MONTH, calender_schedule.get(Calendar.DAY_OF_MONTH)+1);
			       shedule=calender_schedule.getTimeInMillis();
			       delay=shedule-now;//
			  } 
			 System.out.println("Task Reload at(Daily):"+GetTime(calender_schedule));
        }else if(taskType==SCHEDULE_TYPE_WEEKLY){
              //period=oneweek;
              delay=shedule-now;
              if(delay<0){//如果延时为负（说明已经超过了执行的时间  加一后再执行）
                   calender_schedule.set(Calendar.DAY_OF_MONTH, calender_schedule.get(Calendar.DAY_OF_MONTH)+7);
                   shedule=calender_schedule.getTimeInMillis();
                   delay=shedule-now;//
               }
              System.out.println("Task Reload at(Weekly):"+GetTime(calender_schedule));
        }else if (taskType==SCHEDULE_TYPE_MONTHLY){
              //period =onemonth;
              delay=shedule-now;
              if(delay<0){//如果延时为负（说明已经超过了执行的时间  加一后再执行）
                   calender_schedule.set(Calendar.MONTH, calender_schedule.get(Calendar.MONTH)+1);
                   shedule=calender_schedule.getTimeInMillis();
                   delay=shedule-now;
               }
              System.out.println("Task Reload at(Monthly):"+GetTime(calender_schedule));
         }
        //stpe.scheduleAtFixedRate(aut, delay,period, TimeUnit.MILLISECONDS);
		//return delay+period;
        return delay;
	}
	
	public static String transScheduleNode(Integer type) {
		if (type==ScheduleManager.SCHEDULE_TYPE_WEEKLY) {
			return "_Schedule_Week";
		}else if (type==ScheduleManager.SCHEDULE_TYPE_DAILY) {
			return "_Schedule_Day";
		}else if (type==ScheduleManager.SCHEDULE_TYPE_MONTHLY) {
			return "_Schedule_Month";
		}else if (type==ScheduleManager.SCHEDULE_TYPE_HOURLY) {
			return "_Schedule_Hour";
		}else if (type==ScheduleManager.SCHEDULE_TYPE_MINUTELY) {
			return "_Schedule_Minute";
		}else if (type==ScheduleManager.SCHEDULE_TYPE_NORMAL) {
			return "_Schedule_Normal";
		}else if (type==ScheduleManager.SCHEDULE_TYPE_LONG) {
			return "_Schedule_Long";
		}
		return "";
	}
	
	//取得WebService 节点
	public Node getWebServiceCfg(String act_file,String act){
		//打开文件
		if (this.ConfigFile==null || this.ConfigFileName!=act_file) {//打开一次，文件名没有变化则不再打开
			try{
				DocumentBuilderFactory domfac=DocumentBuilderFactory.newInstance();
				DocumentBuilder dombuilder=domfac.newDocumentBuilder();
				InputStream is = WebService.class.getClassLoader().getResourceAsStream(act_file);
				this.ConfigFile=dombuilder.parse(is);
				this.ConfigFileName=act_file;
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		}
		//查询节点
		try{	
			Element root = this.ConfigFile.getDocumentElement();
			NodeList nodes = root.getChildNodes();
			if(nodes!=null){
				for (int i = 0; i < nodes.getLength(); i++){
					Node node = nodes.item(i);
					if(node.getNodeType()==Node.ELEMENT_NODE) { 
						if(node.getNodeName().equals(act)==true) {
							return node; 
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return null;
	}
	
	public Integer getConfigNodeAttrInt(String act,String attrName){
		String num_str=this.getConfigNodeAttr(act, attrName);
		if (num_str==null || !isNumeric(num_str))
			return -1;
		else 
			return Integer.parseInt(num_str);
	}
	

	private boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
	}
	
	public String getConfigNodeAttr(String act,String attrName){
		Map<String,String>attrMap=this.getConfigNodeAttr(act);
		if (attrMap==null)
			return null;
		return attrMap.containsKey(attrName) ? attrMap.get(attrName) : null;
	}
	
	public Map<String,String> getConfigNodeAttr(String act){
		this.ConfigNode=getWebServiceCfg("Service.xml",act);
		if (ConfigNode==null)
			return null;
		return getParseNodeAttrs(this.ConfigNode);	
	}
	
	public Map<String,String> getParseNodeAttrs(Node parseNode){
		Map<String,String> map=new HashMap<String,String>();
		NamedNodeMap attrs = parseNode.getAttributes();
		if (attrs==null)
			return null;
		
		for (int j = 0; j < attrs.getLength(); j++){
            Node attr = attrs.item(j);
            map.put(attr.getNodeName(), Xml_getNodeValue(attr));
        }
		return map;
	}

	
	public String Xml_getNodeValue(Node node2) {
		if (node2!=null &&(node2.getFirstChild())!=null) {
			return node2.getFirstChild().getNodeValue();
		}
		return null;
	}
}
