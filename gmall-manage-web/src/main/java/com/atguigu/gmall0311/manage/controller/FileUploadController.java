package com.atguigu.gmall0311.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${fileServer.url}")
    private String pathUrl; // pathUrl=http://192.168.67.220

    // springMVC
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String imgUrl = pathUrl;
        if (file!=null){
            // 项目目录不能有空格等非法字符！
            String configFile  = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient=new TrackerClient();
            TrackerServer trackerServer=trackerClient.getConnection();
            StorageClient storageClient=new StorageClient(trackerServer,null);

            // String orginalFilename="d://img//zly.jpg";
            String originalFilename = file.getOriginalFilename(); // zly.jpg
            // 后缀名
            String extName = StringUtils.substringAfterLast(originalFilename,".");
            System.out.println(extName+"后缀名");
            // /usr/bin/fdfs_test /etc/fdfs/fdfs_client.conf upload /root/001.jpg
            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                imgUrl+="/"+path;
                // http://192.168.67.220/group1/M00/00/00/wKhD3F1aSReAKLVTAACGx2c4tJ4142.jpg
                /*
                s = group1
                s = M00/00/00/wKhD3F1aSReAKLVTAACGx2c4tJ4142.jpg
                 */

            }
        }
        // http://192.168.67.220/group1/M00/00/00/wKhD3F1aSReAKLVTAACGx2c4tJ4142.jpg

        return imgUrl;
    }
}
