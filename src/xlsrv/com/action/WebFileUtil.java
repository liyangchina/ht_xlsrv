package xlsrv.com.action;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;

/**
 * Servlet user to accept file upload
 */
public class WebFileUtil extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private String serverPath = "/";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());

        System.out.println("进入后台...");
        this.serverPath=WebService.class.getClassLoader().getResource("").getPath();;

        // 1.创建DiskFileItemFactory对象，配置缓存用
        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();

        // 2. 创建 ServletFileUpload对象
        ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);

        // 3. 设置文件名称编码
        servletFileUpload.setHeaderEncoding("utf-8");

        // 4. 开始解析文件
        try {
            List<FileItem> items = servletFileUpload.parseRequest(request);
            for (FileItem fileItem : items) {

                if (fileItem.isFormField()) { // >> 普通数据
                    String info = fileItem.getString("utf-8");
                    System.out.println("info:" + info);
                } else { // >> 文件
                    // 1. 获取文件名称
                    String name = fileItem.getName();
                    // 2. 获取文件的实际内容
                    InputStream is = fileItem.getInputStream();

                    // 3. 保存文件
                    FileUtils.copyInputStreamToFile(is, new File(serverPath + "/FileUpload/" + name));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
    
    /**
	 * 创建文件夹
	 * 
	 * @param path
	 *            目录
	 */
	public void createDir(String path) {
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdir();
			System.out.println("创建文件夹成功！");
		}
	}

	/**
	 * 递归删除文件夹 要利用File类的delete()方法删除目录时， 必须保证该目录下没有文件或者子目录，否则删除失败， 因此在实际应用中，我们要删除目录，
	 * 必须利用递归删除该目录下的所有子目录和文件， 然后再删除该目录。
	 * 
	 * @param path
	 */
	public void delDir(String path) {
		File dir = new File(path);
		if (dir.exists()) {
			File[] tmp = dir.listFiles();
			for (int i = 0; i < tmp.length; i++) {
				if (tmp[i].isDirectory()) {
					delDir(path + "/" + tmp[i].getName());
				} else {
					tmp[i].delete();
				}
			}
			dir.delete();
		}
	}

	/*
	 * 转移文件目录不等同于复制文件，复制文件是复制后两个目录都存在该文件， 而转移文件目录则是转移后，只有新目录中存在该文件
	 */
	/**
	 * 转移文件目录
	 * 
	 * @param filename
	 *            文件名
	 * @param oldpath
	 *            旧目录
	 * @param newpath
	 *            新目录
	 * @param cover
	 *            若新目录下存在和转移文件具有相同文件名的文件时，是否覆盖新目录下文件， cover=true将会覆盖原文件，否则不操作
	 */
	public void changeDirectory(String filename, String oldpath, String newpath, boolean cover) {
		if (!oldpath.equals(newpath)) {
			File oldfile = new File(oldpath + "/" + filename);
			File newfile = new File(newpath + "/" + filename);
			if (newfile.exists()) {// 若在待转移目录下，已经存在待转移文件
				if (cover)// 覆盖
					oldfile.renameTo(newfile);
				else
					System.out.println("在新目录下已经存在：" + filename);
			} else {
				oldfile.renameTo(newfile);
			}
		}
	}

	/**
	 * 创建新文件
	 * 
	 * @param path
	 *            目录
	 * @param filename
	 *            文件名
	 * @throws IOException
	 */
	public void createFile(String path, String filename) throws IOException {
		File file = new File(path + "/" + filename);
		if (!file.exists()) {
			file.createNewFile();
			System.out.println("创建文件成功！");
		}
	}

	/*
	 * 利用BufferedWriter写入文件内容
	 * 
	 * 利用Buffer操作IO速度会稍微快一点
	 */
	public void writeFile(String filename) {
		File file = new File(filename);
		try {
			if (!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file, true);// 传入true表示如果该文件存在，则将新内容添加到文件末尾
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = 0; i < 10; i++)
				bw.write("这是第" + (i + 1) + "行，应该没错哈 ");
			// 关闭
			bw.close();
			bw = null;
			fw.close();
			fw = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 读文件 利用BufferedReader读取在IO操作，利用BufferedReader和BufferedWriter效率会更高一点
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public String ReadFile(String path) throws IOException {
		File file = new File(path);
		if (!file.exists() || file.isDirectory())
			throw new FileNotFoundException();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String temp = null;
		StringBuffer sb = new StringBuffer();
		temp = br.readLine();
		while (temp != null) {
			sb.append(temp + " ");
			temp = br.readLine();
		}
		return sb.toString();
	}

	/**
	 * 文件重命名--如果重命名的目标文件已经存在,则不会进行任何操作
	 * 
	 * @param path
	 *            文件目录
	 * @param oldname
	 *            原来的文件名
	 * @param newname
	 *            新文件名
	 */
	public void renameFile(String path, String oldname, String newname) {
		if (!oldname.equals(newname)) {// 新的文件名和以前文件名不同时,才有必要进行重命名
			File oldfile = new File(path + "/" + oldname);
			File newfile = new File(path + "/" + newname);
			if (newfile.exists())// 若在该目录下已经有一个文件和新文件名相同，则不允许重命名
				System.out.println(newname + "已经存在！");
			else {
				oldfile.renameTo(newfile);
			}
		}
	}

	/**
	 * 以文件流的方式复制文件 支持中文处理，并且可以复制多种类型，比如txt，xml，jpg，doc等多种格式
	 * 
	 * @param src
	 *            文件源目录
	 * @param dest
	 *            文件目的目录
	 * @throws IOException
	 */
	public void copyFile(String src, String dest) throws IOException {
		FileInputStream in = new FileInputStream(src);
		File file = new File(dest);
		if (!file.exists())
			file.createNewFile();
		FileOutputStream out = new FileOutputStream(file);
		int c;
		byte buffer[] = new byte[1024];
		while ((c = in.read(buffer)) != -1) {
			for (int i = 0; i < c; i++)
				out.write(buffer[i]);
		}
		System.out.println("文件拷贝完毕！");
		in.close();
		out.close();
	}

	/**
	 * 删除文件
	 * 
	 * @param path
	 *            目录
	 * @param filename
	 *            文件名
	 */
	public void delFile(String path, String filename) {
		File file = new File(path + "/" + filename);
		if (file.exists() && file.isFile())
			file.delete();
	}

	public String readSysFile(String path, String charsetName1, String charsetName2) {
		BufferedReader br = null;
		StringBuffer sb = new StringBuffer();
		String str = "";
		try {
			InputStream is = new FileInputStream(WebFileUtil.class.getResource(path).toString().substring(6));
			br = new BufferedReader(new InputStreamReader(is));
			String temp = null;
			temp = br.readLine();
			while (temp != null) {
				sb.append(temp + "\n");
				temp = br.readLine();
			}
			str = sb.toString();
			// System.out.println(sb.toString());
			if (charsetName1 != null && charsetName2 != null && charsetName1.trim().length() > 0
					&& charsetName1.trim().length() > 0) {
				// str = new String(str.getBytes("GBK"),"UTF-8");
				str = new String(str.getBytes(charsetName1.trim()), charsetName2.trim());
			}
			is.close();
			br.close();
			sb.setLength(0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str;
	}

//	public static void main(String[] arg) {
//
//		String str;
//		try {
//			str = FileUtil.class.newInstance().readSysFile("/com/jinqiao/tag/file/orgLinkage.pro", "", "");
//			System.out.println(str);
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	
	/**
     * 递归删除文件或者目录
     * @param file_path
     */
    public static void deleteEveryThing(String file_path) {
    try{
        File file=new File(file_path);
        if(!file.exists()){
            return ;
        }
        if(file.isFile()){
            file.delete();
        }else{
            File[] files = file.listFiles();
            for(int i=0;i<files.length;i++){
                String root=files[i].getAbsolutePath();//得到子文件或文件夹的绝对路径
                deleteEveryThing(root);
            }
            file.delete();
        }
    } catch(Exception e) {
        	System.out.println("删除文件失败");
    		}
    }
    
    /*
     * 得到一个文件夹下所有文件
     */
    public static List<String> getAllFileNameInFold(String fold_path) {
        List<String> file_paths = new ArrayList<String>();
        
        LinkedList<String> folderList = new LinkedList<String>();   
        folderList.add(fold_path);   
        while (folderList.size() > 0) {   
            File file = new File(folderList.peekLast());   
            folderList.removeLast();   
            File[] files = file.listFiles();   
            ArrayList<File> fileList = new ArrayList<File>();   
            for (int i = 0; i < files.length; i++) {   
                if (files[i].isDirectory()) {   
                    folderList.add(files[i].getPath());   
                } else {   
                    fileList.add(files[i]);   
                }   
            }   
            for (File f : fileList) {   
                file_paths.add(f.getAbsoluteFile().getPath());   
            }   
        }   
        return file_paths;
    }

}
