package param;

import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MCBranchPerfomance {
    static GRider instance = new GRider();
    static org.rmj.fund.manager.parameters.MCBranchPerformance trans;
    
    public MCBranchPerfomance() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new org.rmj.fund.manager.parameters.MCBranchPerformance(instance, instance.getBranchCode(), false);
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
    public void test02SaveRecord() {
        try {
            if (trans.searchBranch("GMC Dagupan - ", false)){
                assertEquals((String) trans.getMaster("sBranchCd"), "M001");
                
                trans.setMaster("sPeriodxx", "202112");
                assertEquals((String) trans.getMaster("sPeriodxx"), "202112");
                
                trans.setMaster("nMCGoalxx", 100);
                assertEquals((int) trans.getMaster("nMCGoalxx"), 100);
                
                trans.setMaster("nJOGoalxx", 200);
                assertEquals((int) trans.getMaster("nJOGoalxx"), 200);
                
                trans.setMaster("nSPGoalxx", 200000.00);
                
                trans.setMaster("nLRGoalxx", 100000.00);
                
                if (!trans.SaveRecord()){
                    fail(trans.getMessage());
                } 
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test03OpenRecord() {
        try {
            if (trans.SearchRecord("GMC Dagupan - ", false)){
                if (trans.UpdateRecord()){
                    trans.setMaster("nMCGoalxx", 50);

                    trans.setMaster("nJOGoalxx", 20);

                    trans.setMaster("nSPGoalxx", 20000.00);

                    trans.setMaster("nLRGoalxx", 10000.00);

                    if (!trans.SaveRecord()){
                        fail(trans.getMessage());
                    } 
                } else {
                    fail(trans.getMessage());
                }
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
}