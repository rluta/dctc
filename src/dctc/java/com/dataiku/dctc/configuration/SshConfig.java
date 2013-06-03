package com.dataiku.dctc.configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import static com.dataiku.dip.output.PrettyString.scat;
import static com.dataiku.dip.output.PrettyString.pquoted;

import com.dataiku.dctc.file.FileManipulation;

public class SshConfig {
    public void parse(String path) throws IOException {
        File f = new File(path);
        parse(f);
    }
    public void parse(File file) throws IOException {
        BufferedReader stream =  new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        String currentHost = null;
        Map<String, String> hostParam = null;
        while ((line = stream.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("Host") && !line.startsWith("HostName")) {
                currentHost = line.substring(4).trim();
                hostParam  = new HashMap<String, String>();
                config.put(currentHost, hostParam);
            }
            else {
                if (currentHost == null) {
                    throw new IOException(scat("dctc ssh config:",
                                               "in",
                                               pquoted(file.getAbsolutePath()),
                                               "Parameter defined before any section."));
                }
                assert currentHost != null : "currentHost != null";
                String[/* param/value */] parameter = FileManipulation.split(line, " ", 2, false);
                if (parameter[1] == null) {
                    parameter = FileManipulation.split(line, "	", 2, false);
                }
                if (parameter[1] == null) {
                    throw new IOException(scat("dctc ssh config:",
                                               "The parameter",
                                               pquoted(parameter[0]),
                                               "doesn't define any value."));
                }
                hostParam.put(parameter[0], parameter[1]);
            }
        }
    }

    // Getters
    public boolean exists(String host) {
        return config.get(host) != null;
    }
    public Map<String, String> getHostParam(String host) {
        return config.get(host);
    }

    // private
    private Map<String, Map<String, String>> config = new HashMap<String, Map<String, String>>();
    //          Host        Param   Value

}
