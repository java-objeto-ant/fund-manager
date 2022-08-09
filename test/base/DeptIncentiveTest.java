package base;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.fund.manager.base.DeptIncentive;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeptIncentiveTest {
    static GRider instance = new GRider();
    static DeptIncentive trans;
    
    public DeptIncentiveTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new DeptIncentive(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01NewTransaction() {
        try {
            if (trans.NewTransaction()){
                for (int lnCtr = 1; lnCtr <= trans.getItemCount(); lnCtr++){
                    System.out.print(trans.getDetail(lnCtr, "sEmployID"));
                    System.out.print("\t");
                    System.out.print(trans.getDetail(lnCtr, "sOldAmtxx"));
                    System.out.print("\t");
                    System.out.print(trans.getDetail(lnCtr, "sNewAmtxx"));
                    System.out.println("");
                }
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        
    }
    
//    @Test
//    public void test02DisplayFields(){
//        try {
//            trans.displayMasFields();
//            trans.displayDetFields();
//           
//        } catch (SQLException e) {
//            fail(e.getMessage());
//        }
//    }
    
//    @Test
//    public void test03MasterSet(){
//        try {
//            String lsPeriodxx = "202112";
//            String lsRemarksx = "December 2021";
//
//            trans.setMaster("dEffctive", lsPeriodxx);
//            trans.setMaster("sRemarksx", lsRemarksx);
//
//            assertEquals(lsPeriodxx, (String) trans.getMaster("dEffctive"));
//            assertEquals(lsRemarksx, (String) trans.getMaster("sRemarksx"));
//        } catch (SQLException e) {
//            e.printStackTrace();
//            fail(e.getMessage());
//        }
//    }
//    
//    @Test
//    public void test04SearchDepartment(){
//        try {
//            if(trans.searchDepartment("Management Information", false)){
//                assertEquals("026", (String) trans.getMaster("sDeptIDxx"));
//                assertEquals("Management Information System", (String) trans.getMaster("xDeptName"));
//                
//                int lnRow = trans.getItemCount();
//                
//                System.out.println("----------------------------------------");
//                for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
//                    System.out.print(trans.getDetail(lnCtr, "sEmployID"));
//                    System.out.print("\t");
//                    System.out.print(trans.getDetail(lnCtr, "sOldAmtxx"));
//                    System.out.print("\t");
//                    System.out.print(trans.getDetail(lnCtr, "xEmployNm"));
//                    System.out.println("");
//                }
//                System.out.println("----------------------------------------");
//                
//                if (trans.removeDetail(lnRow)){
//                    lnRow -= 1;
//                    assertEquals(lnRow, trans.getItemCount());
//                } else 
//                    fail("Unable to remove employee");
//                
//                if (trans.removeDetail(lnRow)){
//                    lnRow -=1;
//                    assertEquals(lnRow, trans.getItemCount());
//                } else 
//                    fail("Unable to remove employee");
//                
//                System.out.println("----------------------------------------");
//                for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
//                    System.out.print(trans.getDetail(lnCtr, "sEmployID"));
//                    System.out.print("\t");
//                    System.out.print(trans.getDetail(lnCtr, "sOldAmtxx"));
//                    System.out.print("\t");
//                    System.out.print(trans.getDetail(lnCtr, "xEmployNm"));
//                    System.out.println("");
//                }
//                System.out.println("----------------------------------------");
//            } else{
//                fail(trans.getMessage());
//            }
//        } catch (SQLException e) {
//            fail(e.getMessage());
//        }
//    }
//    
    
//    @Test
//    public void test22SaveTransation(){
//        try {
//            if (trans.SaveTransaction()){
//                
//            } else {
//                fail(trans.getMessage());
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//            fail(e.getMessage());
//        }
//    }
}