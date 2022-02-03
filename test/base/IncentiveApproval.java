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
import org.rmj.appdriver.constants.EditMode;
import org.rmj.fund.manager.base.Incentive;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IncentiveApproval {
    static GRider instance = new GRider();
    static Incentive trans;
    
    public IncentiveApproval() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new Incentive(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test01SearchTransaction() {
        try {           
            if (trans.SearchTransaction("", false)){
                int lnCtr;
                int lnRow;
                
                //display incentives
                lnRow = trans.getIncentiveCount();
                System.out.println("----------------------------------------");
                for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                    System.out.print(trans.getIncentiveInfo(lnCtr, "sInctveCD"));
                    System.out.print("\t");
                    System.out.print(trans.getIncentiveInfo(lnCtr, "nInctvAmt"));
                    System.out.print("\t");
                    System.out.print(trans.getIncentiveInfo(lnCtr, "xAllocPer") + "%");
                    System.out.print("\t");
                    System.out.print(trans.getIncentiveInfo(lnCtr, "xAllocAmt"));
                    System.out.print("\t");
                    System.out.print(trans.getIncentiveInfo(lnCtr, "xInctvNme"));
                    System.out.println("");
                }
                System.out.println("----------------------------------------");
                
                //display incentive allocations
                lnRow = trans.getItemCount();
                int lnDed = trans.getIncentiveCount();
                System.out.println("----------------------------------------");
                for(int lnCtr2 = 1; lnCtr2 <= lnDed; lnCtr2++){
                    for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                        System.out.print((String) trans.getIncentiveInfo(lnCtr2, "sInctveCD"));
                        System.out.print("\t");
                        System.out.print(trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", (String) trans.getIncentiveInfo(lnCtr2, "sInctveCD"), (String) trans.getDetail(lnCtr, "sEmployID")));
                        System.out.print("\t");
                        System.out.print(trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", (String) trans.getIncentiveInfo(lnCtr2, "sInctveCD"), (String) trans.getDetail(lnCtr, "sEmployID")));
                        System.out.print("\t");
                        System.out.print(trans.getIncentiveEmployeeAllocationInfo("xEmployNm", (String) trans.getIncentiveInfo(lnCtr2, "sInctveCD"), (String) trans.getDetail(lnCtr, "sEmployID")));
                        System.out.println("");
                    }
                }
                System.out.println("----------------------------------------");
                
                //display deductions
                System.out.println("----------------------------------------");
                lnRow = trans.getDeductionCount();
                for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                    System.out.print(lnCtr);
                    System.out.print("\t");
                    System.out.print(trans.getDeductionInfo(lnCtr, "sRemarksx"));
                    System.out.print("\t");
                    System.out.print(trans.getDeductionInfo(lnCtr, "nDedctAmt"));
                    System.out.println("");
                }
                System.out.println("----------------------------------------");
                
                //display deduction allocations
                lnRow = trans.getItemCount();
                lnDed = trans.getDeductionCount();
                System.out.println("----------------------------------------");
                for(int lnCtr2 = 1; lnCtr2 <= lnDed; lnCtr2++){
                    for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                        System.out.print(lnCtr2);
                        System.out.print("\t");
                        System.out.print(trans.getDeductionEmployeeAllocationInfo("nAllcPerc", lnCtr2, (String) trans.getDetail(lnCtr, "sEmployID")));
                        System.out.print("\t");
                        System.out.print(trans.getDeductionEmployeeAllocationInfo("nAllcAmtx", lnCtr2, (String) trans.getDetail(lnCtr, "sEmployID")));
                        System.out.print("\t");
                        System.out.print(trans.getDeductionEmployeeAllocationInfo("xEmployNm", lnCtr2, (String) trans.getDetail(lnCtr, "sEmployID")));
                        System.out.println("");
                    }
                }
                System.out.println("----------------------------------------");
                
                //display summary
                System.out.println("----------------------------------------");
                for (lnCtr = 1; lnCtr <= trans.getItemCount(); lnCtr++){
                    System.out.print(trans.getDetail(lnCtr, "sEmployID"));
                    System.out.print("\t");
                    System.out.print(trans.getDetail(lnCtr, "nTotalAmt"));
                    System.out.print("\t");
                    System.out.print(trans.getDetail(lnCtr, "xEmployNm"));
                    System.out.println("");
                }
                System.out.println("----------------------------------------");
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test02ApproveCSS(){
        try {            
            assertFalse(trans.ApprovedTransactionCSS());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test03ApproveCM(){
        try {            
            assertFalse(trans.ApprovedTransactionCM());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test04CloseTransaction(){
        try {            
            if (trans.CloseTransaction()) {
                assertEquals(EditMode.UNKNOWN, trans.getEditMode());
            } else 
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test05ApproveCM(){
        try {            
            assertFalse(trans.ApprovedTransactionCM());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test06ApproveCSS(){
        try {         
            trans.setTranStat(1);
            if (trans.SearchTransaction("", false)){
                if (trans.ApprovedTransactionCSS()) {
                    assertEquals(EditMode.UNKNOWN, trans.getEditMode());
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
    public void test07ApproveCSS(){
        try {            
            assertFalse(trans.ApprovedTransactionCSS());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test08ApproveCM(){
        try {         
            trans.setTranStat(1);
            if (trans.SearchTransaction("", false)){
                if (trans.ApprovedTransactionCM()) {
                    assertEquals(EditMode.UNKNOWN, trans.getEditMode());
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
    public void test09ApproveCM(){
        try {            
            assertFalse(trans.ApprovedTransactionCM());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
}
