package ch.psi.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class DataAPITest {
    
    public DataAPITest() {
    }

    /**
     * Test of queryNames method, of class DataAPI.
     */
    @Test
    public void testQueryNames_4args_1() throws Exception {
        System.out.println("queryNames");
        String regex = "AMPLT|PHASE";
        String[] backends = null;
        DataAPI.Ordering ordering = null;
        Boolean reload = null;
        DataAPI instance = new DataAPI("https://data-api.psi.ch/sf");
        List<Map<String, Object>> expResult =   null;
        List<Map<String, Object>> result = instance.queryNames(regex, backends, ordering, reload);
        assertEquals(result.size(), 3);
    }

    /**
     * Test of queryNames method, of class DataAPI.
     */
    @Test
    public void testQueryNames_4args_2() throws Exception {
        System.out.println("queryNames");
        String regex = "S10CB01-RBOC-DCP10:FOR-AMPLT";
        String backend =  "sf-databuffer";
        DataAPI.Ordering ordering = DataAPI.Ordering.asc;
        Boolean reload = null;
        DataAPI instance = new DataAPI("https://data-api.psi.ch/sf");
        List<String> expResult = Arrays.asList(new String[]{"S10CB01-RBOC-DCP10:FOR-AMPLT", "S10CB01-RBOC-DCP10:FOR-AMPLT-AVG", "S10CB01-RBOC-DCP10:FOR-AMPLT-MAX"});
        List<String> result = instance.queryNames(regex, backend, ordering, reload);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
    }
    
}
