package com.xuecheng.mange_media;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class TestFile {

    //文件分块
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("e:/lucene.mp4");
        String chunkPath = "E:\\xc_dev\\ffmpeg\\chunk\\";
        //创建分块文件夹
        File chunkFolder = new File(chunkPath);
        if (!chunkFolder.exists()) {
            chunkFolder.mkdirs();
        }

        //分块大小
        long chunkSize = 1024*1024*1;
        //分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0 / chunkSize);
        if (chunkNum<=0) {
            chunkNum = 1;
        }

        //缓冲区大小
        byte[] b = new byte[1023];
        //是使用randomAccessFile访问文件
        RandomAccessFile read = new RandomAccessFile(sourceFile, "r");
        //分块
        for (int i = 0; i<chunkNum; i++) {
            //创建分块文件
            File file = new File(chunkPath+i);
            boolean newFile = file.createNewFile();
            if (newFile) {
                //向分块文件写数据
                RandomAccessFile write = new RandomAccessFile(file, "rw");
                int len = -1;
                while ((len = read.read(b))!=-1) {
                    write.write(b, 0, len);
                    if (file.length()>chunkSize) {
                        break;
                    }
                }
                write.close();
            }
        }
        read.close();
    }

    //文件合并
    @Test
    public void mergeFile() throws IOException {
        String chunkPath = "E:\\xc_dev\\ffmpeg\\chunk\\";
        File chunkFolder = new File(chunkPath);
        File mergerFile = new File("E:\\xc_dev\\ffmpeg\\lucene.mp4");
        if (mergerFile.exists()) {
            mergerFile.delete();
        }
        mergerFile.createNewFile();

        //writer
        RandomAccessFile writer = new RandomAccessFile(mergerFile, "rw");
        //指针指向文件顶端
        writer.seek(0);

        byte[] b = new byte[1023];
        //分块列表
        File[] files = chunkFolder.listFiles();
        List<File> fileList = new ArrayList<File>(Arrays.asList(files));
        //块文件排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {

                if (Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())) {
                    return 1;
                }
                return -1;
            }
        });

        for (File chunkFile: fileList) {
            RandomAccessFile read = new RandomAccessFile(chunkFile,"rw");
            int len = -1;
            while((len = read.read(b))!=-1){
                writer.write(b, 0, len);
            }
            read.close();
        }
        writer.close();
    }
}
