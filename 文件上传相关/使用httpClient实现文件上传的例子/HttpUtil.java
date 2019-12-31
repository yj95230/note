package com.aisino.util;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.client.MultipartBodyBuilder;

import javax.net.ssl.*;
import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpUtil {

    private static PoolingHttpClientConnectionManager poolConnManager;
    private static RequestConfig requestConfig;
    private static HttpRequestRetryHandler httpRequestRetryHandler;
    //最大连接数
    private static final int maxTotalPool = 200;
    //设置到某个路由的最大连接数
    private static final int maxConPerRoute = 20;
    //数据读取时间
    private static final int socketTimeout = 6000;
    //连接超时时间
    private static final int connectionRequestTimeout = 3000;
    //从池中获取连接超时时间
    private static final int connectTimeout = 1000;

    //日期formatter
    private static DateTimeFormatter dateTimeFormatter=DateTimeFormatter.ofPattern("yyyy-mm-dd HH:mm:ss");

    //设置重试策略
    private static void setHttpRequestRetryHandler(){
        httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                System.out.println("重试次数："+executionCount);
                if (executionCount >= 2) {// 如果已经重试了4次，就放弃
                    System.out.println("重试了几次");
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// io读取被打断异常，不重连
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// ssl异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * 设置requestConfig
     * @param connectTimeout 从连接池中获取连接超时时间
     * @param socketTimeout 数据读取超时时间
     * @param connectionRequestTimeout 连接超时时间
     */
    private static void setRequestConfig(int connectTimeout,int socketTimeout,int connectionRequestTimeout){
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)         //连接超时时间
                .setSocketTimeout(socketTimeout)          //读取数据超时时间（等待数据超时时间）
                .setConnectionRequestTimeout(connectionRequestTimeout)    //从池中获取连接超时时间
                .build();
    }

    /**
     * 创建httpClient
     *
     * @param maxTotalPool 连接池中的连接数量
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    private static CloseableHttpClient getHttpClient(int maxTotalPool) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        //给重试策略赋值
        setHttpRequestRetryHandler();

        //https 请求忽略证书
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);

        Registry registry = RegistryBuilder.create().register("http", new PlainConnectionSocketFactory())
                .register("https", sslConnectionSocketFactory)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(maxTotalPool);//设置池中的里连接数
        setHttpRequestRetryHandler();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)//绕过ssl
                .setConnectionManager(cm)//设置http和https
                .setRetryHandler(httpRequestRetryHandler) //设置重试策略
                .build();
        return httpClient;
    }

    /**
     * 创建HttpPost
     * @param url 地址
     * @param connectTimeout 从连接池中获取连接超时时间
     * @param socketTimeout 数据读取超时时间
     * @param connectionRequestTimeout 连接超时时间
     */
    private static HttpPost getHttpPost(String url,int connectTimeout,int socketTimeout,int connectionRequestTimeout){
        //给requestConfig赋值
        setRequestConfig(connectTimeout,socketTimeout,connectionRequestTimeout);

        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);
        return httpPost;
    }

    /**
     * 设置默认的
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    private static void initDefault() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        //采用绕过验证的方式处理https请求
        //SSLContext sslcontext = createIgnoreVerifySSL();
        //https 请求忽略证书
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);
////        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
////        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
////                sslcontext,hostnameVerifier);
////        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
////                .register("http", PlainConnectionSocketFactory.getSocketFactory())
////                .register("https", sslsf)
////                .build();
        //注册http和https服务
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslConnectionSocketFactory)
                .build();

        poolConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // 最大连接数
        poolConnManager.setMaxTotal(maxTotalPool);
        // 到某个路由的最大连接数
        poolConnManager.setDefaultMaxPerRoute(maxConPerRoute);

        //设置默认的请求设置
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionRequestTimeout)         //连接超时时间
                .setSocketTimeout(socketTimeout)          //读取数据超时时间（等待数据超时时间）
                .setConnectionRequestTimeout(connectTimeout)    //从池中获取连接超时时间
                .build();

        //设置重试策略
        setHttpRequestRetryHandler();
    }

    /**
     * 创建httpClient 使用默认方式 和上面的initDefault连起用
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    private static CloseableHttpClient getDefaultHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        initDefault();
        CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(poolConnManager)             //设置连接管理器
                .setRetryHandler(httpRequestRetryHandler)          //设置重试策略
                .build();
        return httpclient;
    }

    /**
     * 使用post发送请求 content-type：text/xml
     * @param url
     * @param xmlStr
     * @return 字符串的请求结果
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    public static String requestPost(String url, String xmlStr) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        CloseableHttpClient httpClient = getDefaultHttpClient();
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "text/xml; charset=UTF-8");

        //StringBuffer responseStr = new StringBuffer();

        httpPost.setEntity(new StringEntity(xmlStr, "utf-8"));
        httpPost.setConfig(requestConfig);

        CloseableHttpResponse response=null;
        try {
            response=httpClient.execute(httpPost);//执行请求

            HttpEntity httpEntity=response.getEntity();
            return httpEntity.toString();
        } catch (IOException e) {
            System.out.println("执行报错："+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取文件并写入指定位置
     * @param url 请求地址
     * @param fileSavePath 文件保存路径
     * @return
     */
    public static boolean requestForWriteFile(String url,String fileSavePath) {
        try{
            CloseableHttpClient httpClient = getHttpClient(50);
            HttpPost httpPost = getHttpPost(url,60000,240000,30000);

            CloseableHttpResponse response=httpClient.execute(httpPost);
            if(response.getStatusLine().getStatusCode()==HttpStatus.SC_OK){
                HttpEntity httpEntity=response.getEntity();
                return writeFile(fileSavePath, httpEntity);
            }else{
                log.error("访问出错，状态码："+response.getStatusLine().getStatusCode());
            }
        }catch (Exception e){
            log.error(LocalDateTime.now().format(dateTimeFormatter)+"----请求出错："+e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    //写文件
    public static boolean writeFile(String path, HttpEntity httpEntity) {
        try{
            InputStream inputStream=httpEntity.getContent();
            ObjectMapper om = new ObjectMapper();
            //JavaType javaType=om.getTypeFactory().constructParametricType(ArrayList.class, UpFile.class);
            //List<String> fileContents = om.readValue(inputStream,List.class);

            JsonNode jsonNode=om.readTree(inputStream);
            for (JsonNode node : jsonNode) {
                if(node!=null){
                    String fileName=node.get("fileName").toString().replaceAll("\\\"","");
//                    String context = node.get("context").toString().replaceAll("\\\"","");
//                    System.out.println(context);
                    BufferedOutputStream bfo = new BufferedOutputStream(new FileOutputStream(new File(path + File.separator+fileName)));
                    bfo.write(Base64.decodeBase64(node.get("context").toString()));
                    bfo.flush();
                    bfo.close();
                }
            }
            return true;
        }catch (IOException e){
            log.error(LocalDateTime.now().format(dateTimeFormatter)+"----写入文件报错："+e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 文件上传 (单文件) 已测试成功
     * @param url 地址
     * @param fileName 文件名（全路径）
     */
    public static void fileUpOne(String url,String fileName){
        File file = new File(fileName);
        try {
            CloseableHttpClient httpClient = getHttpClient(100);
            HttpPost httpPost = getHttpPost(url, 60000, 240000, 30000);
            //定义一个边界----》每个post参数之间的分隔。随意设定，只要不会和其他的字符串重复即可。
            String boundary = "-------------------454545564654";
            //设置请求头
            httpPost.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
            //HttpEntity build
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            //字符编码
            builder.setCharset(Charset.forName("utf-8"));
            //模拟浏览器
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            //设置boundary
            builder.setBoundary(boundary);
            //相当于设置表单的 multipart/form-data
            FileBody fileBody = new FileBody(file);
            builder.addPart("multipartFile", fileBody);
            // binary---文件流
            //builder.addBinaryBody("name=\"multipartFile\"; filename=\"test.docx\"", new FileInputStream(file), ContentType.MULTIPART_FORM_DATA, fileName);
            //其他参数
            //builder.addTextBody("filename", fileBody.getFilename(), ContentType.create("text/plain", Consts.UTF_8));
            //构造httpEntity
            HttpEntity entity = builder.build();
            //设置参数
            httpPost.setEntity(entity);

            HttpResponse response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    ObjectMapper om = new ObjectMapper();
                    Map<String, Object> resultMap = om.readValue(responseEntity.getContent(), Map.class);
                    System.out.println(resultMap.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("上传文件保存，错误消息："+e.getMessage());
            e.printStackTrace();
        }

    }

    public static void fileUpMany(String url,Map<String,Object>fileNameMap,Map<String,String>keyNameMap){
        try {
            HttpClient httpClient = getHttpClient(100);
            HttpPost httpPost = getHttpPost(url,60000, 240000, 30000);

            String boundary="-------------------454545564654";
            //设置请求头
            httpPost.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
            //HttpEntity build
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            //字符编码
            builder.setCharset(Charset.forName("utf-8"));
            //模拟浏览器
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            //设置boundary
            builder.setBoundary(boundary);
            //相当于设置表单的 multipart/form-data
            for (Map.Entry<String, Object> entry : fileNameMap.entrySet()) {
                if(entry.getValue()!=null){
                    builder.addPart(entry.getKey(),new FileBody(new File(entry.getValue().toString())));
                }
            }
            //ObjectMapper om=new ObjectMapper();
            //builder.addTextBody("fileTypes", om.writeValueAsString(keyNameMap), ContentType.create("text/plain", Consts.UTF_8));
            HttpEntity httpEntity=builder.build();
            httpPost.setEntity(httpEntity);

            HttpResponse response=httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    ObjectMapper om = new ObjectMapper();
                    Map<String, Object> resultMap = om.readValue(responseEntity.getContent(), Map.class);
                    System.out.println(resultMap.get("message"));
                }
            }
        }catch(Exception e){
            log.error("上传文件保存，错误消息："+e.getMessage());
            e.printStackTrace();
        }

    }
    

    /**
     * ssl绕过验证
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");

        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        sc.init(null, new TrustManager[] { trustManager }, null);
        return sc;
    }


    public static String postXML(String url, String xml, String charset) throws Exception {
        if (url == null || StringUtils.isEmpty(url)) {
            return null;
        }

        //https 请求忽略证书
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);

        Registry registry = RegistryBuilder.create().register("http", new PlainConnectionSocketFactory())
                .register("https", sslConnectionSocketFactory)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(100);
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory)
                .setConnectionManager(cm)
                .build();
        // 创建HttpClient实例
        HttpResponse response = null;

        HttpEntity entity = null;
        StringEntity myEntity = new StringEntity(xml,charset==null?"UTF-8":charset);

        HttpPost hp = new HttpPost(url);
        //连接超时10s，数据读取时间1分钟
        hp.setConfig(RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(60000).build());
        hp.addHeader("Content-type", "text/xml");
        hp.setEntity(myEntity);
        // 发送请求，得到响应
        for(int i = 1; i < 5; i++){
            try {
                response = httpClient.execute(hp);
                break;
            } catch (Exception e){
                if(e instanceof ConnectTimeoutException){
                    System.out.println("第"+i+"次访问连接失败");
                    e.printStackTrace();
                    if(i == 4){//四次都访问失败
                        throw new RuntimeException("4次访问连接失败");
                    }
                }else{//除了连接不上的异常，其它都不重新请求
                    throw e;
                }
            }
        }
        try {
            entity = response.getEntity();
            if (response.getEntity() != null) {
                return EntityUtils.toString(response.getEntity(), Consts.UTF_8);
            }
        } catch(Exception e){
            throw e;
        }finally {
            if (entity != null)
                entity.consumeContent();
            hp.releaseConnection();
        }
        return null;
    }

}
