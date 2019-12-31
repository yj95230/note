package com.aisino.fileUp.service;

import com.aisino.config.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: yj
 * @Date: 2019/12/2 0002 10:39
 */
@Slf4j
@Service
public class UpAndDownService {

    @Value("${fileTempPath}")
    private String fileTempPath;

    @Value("${fileSavePath}")
    private String fileSavePath;

    @Autowired
    Properties properties;


    private static DateTimeFormatter dateTimeFormatter=DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    public Map<String,Object> reciveFile(HttpServletRequest request){
        Map<String,Object> result=new HashMap<>();
        int status=0;
        String msg="获取上传文件错误";
        try {
            status=1;
            msg="上传文件成功！";
        } catch (Exception e) {
            log.error("error："+e.getMessage());
            e.printStackTrace();
        }
        result.put("status",status);
        result.put("msg",msg);
        return result;
    }

    /**
     * 长传文件
     * @param request
     * @param files 上传的文件
     * @param filePath 存放路径
     * @return
     */
    public Map<String,Object> upFile(HttpServletRequest  request,MultipartFile[] files, String filePath){
        Map<String,Object>resultMap=new HashMap<>();
        CommonsMultipartResolver multipartResolver=new CommonsMultipartResolver(request.getSession().getServletContext());
        boolean flag=false;
        String message="上传错误";
        if(multipartResolver.isMultipart(request)) {
            File dirFile = new File(filePath+File.separator+"upload"+File.separator+getFilePackName());
            dirFile.mkdirs();
            for (MultipartFile file : files) {
                File targetFile = new File(dirFile+File.separator+file.getOriginalFilename());
                if (!targetFile.exists()) {
                    try {
                        targetFile.createNewFile();
                    } catch (IOException e) {
                        message="创建文件错误，createNewFile";
                        log.error(dateTimeFormatter + "：服务器端创建文件，" + file.getOriginalFilename() + "失败---createNewFile");
                        e.printStackTrace();
                    }
                    try {
                        file.transferTo(targetFile);
                        flag = true;
                        message="上传成功";
                    } catch (IOException e) {
                        message="创建文件错误，transferTo";
                        log.error(dateTimeFormatter + "：服务器端创建文件，" + file.getOriginalFilename() + "失败---transferTo");
                        e.printStackTrace();
                    }
                }else{
                    message="上传文件，"+file.getOriginalFilename()+"已经存在！";
                }
            }
        }
        resultMap.put("flag",flag);
        resultMap.put("message",message);
        return resultMap;
    }

    /**
     * 上传文件
     * @param request
     * @param filePath 文件路径
     * @return
     */
    public Map<String,Object> upFile(HttpServletRequest  request,String filePath){
        Map<String,Object>resultMap=new HashMap<>();
        CommonsMultipartResolver multipartResolver=new CommonsMultipartResolver(request.getSession().getServletContext());
        boolean flag=false;
        String message="上传错误";
        if(multipartResolver.isMultipart(request)) {
            MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
            Map<String, MultipartFile> multipartFileMap = multipartHttpServletRequest.getFileMap();
            if (MapUtils.isNotEmpty(multipartFileMap)) {
                File dirFile = new File(filePath+File.separator+getFilePackName());
                dirFile.mkdirs();
                StringBuilder sb=new StringBuilder("{");
                int i=0;
                for (MultipartFile file : multipartFileMap.values()) {
                    File targetFile = new File(dirFile+File.separator+file.getOriginalFilename());
                    if (!targetFile.exists()) {
                        try {
                            targetFile.createNewFile();
                        } catch (IOException e) {
                            message="创建文件错误，createNewFile";
                            log.error(dateTimeFormatter + "：服务器端创建文件，" + file.getOriginalFilename() + "失败---createNewFile");
                            e.printStackTrace();
                        }
                        try {
                            file.transferTo(targetFile);
                            flag = true;
                            message="上传成功";
                        } catch (IOException e) {
                            message="创建文件错误，transferTo";
                            log.error(dateTimeFormatter + "：服务器端创建文件，" + file.getOriginalFilename() + "失败---transferTo");
                            e.printStackTrace();
                        }
                    }else{
                        if(i== multipartFileMap.values().size()-1){
                            sb.append(file.getOriginalFilename()+"}");
                            message="上传文件，"+sb.toString()+"已经存在！";
                        }else{
                            sb.append(file.getOriginalFilename()+",");
                            i++;
                        }
                    }
                }
            }
        }
        resultMap.put("flag",flag);
        resultMap.put("message",message);
        return resultMap;
    }

    /**
     * servlet的接收文件上传（使用commons-fileupload）
     * @param request
     * @param fileTempPath 文件的临时存放路径
     * @param fileSavePath 文件存放路径，父目录
     * @param maxSize 允许文件的最大容量
     * @return
     * @throws Exception
     */
    private boolean saveFile(HttpServletRequest request,String fileTempPath,String fileSavePath,int maxSize) throws Exception {
        DiskFileItemFactory factory=new DiskFileItemFactory();
        //当超过该大小时，文件存储为临时文件
        factory.setSizeThreshold(properties.getDEFAULT_SIZE_THRESHOLD());
        //临时路径
        factory.setRepository(new File(fileTempPath));

        ServletFileUpload upload=new ServletFileUpload(factory);
        //设置最大容量
        upload.setFileSizeMax(maxSize);
//        upload.

        List<FileItem> fileItemList=upload.parseRequest(request);

        File file=new File(fileSavePath+File.separator+getFilePackName());
        file.mkdirs();

        for (FileItem item : fileItemList) {
            if (!item.isFormField()) {
                String fileName=item.getName();
                long size=item.getSize();
                if(fileName==null || "".equals(fileName) || size==0){
                    continue;
                }
                item.write(new File(fileSavePath+File.separator+getFilePackName()+File.separator+fileName));
            }
        }
        return true;
    }

    private String getFilePackName(){
        return LocalDate.now().format(dateTimeFormatter);
    }
}
