package xlsrv.com.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

/**
 * 针对排序Group的简单数据聚合
 * Created by 李杨 on 20197/04/16.
 */

public class GetherGrid {
	String[] GroupColumns=null;
	String[] AddColumns=null;
	StringBuffer   XmlStr;
	String   CntMethod="";
	Integer Rec=0;
	String  CntCol="";
	String  Error="";
	String  LastValue="";
	List<Map<String, Object>> ListMap;
	String NodeMethod="";
	Boolean AddNum=true;
	Boolean AddNodes_Attr=true;
	Integer TransType=0;
	//String[] title= {"name","text","sum","id","num","children","\"checked\":false,\"open\":false"};
	String[] title= {"name","text","sum","id","num","son"};
	Integer Id=100000;
	
	public GetherGrid(List<Map<String, Object>> from_list_map, String group_columns, String add_columns,String add_columns_attr,String cnt_col,String cnt_method) {
		//取得参数List
		ListMap = from_list_map;
		this.GroupColumns=group_columns.split(";");
		this.CntCol=cnt_col;
		this.CntMethod=cnt_method;
		//this.Id=0;
		
		//聚合处理
		this.XmlStr=new StringBuffer("");
		this.Rec=0;
		this.Error="";
		this.LastValue="";
		//循环from_list_map查找剩余的字段。
		if (add_columns!=null && add_columns!="")
			this.AddColumns=add_columns.split(";");
		else
			this.AddColumns=null;
		this.AddNodes_Attr=((add_columns_attr!=null) && add_columns_attr.equals("true")) ? true : false;
		//测试字段是否存在
		/*if (from_list_map.size()!=0) {
			Map<String, Object> tmp=from_list_map.get(0);
			//测试group_columns；
			for(Integer i=0; i< this.GroupColumns.length; i++) {
				
			}
			//测试add_columns
			//测试cnt_col;
		}*/
	}
	
	public String GetherGridXml(Integer group_col,Integer Start, Integer End) {
		//this.Id++;
		StringBuffer tmp_xml=new StringBuffer("");
		Boolean is_leaf=false;
		if (this.ListMap.size()==0)
			return "";
		if (group_col >=(this.GroupColumns.length-1))
			is_leaf=true; 
		List<String> cnt_arr=new ArrayList<String>(); //累计数
		List<String> cnt_all_arr=new ArrayList<String>(); //累计数
		Map<String, Object> col_map_start=(Map<String, Object>)ListMap.get(Start);
		
		//循环记录
		Integer step_start=Start;
		String col_val_old=this.GetRecColValue(col_map_start, group_col); 
		String add_leaf_xml="";
		for(Integer i=Start; i<End; i++) {
			String r_val="";
			//取得当前记录和列值
			Map<String, Object> col_map=(Map<String, Object>)ListMap.get(i);
			String col_val=this.GetRecColValue(col_map, group_col);
			
			//判断列值变化，创建xml节点
			Boolean is_chg=!col_val_old.equals(col_val);
			Boolean is_end=(i==(End-1));
		
			if (is_leaf) {//如果页节点，
				//则取得当前节点值；
				if (this.CntCol.length()==0 || this.CntCol.equals(".")) {
					r_val="1";
				}else {
					if (!col_map.containsKey(this.CntCol)) {//不存在该字段。
						this.Error="Error: GetherGridXml->col("+this.CntCol+") field not found!";
						return null;
					}
					r_val=(String)col_map.get(this.CntCol);
				}
				//变化处理
				if (is_chg) {//如果前一层变化
					//递归区域内节点，累加值
					String cnt_node=DoMethod(cnt_arr);
					this.AddGroupNode(tmp_xml,this.GroupColumns[group_col],col_val_old, cnt_node,i-step_start,add_leaf_xml);
					//递进
					cnt_arr.clear(); 
					step_start=i;
					col_val_old=col_val;
					add_leaf_xml="";
				}
				
				//递进处理
				String tmp=this.AddColumns==null ? "" : this.AddNodes(col_map);//添加当前节点
				if (tmp==null)
					return null;//不存在该字段。
				add_leaf_xml+=tmp;
				cnt_arr.add(r_val); //累加计数
				
				//如果结束
				if (is_end) {
					String cnt_node=DoMethod(cnt_arr);
					this.AddGroupNode(tmp_xml,this.GroupColumns[group_col],col_val, cnt_node,i-step_start+1,add_leaf_xml);
				}
				
				cnt_all_arr.add(r_val);
			}else {//如果非叶节点
				if (is_chg) {//如果变化
					String last_xml=this.GetherGridXml(group_col+1,step_start,i);//则递归旧节点集
					r_val=this.LastValue;
					if (!this.Error.isEmpty())
						return null;
					this.AddGroupNode(tmp_xml,this.GroupColumns[group_col],col_val_old,r_val,i-step_start,last_xml);
					//递进
					step_start=i;
					col_val_old=col_val;
				}
				if (is_end) {//如果结束
					String last_xml=this.GetherGridXml(group_col+1,step_start,End);//则递归旧节点集
					r_val=this.LastValue;
					if (!this.Error.isEmpty())
						return null;
					this.AddGroupNode(tmp_xml,this.GroupColumns[group_col],col_val_old,r_val,i-step_start+1,last_xml);
				}
				cnt_all_arr.add(r_val);
			}
		}
		this.LastValue=DoMethod(cnt_all_arr);
		
		String out_xml=tmp_xml.toString();
		if (Start==0 && !out_xml.isEmpty() && out_xml.charAt(out_xml.length()-1)==',')
			return out_xml.substring(0,out_xml.length()-1);
		else
			return out_xml;
		
	}
	
