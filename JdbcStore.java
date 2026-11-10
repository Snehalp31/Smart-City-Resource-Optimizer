import java.sql.*;
import java.nio.file.*;
import java.util.*;

/**
 * Optional JDBC persistence. Uses SQLite for local development when its driver is present,
 * or MySQL when DB_URL/DB_USER/DB_PASSWORD are configured.
 */
public final class JdbcStore {
    private static boolean active;
    private static String url;
    private static String user;
    private static String password;
    private static boolean mysql;

    private JdbcStore() {}
    public static boolean isActive() { return active; }

    public static void initializeIfAvailable() throws Exception {
        String configured = System.getenv("DB_URL");
        url = (configured == null || configured.isBlank()) ? "jdbc:sqlite:data/smartcity.db" : configured;
        user = System.getenv().getOrDefault("DB_USER", "");
        password = System.getenv().getOrDefault("DB_PASSWORD", "");
        mysql = url.startsWith("jdbc:mysql:");
        try (Connection c = connection()) {
            createSchema(c);
            active = true;
        } catch (SQLException ex) {
            if (configured != null && !configured.isBlank()) {
                throw new Exception("Database was configured but connection/schema initialization failed: " + ex.getMessage(), ex);
            }
            // No SQLite driver or local DB unavailable: retain the supported file-store fallback.
            active = false;
        }
    }

