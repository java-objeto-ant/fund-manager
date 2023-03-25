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
import static param.PanaloItem.trans;

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
            
            trans.setMaster(3, "2023-04-20");
            assertEquals("2023-04-20", trans.getMaster("dRaffleDt").toString());
            
            trans.setMaster("sRemarksx", "Sample Remarks");
            assertEquals("Sample Remarks", (String) trans.getMaster("sRemarksx"));
            
            
            
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    @Test 
    public void test03SaveMaster(){
        
        try {
            if (!trans.SaveRecord())
                fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    @Test 
    public void test04SearchRecord(){
        
        try {
            if (!trans.SearchRecord("M00123000002",true))
                fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    
    @Test 
    public void test05ActivateRecord(){
        try {
            if (trans.SearchRecord("M00123000002", true)){
                if (trans.ActivateRecord()){
                } else
                    fail(trans.getMessage());
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    @Test 
    public void test06DeactivateRecord(){
        try {
            if (trans.SearchRecord("M00123000002", true)){
                if (trans.DeactivateRecord()){
                } else
                    fail(trans.getMessage());
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
}