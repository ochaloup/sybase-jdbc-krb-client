package krb.test.sybase;

import com.sybase.jdbc4.jdbc.SybDataSource;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Martin Simka
 */
public class Main {

    // sybase 157
    private static final String JDBC_URL = "jdbc:sybase:Tds:db05.mw.lab.eng.bos.redhat.com:5000/krbusr01";

    public static void main(String[] args) throws Exception {

        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.krb5.realm", "MW.LAB.ENG.BOS.REDHAT.COM");
        System.setProperty("java.security.krb5.kdc", "kerberos-test.mw.lab.eng.bos.redhat.com");
        System.setProperty("java.security.krb5.conf", new File("krb5.conf").getAbsolutePath());

        class Krb5LoginConfiguration extends Configuration {

            private final AppConfigurationEntry[] configList = new AppConfigurationEntry[1];

            public Krb5LoginConfiguration() {
                Map<String, String> options = new HashMap<String, String>();
                options.put("storeKey", "false");
                options.put("useKeyTab", "true");
                options.put("keyTab", "KRBUSR01");
                options.put("principal", "KRBUSR01@MW.LAB.ENG.BOS.REDHAT.COM");
                options.put("doNotPrompt", "true");
                options.put("useTicketCache", "true");
                options.put("ticketCache", "/tmp/krbcc_1000");
                options.put("refreshKrb5Config", "true");
                options.put("isInitiator", "true");
                options.put("addGSSCredential", "true");
                configList[0] = new AppConfigurationEntry(
                        "org.jboss.security.negotiation.KerberosLoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        options);
            }

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                if (name.equals("test")) {
                    return configList;
                }
                return null;
            }
        }

        Configuration.setConfiguration(new Krb5LoginConfiguration());
        final LoginContext lc = new LoginContext("test");
        lc.login();
        Subject subject = lc.getSubject();

        Connection conn = Subject.doAs(subject, new PrivilegedExceptionAction<Connection>() {
            @Override
            public Connection run() throws Exception {
                SybDataSource ds = new SybDataSource();
                ds.setREQUEST_KERBEROS_SESSION("true");
                ds.setSERVICE_PRINCIPAL_NAME("DB05@MW.LAB.ENG.BOS.REDHAT.COM");
                ds.setServerName("db05.mw.lab.eng.bos.redhat.com");
                ds.setPortNumber(5000);
                return ds.getConnection();
            }
        });

        try {
          create(conn);
          print(conn);
        } finally {
          if(conn!=null) conn.close();
        }
    }

    private static void printUserName(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select suser_name()");
            while (rs.next())
                System.out.println("User is: " + rs.getString(1));
            rs.close();
            rs = stmt.executeQuery("select db_name()");
            while (rs.next())
                System.out.println("Database is: " + rs.getString(1));
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

    private static void create(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("CREATE TABLE testentity ( id VARCHAR(255), a INTEGER )");
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }

    private static void drop(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute("DROP TABLE testentity");
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }

    private static void print(Connection conn) throws SQLException {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String query = "select * from testentity";
            // String query = "select table_name from user_tables";
            // String query = "select owner, table_name from all_tables";
            System.out.println("Query: " + query);
            ResultSet rs = stmt.executeQuery(query);
            int columnCount = rs.getMetaData().getColumnCount();
            System.out.println("Result set is: " + rs);
            int row = 1;
            while (rs.next()) {
                StringBuffer output = new StringBuffer();
                for(int i=1; i<=columnCount; i++) { 
                  // String user = rs.getString(1);
                  // System.out.println("User is: " + user);
                  // logger.log(Level.INFO, "User is: " + user);
                  output.append("|" + rs.getObject(i).toString()); 
                }
                System.out.println(row + output.toString());
                row++;
            }
            rs.close();
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }
}
