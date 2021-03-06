package com.dataiku.dip.hadoop;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.dataiku.dctc.HadoopDistributionClassLoader;

public class HadoopUtils {
    public static String getHadoopHome() {
        String hadoopHome = System.getenv("HADOOP_HOME");
        if (hadoopHome == null) {
            hadoopHome = System.getenv("HADOOP_PREFIX");
        }
        if (hadoopHome == null) {
            hadoopHome = HadoopDistributionClassLoader.guessHadoopHome();
        }
        if (hadoopHome == null) {
            throw new IllegalArgumentException("Missing HADOOP_HOME or HADOOP_PREFIX environment variable");
        }
        if (!hadoopHome.endsWith("/"))  {
            hadoopHome += "/";
        }
        return hadoopHome;
    }
    
    public static String getCoreSiteLocation() {
        return getHadoopHome() + "conf/core-site.xml";
    }
    
    public static FileSystem getFS() throws IOException{
        Configuration conf = new Configuration();
        conf.addResource(new Path(HadoopUtils.getCoreSiteLocation()));
        return FileSystem.get(conf);
    }
    
    public static ClassLoader getConfigClassLoader() throws Exception {
        URL url = new URL("file:///" + getHadoopHome() + "conf/");
        System.out.println("LOADING " + url);
        URLClassLoader ucl =  new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader());
        return ucl;
    }
}
