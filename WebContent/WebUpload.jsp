<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
        <title>使用webuploader上传</title>
        <!-- 1.引入文件 -->
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath }/js/webuploader/0.1.5/webuploader.css" rel="external nofollow" >
        <script type="text/javascript" src="${pageContext.request.contextPath }/js/jquery/jquery3.3.1-min.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath }/js/webuploader/0.1.5/webuploader.js"></script>
        </head>
        <body>
         <!-- 2.创建页面元素 -->
         <div id="upload">
          <div id="filePicker">文件上传</div>
         </div>
         <div id="fileList"></div>
         <style type="text/css">
	      #dndArea {
	            width: 200px;
	            height: 100px;
	            border-color: red;
	            border-style: dashed;
	        }
	    </style>        
	    <!-- 创建用于拖拽的区域 -->
	    <div id="dndArea"></div>
         <!-- 3.添加js代码 -->
         <script type="text/javascript">
          var uploader = WebUploader.create(
           {
            swf:"${pageContext.request.contextPath }/js/webuploader/0.1.5/Uploader.swf",
            server:"${pageContext.request.contextPath }/FileUploadServlet",
            pick:"#filePicker",
            auto:true,
         // 开启拖拽
            dnd:"#dndArea",
            // 屏蔽拖拽区域外的响应
            disableGlobalDnd:true
           });
          
       	  // 生成缩略图和上传进度
          uploader.on("fileQueued", function(file) {
                  // 把文件信息追加到fileList的div中
                  $("#fileList").append("<div id='" + file.id + "'><img/><span>" + file.name + "</span><div><span class='percentage'><span></div></div>")

                  // 制作缩略图
                  // error：不是图片，则有error
                  // src:代表生成缩略图的地址
                  uploader.makeThumb(file, function(error, src) {
                      if (error) {
                          $("#" + file.id).find("img").replaceWith("<span>无法预览&nbsp;</span>");
                      } else {
                          $("#" + file.id).find("img").attr("src", src);
                      }
                  });
              }
          );

          // 监控上传进度
          // percentage:代表上传文件的百分比
          uploader.on("uploadProgress", function(file, percentage) {
              $("#" + file.id).find("span.percentage").text(Math.round(percentage * 100) + "%");
          });
         </script>
        </body>
</html>
