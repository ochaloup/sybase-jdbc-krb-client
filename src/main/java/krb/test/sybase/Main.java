package krb.test.sybase;

import com.sybase.jdbc4.jdbc.SybDriver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * @author Martin Simka
 */
public class Main {

    // sybase 157
    private static final String JDBC_URL = "jdbc:sybase:Tds:db05.mw.lab.eng.bos.redhat.com:5000/krbusr01";

    public static void main(String[] args) throws Exception {

            System.setProperty("java.security.auth.login.config", new File("login.conf").getAbsolutePath());

            System.setProperty("java.security.krb5.realm", "MW.LAB.ENG.BOS.REDHAT.COM");
            System.setProperty("java.security.krb5.kdc", "kerberos-test.mw.lab.eng.bos.redhat.com");
            System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
            //System.setProperty("java.security.krb5.conf", new File("krb5.conf").getAbsolutePath());
            System.setProperty("sun.security.krb5.debug", "true");

            Properties props = new Properties();
            //props.put("user", "KRBUSR01@MW.LAB.ENG.BOS.REDHAT.COM");
            props.put("REQUEST_KERBEROS_SESSION", "true");
            props.put("SERVICE_PRINCIPAL_NAME", "DB05@MW.LAB.ENG.BOS.REDHAT.COM");

            DriverManager.registerDriver(new SybDriver());
            Connection conn = DriverManager.getConnection(JDBC_URL, props);
            printUserName(conn);

            conn.close();
    }

    private static void printUserName(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select suser_name()");
            while (rs.next())
                System.out.println("User is: " + rs.getString(1));
            rs.close();
            // get auth type -- need permission
//            rs = stmt.executeQuery("select auth_scheme from sys.dm_exec_connections where session_id=@@spid;");
//            while (rs.next())
//                System.out.println("Auth type: " + rs.getString(1));
//            rs.close();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }
}
