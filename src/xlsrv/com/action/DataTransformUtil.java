package xlsrv.com.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单数据转换easyui Tree数据格式工具类.
 * Created by xufy on 2017/12/19.
 */

public class DataTransformUtil {
    public static String DoTrans(String sb,String[] paras){
        List<Map<String,Object>> sbList = JSON.parseObject(sb.toString(),new TypeReference<List<Map<String,Object>>>() {});
        List<Tree> result;
        if (paras==null)
        		result = transformToEasyuiTree("id","name","pId","-1","open",false,sbList);
        else {
        		result = transformToEasyuiTree(paras[0],paras[1],paras[2],paras[3],paras[4],(paras.length>5 && paras[5]=="true") ? true : false ,sbList);
        }
        
        //System.out.print(JSON.toJSONString(result));
        
        return JSON.toJSONString(result);
    }
    
    /*
    * 转换方法.
    */
    public static List<Tree> transformToEasyuiTree(String id,String text,String pid,String pidValue,
                       String state,Boolean checked,List<Map<String,Object>> paramList){
        if (0 >= paramList.size()) {
            return null;
        }
        List<Tree> rootList = new ArrayList<Tree>();
        Tree root = null;
        for (Map<String,Object> param:paramList) {
            if (pidValue.equals(param.get(pid))) {
                root = mapToTree(id,text,state,checked,param);
                rootList.add(root);
            }
        }
        if (0 < rootList.size()) {
            for (Tree tree:rootList) {
                addChildren(tree,id,text,pid,state,checked,paramList);
            }
        }

        return rootList;
    }
    /*
    * 为每个节点添加子节点.
    */
    public static Tree addChildren(Tree root,String id,String text,String pid,String state,Boolean checked,List<Map<String,Object>> paramList) {
        for (Map<String,Object> param:paramList) {
            if (root.id.equals(param.get(pid))){
                List<Tree> children = root.getChildren();
                children.add(mapToTree(id,text,state,checked,param));
            }
        }
        if (0 < root.getChildren().size()) {
            for (int i = 0; i < root.getChildren().size(); i++) {
                addChildren(root.getChildren().get(i),id,text,pid,state,checked,paramList);
            }
        }
        return root;
    }
    /*
    * map转换成tree.
    *
    */
    public static Tree mapToTree(String id,String text,String state,Boolean checked,Map<String,Object> map) {
        if (null == map) {
            return null;
        }
        Tree tree = new Tree();
        Map<String,Object> attrMap = new HashMap<String,Object>();
        for (Map.Entry<String,Object> set:map.entrySet()) {
            if (id.equals(set.getKey())) {
                tree.setId((String)set.getValue());
            } else if (text.equals(set.getKey())) {
                tree.setText((String)set.getValue());
            } else {
                attrMap.put(set.getKey(),set.getValue());
            }
        }
        if (state != null && state != "") {
            tree.setState(state);
        }
        tree.setAttributes(attrMap);
        tree.setChecked(checked);
        tree.setChildren(new ArrayList<Tree>());
        return tree;
    }
    /*
    * 内部类.
    */
    public static class Tree{
        private String id;
        private String text;
        private String state;
        private Boolean checked;
        private Map<String,Object> attributes;
        private List<Tree> children;
        public Tree(){
            state = "open";
            checked = false;
        }
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Boolean getChecked() {
            return checked;
        }

        public void setChecked(Boolean checked) {
            this.checked = checked;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public List<Tree> getChildren() {
            return children;
        }

        public void setChildren(List<Tree> children) {
            this.children = children;
        }
    }
}
