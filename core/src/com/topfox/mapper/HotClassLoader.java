//package com.topfox.mapper;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.nio.ByteBuffer;
//import java.nio.channels.Channels;
//import java.nio.channels.FileChannel;
//import java.nio.channels.WritableByteChannel;
//
//public class HotClassLoader extends ClassLoader {
//    private File fileClass;
//    private static Logger logger = LoggerFactory.getLogger("com.topfox.data.HotClassLoader");
//
//    public HotClassLoader(File objFile) {
//        super(ClassLoader.getSystemClassLoader());
//
//        this.fileClass = objFile;
//    }
//
//    @Override
//    protected Class<?> findClass(String name) throws ClassNotFoundException {
//        //这个classLoader的主要方法
//        Class clazz = null;
//        try {
//            byte[] data = getClassFileBytes();
//            //这个方法非常重要
//            clazz = defineClass(name, data, 0, data.length);
//            if (null == clazz) {
//                //如果在这个类加载器中都不能找到这个类的话，就真的找不到了
//            }
//        } catch (Exception e) {
//            logger.warn(e.getMessage());
//            //e.printStackTrace();
//        }
//        return clazz;
//
//    }
//
//    /**
//     * 把CLASS文件转成BYTE
//     *
//     * @throws Exception
//     */
//    private byte[] getClassFileBytes() throws Exception {
//        //采用NIO读取
//        FileInputStream fis = new FileInputStream(fileClass);
//        FileChannel fileC = fis.getChannel();
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        WritableByteChannel outC = Channels.newChannel(baos);
//        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
//        while (true) {
//            int i = fileC.read(buffer);
//            if (i == 0 || i == -1) {
//                break;
//            }
//            buffer.flip();
//            outC.write(buffer);
//            buffer.clear();
//        }
//        fis.close();
//        return baos.toByteArray();
//    }
//}
