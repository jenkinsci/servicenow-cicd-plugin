package io.jenkins.plugins.servicenow.application;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.MessageFormat;

public class WorkspaceApplicationVersion implements ApplicationVersion {

    private static Logger LOG = LogManager.getLogger(WorkspaceApplicationVersion.class);

    private static final String PROPERTIES_FILE = "sn_source_control.properties";
    private static final String APPLICATION_HEAD_FILE = "sys_app_{sysId}.xml";
    private static final String PATH_REGEX = "^path=(.*)";
    private static final String PATH_SPLITREGEX = "^path=";
    private static final String VERSION_REGEX = "\\s*<version>(.*)</version>";
    private static final String VERSION_SPLITREGEX = "</?version>";

    public String getVersion(String workspaceDir, final String sysId, final String scope) {
        if(StringUtils.isBlank(sysId) && StringUtils.isBlank(scope)) {
            throw new IllegalArgumentException("At least one of parameters must not be empty: Sys ID or Scope of the application");
        }
        try {
            String appDir = StringUtils.isNotBlank(scope) ? scope : this.getApplicationDir(workspaceDir);
            if(StringUtils.isNotBlank(sysId)) {
                File file = new File(workspaceDir + "/" + appDir + "/" + APPLICATION_HEAD_FILE.replace("{sysId}", sysId));
                return getVersion(file);
            } else {
                File dir = new File(workspaceDir + "/" + appDir);
                File[] matchingFiles = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith("sys_app_") && name.endsWith("xml");
                    }
                });
                if(matchingFiles != null && matchingFiles.length == 1) {
                    return getVersion(matchingFiles[0]);
                }
            }
        } catch (FileNotFoundException ex) {
            LOG.warn("Application version not found for following parameters: [directory: "+workspaceDir+"," +
                    "Sys ID: "+sysId+", Scope: "+scope+"]", ex);
        }

        return StringUtils.EMPTY;
    }

    private String getVersion(File file) throws FileNotFoundException {
        return searchForRegex(file, VERSION_REGEX, VERSION_SPLITREGEX);
    }

    private String getApplicationDir(String workspaceDir) throws FileNotFoundException {
        File file = new File(workspaceDir + "/" + PROPERTIES_FILE);

        return searchForRegex(file, PATH_REGEX, PATH_SPLITREGEX);
    }

    private String searchForRegex(final File file, final String lineRegex, final String splitRegex) throws FileNotFoundException {
        final InputStream inputStream = new FileInputStream(file);
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if(line.matches(lineRegex)) {
                    return line.split(splitRegex)[1];
                }
            }
        } catch(IOException ex) {
            LOG.warn(MessageFormat.format("Not found regex `{0}` in `{1}`", lineRegex, file.getAbsolutePath()));
        }

        return StringUtils.EMPTY;
    }
}