	public void AddGroupNode(StringBuffer tmp_xml,String ColName,String col_val_old,String cnt_node,Integer Num,String in_xml) {
		String node=col_val_old.replace("&","&amp;");
		String head;
		if (!in_xml.isEmpty() && in_xml.charAt(in_xml.length()-1)==',') {//进去最后的","
			in_xml=in_xml.substring(0,in_xml.length()-1);
		}
		if (this.TransType==0) {//xml 处理
			if (in_xml.length()==0) {
				//叶节点
				if (this.AddNum)
					head=(String)"<"+ColName+" V=\""+node+"\" S=\""+cnt_node+"\" N=\""+Num.toString()+"\"/>";
				else
					head=(String)"<"+ColName+" V=\""+node+"\" S=\""+cnt_node+"\"/>";
				tmp_xml.append(head);
			}else {
				//非叶节点
				if (this.AddNum)
					head=(String)"<"+ColName+" V=\""+node+"\" S=\""+cnt_node+"\" N=\""+Num.toString()+"\">"+in_xml;
				else
					head=(String)"<"+ColName+" V=\""+node+"\" S=\""+cnt_node+"\">"+in_xml;
				tmp_xml.append(head);
				String end=(String)"</"+ColName+">";
				tmp_xml.append(end);
			}
		}else if (this.TransType==1){
			if (in_xml.length()==0) {
				//叶节点
				if (this.AddNum)
					head=(String)"{\""+title[0]+"\":\""+ColName+"\",\""+title[1]+"\":\""+node+"\",\""+title[2]+"\":\""+cnt_node+"\",\""+title[3]+"\":\""+(this.Id++)+"\",\""+title[4]+"\":\""+Num.toString()+"\","+title[6]+"},";
				else
					head=(String)"{\""+title[0]+"\":\""+ColName+"\",\""+title[1]+"\":\""+node+"\",\""+title[2]+"\":\""+cnt_node+"\",\""+title[3]+"\":\""+(this.Id++)+"\","+title[6]+"},";
				tmp_xml.append(head);
			}else {
				//非叶节点
				if (this.AddNum)
				    head=(String)"{\""+title[0]+"\":\""+ColName+"\",\""+title[1]+"\":\""+node+"\",\""+title[2]+"\":\""+cnt_node+"\",\""+title[3]+"\":\""+(this.Id++)+"\",\""+title[4]+"\":\""+Num.toString()+"\","+title[6]+",\""+title[5]+"\":["+in_xml;
				else
					head=(String)"{\""+title[0]+"\":\""+ColName+"\",\""+title[1]+"\":\""+node+"\",\""+title[2]+"\":\""+cnt_node+"\",\""+title[3]+"\":\""+(this.Id++)+"\","+title[6]+",\""+title[5]+"\":["+in_xml;
				tmp_xml.append(head);
				String end=(String)"]},";
				tmp_xml.append(end);
			}
		}
	}
	
	public String AddNodes(Map<String, Object> col_map) {
		String xml_str="";
		if (this.TransType==0) {//xml 处理
			xml_str=this.AddNodes_Attr ? "<row" : "<row>";
			for(Integer i=0; i<this.AddColumns.length; i++) {
				//取得名称和值
				String col_name=this.AddColumns[i];
				if (!col_map.containsKey(col_name)) {//不存在该字段。
					this.Error="Error: GetherGridXml->col("+col_name+") field not found!";
					return null;
				}
				String col_val=col_map.get(col_name)==null ? "" :""+col_map.get(col_name);//取得新col_val	
				//添加节点字串
				if (this.AddNodes_Attr) {
					col_val=col_val.replace("&","&amp;");
					xml_str+=" "+col_name+"="+"\""+col_val+"\"";
				}else 
					xml_str+="<"+col_name+"><![CDATA["+col_val+"]]></"+col_name+">";
			}
			xml_str+=this.AddNodes_Attr ? "/>" : "</row>";
		}else if (this.TransType==1) {//json处理
			xml_str="{";
			for(Integer i=0; i<this.AddColumns.length; i++) {
				//取得名称和值
				String col_name=this.AddColumns[i];
				if (!col_map.containsKey(col_name)) {//不存在该字段。
					this.Error="Error: GetherGridXml->col("+col_name+") field not found!";
					return null;
				}
				String col_val=col_map.get(col_name)==null ? "" :""+col_map.get(col_name);//取得新col_val	
				//添加节点字串
				col_val=col_val.replace("&","&amp;");
				if (i==(this.AddColumns.length-1)) {
					if (title[6]!=null && !title[6].isEmpty()) 
						xml_str+="\""+col_name+"\":"+"\""+col_val+"\","+title[6];	
					else
						xml_str+="\""+col_name+"\":"+"\""+col_val+"\"";	
				}else
					xml_str+="\""+col_name+"\":"+"\""+col_val+"\",";
			}
			xml_str+="},";
		}
		
		return xml_str;
	}
	
	public String DoMethod(List<String> cnts) {
		String result="";
		if (this.CntMethod.equals("Append")) {
			for(int i=0; i<cnts.size(); i++) {
				if (i<(cnts.size()-1))
					result+=(String)cnts.get(i)+";";
				else
					result+=cnts.get(i);
			}
		}
		if (this.CntMethod.equals("Num")) {
			Integer num=cnts.size();
			result=num.toString();
		}
		return result;
	}
	
	public String GetRecColValue(Map<String, Object> col_map, Integer group_col) {
		String col_name=this.GroupColumns[group_col];
		if (!col_map.containsKey(col_name)) {//不存在该字段。
			this.Error="Error: GetherGridXml->col("+col_name+") field not found!";
			return null;
		}
		String col_val=""+col_map.get(col_name);//取得新col_val
		return col_val;
	}
}
