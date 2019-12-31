package com.aisino.fileUp.controller;

import com.aisino.fileUp.service.UpAndDownService;
import com.aisino.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传、下载
 * @Author: yj
 * @Date: 2019/11/29 0029 16:40
 */
@RestController
@Slf4j
public class UpAndDownController {



    @Autowired
    private UpAndDownService upAndDownService;


    @GetMapping("/upFileClient")
    public void upFileClient(){
        HttpUtil.fileUpOne("http://localhost:8080/fileUp","C:\\Users\\Administrator\\Desktop\\深入剖析Tomcat（中文版）.pdf");
    }

    @GetMapping("/upFileMany")
    public void upFileMany(){
        Map<String,Object>fileMap=new HashMap<>();
        fileMap.put("Hibernate5讲义","F:\\BaiduNetdiskDownload\\Hibernate5讲义.pdf");
        fileMap.put("SpringBoot使用Http请求头参数或请求参数进一步确定调用的方法","F:\\BaiduNetdiskDownload\\SpringBoot使用Http请求头参数或请求参数进一步确定调用的方法.docx");
        fileMap.put("UltraComparepro_17026","F:\\BaiduNetdiskDownload\\UltraComparepro_17026.zip");
        HttpUtil.fileUpMany("http://localhost:8080/fileUp",fileMap,null);
    }

    //https://www.cnblogs.com/fjsnail/p/3491033.html
    //https://blog.csdn.net/qqb67g8com/article/details/81334608
    @PostMapping("/fileUp")
    public Map<String,Object> up(HttpServletRequest request){

        Enumeration<String> nameEnumerations = request.getHeaderNames();
        while (nameEnumerations.hasMoreElements()){
            System.out.println(nameEnumerations.nextElement()+"："+request.getHeader(nameEnumerations.nextElement()));
        }
        return upAndDownService.upFile(request,"D:\\upFile-server");

    }

    @PostMapping("")
    public Map<String,Object> reciveFile (HttpServletRequest request){
        return upAndDownService.reciveFile(request);
    }

}
