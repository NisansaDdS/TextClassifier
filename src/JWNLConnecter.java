/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import net.didion.jwnl.JWNL;
/**
 *
 * @author semap
 */
public class JWNLConnecter {

    private static final String WORDNET_PROPERTY = "wordnet.configfile";
    private static final String JWNL_FILE_PROPERTIES_XML = "file_properties.xml";
    private static final String JWNL_DIR_PROPERTIES_XML = ".";
    
    public static boolean initializeJWNL() {
        try {
            JWNL.initialize(
                    getJWNLConfigFileStream(
                    WORDNET_PROPERTY,
                    JWNL_FILE_PROPERTIES_XML,
                    JWNL_DIR_PROPERTIES_XML));
            return true;
        } catch (Exception ex) {
            String estr = "Error: Unable to initialize JWNL: " + ex.toString() + "\n";
            Throwable th = ex.getCause();
            while (th != null) {
                estr += th.toString() + "\n";
                th = th.getCause();
            }
            System.err.println(estr);
            return false;
        }
    }

    private static InputStream getJWNLConfigFileStream(
            String propertyName,
            String file,
            String defaultDir)
            throws FileNotFoundException {
        InputStream in = null;
        String property = System.getProperty(propertyName);

        if (property != null) {
            in = new FileInputStream(property);
            if (in != null) {
                System.err.println("Info: Using file defined in "
                        + propertyName + ":" + property);
                return in;
            }
        }

        String defaultFile = defaultDir + "/" + file;
        in = new FileInputStream(defaultFile);
        if (in != null) {
            System.err.println("Info: Using default " + defaultFile);
            return in;
        }
        throw new RuntimeException("Error loading " + file + " file.");
    }

}
