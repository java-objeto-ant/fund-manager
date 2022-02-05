/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package base;

import static base.InventoryCountTest.trans;
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
public class InventoryCountDetailTest {
    static GRider instance = new GRider();
    static InventoryCount trans;
    static LTransaction listener;
    
    public InventoryCountDetailTest(){
        
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
    public void test01SearchTransaction(){
        boolean isSuccess = false;
        try{
            if(trans.SearchTransaction("GMC Dagupan", false)){
                isSuccess = true;
            } else {
                isSuccess = false;
            }
        } catch (Exception e){
            fail(e.getMessage());
            isSuccess = false;
        }
        assertTrue(isSuccess);
    }
    
    @Test
    public void test02GetMaster(){
        boolean isSuccess = false;
        try{
           int lnCount = trans.getItemCount();
           for(int x = 0; x < lnCount; x++){
               trans.getMaster(x + 1);
           }
           isSuccess = true;
        } catch (Exception e){
            fail(e.getMessage());
            isSuccess = false;
        }
        assertTrue(isSuccess);
    }
    
    
    @Test
    public void test03GetDetail(){
        boolean isSuccess = false;
        try{
           int lnCount = trans.getItemCount();
           for(int x = 0; x < lnCount; x++){
//               String lsTransNox = (String)trans.getDetail(x + 1, 1);
           }
           isSuccess = true;
        } catch (Exception e){
            fail(e.getMessage());
            isSuccess = false;
        }
        assertTrue(isSuccess);
    }
}
