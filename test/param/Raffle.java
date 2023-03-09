package param;

import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import static param.Incentive.trans;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Raffle {
    static GRider instance = new GRider();
    static org.rmj.fund.manager.parameters.Raffle trans;
    
    public Raffle() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new org.rmj.fund.manager.parameters.Raffle(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01NewRecord() {
        try {
            if (trans.NewRecord()){
                trans.displayMasFields();
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test02SetMaster(){
        try {
            trans.setMaster(2, "2023-03-07");
            assertEquals("2023-03-07", trans.getMaster(2).toString());
            
            trans.setMaster(3, "2023-04-07");
            assertEquals("2023-04-07", trans.getMaster(3).toString());
            
            trans.setMaster("sRemarksx", "Testing Remarks");
            assertEquals("Testing Remarks", (String) trans.getMaster("sRemarksx"));
            
            trans.setMaster("cTranStat", "1");
            assertEquals("1", (String) trans.getMaster("cTranStat"));
            
            trans.setMaster("sModified", "M001111122");
            assertEquals("M001111122", (String) trans.getMaster("sModified"));
            
            trans.setMaster("dModified", "2023-03-07");
            assertEquals("2023-03-07", (String) trans.getMaster("dModified"));
            
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
}