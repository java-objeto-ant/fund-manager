package base;

import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.fund.manager.base.IncentiveRelease;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReleaseLoad {
    static GRider instance = new GRider();
    static IncentiveRelease trans;
    
    public ReleaseLoad() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new IncentiveRelease(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01SearchTransaction() {
        try {
            if (trans.SearchTransaction("M00121000001", true)){
                System.out.println(trans.getItemCount());
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test02ConfirmTransaction() {
        try {
            if (!trans.ConfirmTransaction()) fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test03SearchTransaction() {
        try {
            if (trans.SearchTransaction("M00121000001", true)){
                System.out.println(trans.getItemCount());
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test04CancelTransaction() {
        try {
            if (!trans.CancelTransaction()) fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
}