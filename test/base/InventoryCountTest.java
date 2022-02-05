/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package base;

import static base.CashCountApprove.instance;
import static base.CashCountLoad.trans;
import java.sql.SQLException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.fund.manager.base.InventoryCount;
import org.rmj.fund.manager.base.LTransaction;

/**
 *
 * @author User
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InventoryCountTest {
    static GRider instance = new GRider();
    static InventoryCount trans;
    static LTransaction listener;
    
    public InventoryCountTest(){
        
    }
    
    @BeforeClass
    public static void setUpClass(){
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        listener = new LTransaction() {
            @Override
            public void MasterRetreive(int fnIndex, Object foValue) {
                System.out.println(fnIndex + "-->" + foValue);
            }
        };
        
        trans = new InventoryCount(instance, instance.getBranchCode(), false);
        trans.setListener(listener);
        trans.setWithUI(false);
    }
    
    @Test
    public void testDisplayMasterFields(){
        try{
            trans.displayMasFields();
        } catch (Exception e){
            fail(e.getMessage());
        }
    }
        
    @Test
    public void test01SearchInventory(){
        boolean isSuccess = false;
        try {
            if (trans.SearchTransaction("GMC Dagupan", false)){
                //get values
                isSuccess = true;
            } else {
                fail(trans.getMessage());
                isSuccess = false;
            }
        } catch (SQLException e) { 
            fail(e.getMessage());
            isSuccess = false;
        }   
        assertTrue(isSuccess);
    }
    
    @Test
    public void test02CloseTransaction(){
        boolean isSuccess = false;
        try {
            if (trans.CloseTransaction()){
                //get values
                isSuccess = true;
            } else {
                fail(trans.getMessage());
                isSuccess = false;
            }
        } catch (SQLException e) { 
            fail(e.getMessage());
            isSuccess = false;
        }   
        assertTrue(isSuccess);
    }
    
    @Test
    public void test03SearchInventoryEmptyParameter(){
        boolean isSuccess = false;
        try {
            if (trans.SearchTransaction("", false)){
                //get values
                isSuccess = true;
            } else {
                fail(trans.getMessage());
                isSuccess = false;
            }
        } catch (SQLException e) { 
            fail(e.getMessage());
            isSuccess = false;
        }   
        assertTrue(isSuccess);
    }
    
    @Test
    public void test04CloseTransaction2(){
        boolean isSuccess = false;
        try {
            if (trans.CloseTransaction()){
                //get values
                isSuccess = true;
            } else {
                fail(trans.getMessage());
                isSuccess = false;
            }
        } catch (SQLException e) { 
            fail(e.getMessage());
            isSuccess = false;
        }   
        assertTrue(isSuccess);
    }
    
    
    @Test
    public void test05SearchInventoryEmptyParameterForPosting(){
        boolean isSuccess = false;
        try {
            if (trans.SearchTransaction("", false)){
                //get values
                isSuccess = true;
            } else {
                fail(trans.getMessage());
                isSuccess = false;
            }
        } catch (SQLException e) { 
            fail(e.getMessage());
            isSuccess = false;
        }   
        assertTrue(isSuccess);
    }
    
    @Test
    public void test06PostTransaction(){
        boolean isSuccess = false;
        try {
            if (trans.PostTransaction()){
                //get values
                isSuccess = true;
            } else {
                fail(trans.getMessage());
                isSuccess = false;
            }
        } catch (SQLException e) { 
            fail(e.getMessage());
            isSuccess = false;
        }   
        assertTrue(isSuccess);
    }
}
