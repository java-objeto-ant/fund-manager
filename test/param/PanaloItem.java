/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package param;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.fund.manager.base.LMasDetTrans;
import static param.IncentiveBank.trans;

/**
 *
 * @author User
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PanaloItem {
    static GRider instance = new GRider();
    static org.rmj.fund.manager.parameters.PanaloItem trans;
    static org.rmj.fund.manager.base.LMasDetTrans listener;
    
    public PanaloItem() {
    }
    
    @BeforeClass
    public static void setUpClass() {  
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
//        listener = new LMasDetTrans() {
//            @Override
//            public void MasterRetreive(int fnIndex, Object foValue) { 
//               
//                switch (fnIndex) {
//                    case 3:
//                        System.out.println(foValue);
//                        assertEquals("0002", (String)foValue);
//                        break;
//                    case 4:
//                        assertEquals("MONTHLY REBATE", (String) foValue);
//                        break;
//
//                    default:
//                        throw new AssertionError();
//                }
//                
//
//            }
//            @Override
//            public void DetailRetreive(int fnRow, int fnIndex, Object foValue) {
//            }
//         };
        trans = new org.rmj.fund.manager.parameters.PanaloItem(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
        trans.setListener(listener);
        
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01NewTransaction() {
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
    public void test02SearchPanalo(){
        try {
            if (trans.searchPanalo("0002", true)){ 
                assertEquals("0002", (String) trans.getMaster("sPanaloCd"));
                assertEquals("sPanaloDs", "MONTHLY REBATE", (String) trans.getMaster("sPanaloDs"));
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    @Test 
    public void test03SearchSpareparts(){
        try {
            if (trans.searchSpareparts("11001-1499", true)){ 
                assertEquals("11001-1499", (String) trans.getMaster("sBarrcode"));
                assertEquals("GCO116000003", (String) trans.getMaster("sPartsIDx"));
                assertEquals("xDescript", "HEAD CYLINDER", (String) trans.getMaster("xDescript"));
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test04SetMaster(){
        try {
           
            trans.setMaster("sDescript", "Sample Panalo Item");
            assertEquals("sDescript", "Sample Panalo Item", (String) trans.getMaster("sDescript"));

        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test05SaveRecord(){
        try {
            if (!trans.SaveRecord())
                fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test06Search(){
        try {
            if (trans.SearchRecord("Updated Panalo Item", false)){
//                System.out.println((String) trans.getMaster("sItemIDxx"));
                if (trans.UpdateRecord()){
                    trans.setMaster("sDescript", "Updated Panalo Item");
                    assertEquals("sDescript", "Updated Panalo Item", (String) trans.getMaster("sDescript"));
            

                } else
                    fail(trans.getMessage());
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
//    
    @Test 
    public void test07SaveRecord(){
        try {
            if (!trans.SaveRecord())
                fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    
    @Test 
    public void test08Search(){
        try {
            if (trans.SearchRecord("Updated Panalo", false)){
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
    public void test09Search(){
        try {
            if (trans.SearchRecord("Updated Panalo", false)){
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
