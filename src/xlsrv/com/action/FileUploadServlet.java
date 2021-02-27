package xlsrv.com.action;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

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
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private String serverPath = "/";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.getWriter().append("Served at: ").append(request.getContextPath());

        System.out.println("进入后台...");
        //this.serverPath=WebService.class.getClassLoader().getResource("").getPath();
        this.serverPath=request.getSession().getServletContext().getRealPath("/");
        String dest_dir=request.getParameter("dest_dir")!=null ? request.getParameter("dest_dir") : "FileUpload123";
        String random_str=request.getParameter("random")!=null ? request.getParameter("random") : "";
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
                    //String add_random=random_str.equals("true") ? this.getRandomString(8)+"_" : "";
                    String ext=name.indexOf(".")>=0 ? name.substring(name.indexOf(".")) :"";
                    String file_name=random_str.equals("true") ? this.getRandomString(8)+ext : name;
                    FileUtils.copyInputStreamToFile(is, new File(serverPath + dest_dir+"/"+ file_name));
                    //FileUtils.copyInputStreamToFile(is, new File(serverPath + dest_dir+"/"+ add_random+name));
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
    
    public String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
          int number=random.nextInt(62);
          sb.append(str.charAt(number));
        }
        return sb.toString();
    }

}
