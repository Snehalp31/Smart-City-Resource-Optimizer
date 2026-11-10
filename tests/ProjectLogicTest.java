import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ProjectLogicTest {
    @Test
    void dijkstraFindsShortestWeightedRoute() {
        MainGUIPastel.CityModel city = new MainGUIPastel.CityModel();
        city.addArea("A"); city.addArea("B"); city.addArea("C");
        city.addRoad("A", "B", 5); city.addRoad("B", "C", 4); city.addRoad("A", "C", 20);
        city.resources.put("A", new java.util.ArrayList<>());
        city.resources.get("A").add(new MainGUIPastel.Resource("ambulance", "AMB-1", "Driver", true));
        MainGUIPastel.Allocation allocation = city.allocateNearest("C", "ambulance");
        assertNotNull(allocation);
        assertEquals(9, allocation.distance);
        assertEquals(java.util.List.of("A", "B", "C"), allocation.path);
    }

    @Test
    void disconnectedResourceIsNotAllocated() {
        MainGUIPastel.CityModel city = new MainGUIPastel.CityModel();
        city.addArea("A"); city.addArea("B");
        city.resources.put("A", new java.util.ArrayList<>());
        city.resources.get("A").add(new MainGUIPastel.Resource("fire", "F-1", "Driver", true));
        assertNull(city.allocateNearest("B", "fire"));
    }

    @Test
    void unavailableOrWrongTypeResourcesAreNotAllocated() {
        MainGUIPastel.CityModel city = new MainGUIPastel.CityModel();
        city.addArea("A"); city.addArea("B"); city.addRoad("A", "B", 3);
        city.resources.put("A", new java.util.ArrayList<>());
        city.resources.get("A").add(new MainGUIPastel.Resource("ambulance", "AMB-1", "Driver", false));
        city.resources.get("A").add(new MainGUIPastel.Resource("fire", "F-1", "Driver", true));
        assertNull(city.allocateNearest("B", "ambulance"));
    }

    @Test
    void passwordsAreHashedAndVerified() throws Exception {
        char[] password = "securePass123!".toCharArray();
        MainGUIPastel.Account account = MainGUIPastel.Passwords.create("demo", password, "citizen");
        assertNotEquals("securePass123!", account.passwordHash);
        assertTrue(MainGUIPastel.Passwords.verify("securePass123!".toCharArray(), account.salt, account.passwordHash));
        assertFalse(MainGUIPastel.Passwords.verify("incorrect".toCharArray(), account.salt, account.passwordHash));
    }
}
