package base;

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
public class IncentiveOpen {
    static GRider instance = new GRider();
    static Incentive trans;
    
    public IncentiveOpen() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new Incentive(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
        trans.setTranStat(0);
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
    public void test02UpdateTransaction(){
        try {            
            if (trans.UpdateTransaction()) {
                assertEquals(EditMode.UPDATE, trans.getEditMode());
                
                assertFalse(trans.removeDetail(1));
                assertFalse(trans.removeDeduction(1));
                assertFalse(trans.removeIncentive("001"));
                
                if (!trans.SaveTransaction())
                    fail(trans.getMessage());
            } else 
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
    }
    
    @Test
    public void test03SearchTransaction() {
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
    public void test04UpdateTransaction(){
        try {            
            if (trans.UpdateTransaction()) {
                assertEquals(EditMode.UPDATE, trans.getEditMode());
                
                trans.setDeductionEmployeeAllocationInfo("nAllcPerc", 1, "M00111005387", 60.00);
                assertEquals(String.valueOf(60.00), String.valueOf(trans.getDeductionEmployeeAllocationInfo("nAllcPerc", 1, "M00111005387")));
                
                trans.setDeductionEmployeeAllocationInfo("nAllcPerc", 1, "M00110017110", 40.00);
                assertEquals(String.valueOf(40.00), String.valueOf(trans.getDeductionEmployeeAllocationInfo("nAllcPerc", 1, "M00110017110")));
                
                trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00111005387", 50000.00);
                assertEquals(50000.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00111005387"));

                trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00103001137", 25000.00);
                assertEquals(25000.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00103001137"));

                trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00108011504", 25000.00);
                assertEquals(25000.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00108011504"));

                trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00110017110", 40.00);
                assertEquals(String.valueOf(40.00), String.valueOf(trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00110017110")));

                trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00113001338", 20.00);
                assertEquals(String.valueOf(20.00), String.valueOf(trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00113001338")));            

                trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001131", 20.00);
                assertEquals(String.valueOf(20.00), String.valueOf(trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001131")));            

                trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001680", 20.00);
                assertEquals(String.valueOf(20.00), String.valueOf(trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001680")));
                
                if (!trans.SaveTransaction())
                    fail(trans.getMessage());
            } else 
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test05SearchTransaction() {
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
}