    private static Connection connection() throws SQLException {
        Connection c = user.isBlank() ? DriverManager.getConnection(url) : DriverManager.getConnection(url,user,password);
        if (!mysql) {
            try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); }
        }
        return c;
    }

    private static void createSchema(Connection c) throws SQLException {
        String autoId = mysql ? "BIGINT PRIMARY KEY AUTO_INCREMENT" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        try (Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users (user_id VARCHAR(120) PRIMARY KEY, salt VARCHAR(200) NOT NULL, password_hash VARCHAR(300) NOT NULL, role VARCHAR(20) NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS areas (name VARCHAR(200) PRIMARY KEY, latitude DOUBLE NULL, longitude DOUBLE NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS roads (area_a VARCHAR(200) NOT NULL, area_b VARCHAR(200) NOT NULL, distance_km INTEGER NOT NULL, PRIMARY KEY(area_a,area_b), FOREIGN KEY(area_a) REFERENCES areas(name), FOREIGN KEY(area_b) REFERENCES areas(name))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS resource_centers (area_name VARCHAR(200) PRIMARY KEY, FOREIGN KEY(area_name) REFERENCES areas(name))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS resources (resource_id VARCHAR(120) PRIMARY KEY, area_name VARCHAR(200) NOT NULL, resource_type VARCHAR(100) NOT NULL, driver_name VARCHAR(200) NOT NULL, available BOOLEAN NOT NULL, FOREIGN KEY(area_name) REFERENCES resource_centers(area_name))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS emergency_requests (request_id VARCHAR(80) PRIMARY KEY, requester_id VARCHAR(120) NOT NULL, emergency_type VARCHAR(100) NOT NULL, description TEXT, location_area VARCHAR(200) NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, priority INTEGER NOT NULL, status VARCHAR(30) NOT NULL, resource_id VARCHAR(120), route_text TEXT, created_at VARCHAR(50) NOT NULL, FOREIGN KEY(requester_id) REFERENCES users(user_id))");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS activity_history (id " + autoId + ", occurred_at VARCHAR(50) NOT NULL, actor_id VARCHAR(120), action_text TEXT NOT NULL)");
        }
    }

    public static void loadAccounts() throws SQLException {
        MainGUIPastel.ACCOUNTS_MAP.clear();
        try (Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT user_id,salt,password_hash,role FROM users")) {
            while(r.next()) MainGUIPastel.ACCOUNTS_MAP.put(r.getString(1),
                new MainGUIPastel.Account(r.getString(2),r.getString(3),r.getString(4)));
        }
    }
    public static void saveAccounts() throws SQLException {
        try(Connection c=connection()) {
            c.setAutoCommit(false);
            try(PreparedStatement u=c.prepareStatement("UPDATE users SET salt=?,password_hash=?,role=? WHERE user_id=?");
                PreparedStatement i=c.prepareStatement("INSERT INTO users(user_id,salt,password_hash,role) VALUES(?,?,?,?)")) {
                for(Map.Entry<String,MainGUIPastel.Account> e:MainGUIPastel.ACCOUNTS_MAP.entrySet()) {
                    MainGUIPastel.Account a=e.getValue();
                    u.setString(1,a.salt);u.setString(2,a.passwordHash);u.setString(3,a.role);u.setString(4,e.getKey());
                    if(u.executeUpdate()==0){i.setString(1,e.getKey());i.setString(2,a.salt);i.setString(3,a.passwordHash);i.setString(4,a.role);i.executeUpdate();}
                }
                c.commit();
            } catch(SQLException ex){c.rollback();throw ex;}
        }
    }
    public static void loadCity() throws SQLException {
        MainGUIPastel.CITY.adj.clear(); MainGUIPastel.CITY.coordinates.clear();
        try(Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT name,latitude,longitude FROM areas")) {
            while(r.next()) {
                String name=r.getString(1); MainGUIPastel.CITY.addArea(name);
                double lat=r.getDouble(2),lng=r.getDouble(3);
                if(!r.wasNull()) MainGUIPastel.CITY.coordinates.put(name,new MainGUIPastel.GeoPoint(lat,lng));
            }
        }
        try(Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT area_a,area_b,distance_km FROM roads")) {
            while(r.next()) {
                String a=r.getString(1),b=r.getString(2); int d=r.getInt(3);
                if(MainGUIPastel.CITY.adj.containsKey(a)&&MainGUIPastel.CITY.adj.containsKey(b)&&d>0)
                    MainGUIPastel.CITY.addRoad(a,b,d);
            }
        }
    }
    public static void saveCity() throws SQLException {
        try(Connection c=connection()) {
            c.setAutoCommit(false);
            try(PreparedStatement areaUpdate=c.prepareStatement("UPDATE areas SET latitude=?,longitude=? WHERE name=?");
                PreparedStatement areaInsert=c.prepareStatement("INSERT INTO areas(name,latitude,longitude) VALUES(?,?,?)");
                Statement del=c.createStatement();
                PreparedStatement road=c.prepareStatement("INSERT INTO roads(area_a,area_b,distance_km) VALUES(?,?,?)")) {
                for(String name:MainGUIPastel.CITY.adj.keySet()) {
                    MainGUIPastel.GeoPoint p=MainGUIPastel.CITY.coordinates.get(name);
                    areaUpdate.setObject(1,p==null?null:p.lat);areaUpdate.setObject(2,p==null?null:p.lng);areaUpdate.setString(3,name);
                    if(areaUpdate.executeUpdate()==0){areaInsert.setString(1,name);areaInsert.setObject(2,p==null?null:p.lat);areaInsert.setObject(3,p==null?null:p.lng);areaInsert.executeUpdate();}
                }
                del.executeUpdate("DELETE FROM roads");
                for(String a:MainGUIPastel.CITY.adj.keySet()) for(Map.Entry<String,Integer> e:MainGUIPastel.CITY.adj.get(a).entrySet())
                    if(a.compareTo(e.getKey())<0){road.setString(1,a);road.setString(2,e.getKey());road.setInt(3,e.getValue());road.addBatch();}
                road.executeBatch();c.commit();
            }catch(SQLException ex){c.rollback();throw ex;}
        }
    }
    public static void loadResources() throws SQLException {
        MainGUIPastel.CITY.resources.clear();
        try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT area_name FROM resource_centers")) {
            while(r.next())MainGUIPastel.CITY.resources.put(r.getString(1),new ArrayList<>());
        }
        try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT area_name,resource_type,resource_id,driver_name,available FROM resources")) {
            while(r.next())MainGUIPastel.CITY.resources.computeIfAbsent(r.getString(1),k->new ArrayList<>())
                .add(new MainGUIPastel.Resource(r.getString(2),r.getString(3),r.getString(4),r.getBoolean(5)));
        }
    }
    public static void saveResources() throws SQLException {
        try(Connection c=connection()) {
            c.setAutoCommit(false);
            try(Statement del=c.createStatement();
                PreparedStatement center=c.prepareStatement("INSERT INTO resource_centers(area_name) VALUES(?)");
                PreparedStatement resource=c.prepareStatement("INSERT INTO resources(resource_id,area_name,resource_type,driver_name,available) VALUES(?,?,?,?,?)")) {
                del.executeUpdate("DELETE FROM resources");del.executeUpdate("DELETE FROM resource_centers");
                for(Map.Entry<String,List<MainGUIPastel.Resource>> e:MainGUIPastel.CITY.resources.entrySet()) {
                    center.setString(1,e.getKey());center.executeUpdate();
                    for(MainGUIPastel.Resource r:e.getValue()) {
                        resource.setString(1,r.id);resource.setString(2,e.getKey());resource.setString(3,r.type);
                        resource.setString(4,r.driver);resource.setBoolean(5,r.available);resource.addBatch();
                    }
                }
                resource.executeBatch();c.commit();
            }catch(SQLException ex){c.rollback();throw ex;}
        }
    }
    public static void loadRequests() throws SQLException {
        MainGUIPastel.REQUESTS_LIST.clear();
        try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery(
            "SELECT request_id,requester_id,emergency_type,location_area,priority,status,COALESCE(resource_id,'None'),COALESCE(route_text,''),COALESCE(description,''),latitude,longitude,created_at FROM emergency_requests")) {
            while(r.next())MainGUIPastel.REQUESTS_LIST.add(new MainGUIPastel.EmergencyRequest(
                r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getInt(5),r.getString(6),
                r.getString(7),r.getString(8),r.getString(9),r.getDouble(10),r.getDouble(11),r.getString(12)));
        }
    }
    public static void saveRequests() throws SQLException {
        try(Connection c=connection()) {
            c.setAutoCommit(false);
            try(Statement del=c.createStatement();PreparedStatement ins=c.prepareStatement(
                "INSERT INTO emergency_requests(request_id,requester_id,emergency_type,description,location_area,latitude,longitude,priority,status,resource_id,route_text,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")) {
                del.executeUpdate("DELETE FROM emergency_requests");
                for(MainGUIPastel.EmergencyRequest r:MainGUIPastel.REQUESTS_LIST) {
                    ins.setString(1,r.id);ins.setString(2,r.requester);ins.setString(3,r.type);ins.setString(4,r.description);
                    ins.setString(5,r.location);ins.setDouble(6,r.latitude);ins.setDouble(7,r.longitude);ins.setInt(8,r.priority);
                    ins.setString(9,r.status);ins.setString(10,r.resourceId.equals("None")?null:r.resourceId);
                    ins.setString(11,r.route);ins.setString(12,r.createdAt);ins.addBatch();
                }
                ins.executeBatch();c.commit();
            }catch(SQLException ex){c.rollback();throw ex;}
        }
    }
    public static void logActivity(String actor,String action) {
        try(Connection c=connection();PreparedStatement p=c.prepareStatement(
            "INSERT INTO activity_history(occurred_at,actor_id,action_text) VALUES(?,?,?)")) {
            p.setString(1,java.time.Instant.now().toString());p.setString(2,actor);p.setString(3,action);p.executeUpdate();
        }catch(SQLException ex){System.err.println("Activity database write failed: "+ex.getMessage());}
    }
    public static String readActivity() throws SQLException {
        StringBuilder b=new StringBuilder();
        try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery(
            "SELECT occurred_at,actor_id,action_text FROM activity_history ORDER BY id DESC")) {
            while(r.next())b.append(r.getString(1)).append(" | ").append(r.getString(2)).append(" | ").append(r.getString(3)).append(System.lineSeparator());
        }
        return b.toString();
    }
}
