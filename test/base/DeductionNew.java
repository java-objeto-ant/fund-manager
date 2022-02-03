package base;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static base.IncentiveNew.trans;
import java.sql.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GRider;
import org.rmj.fund.manager.base.Incentive;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeductionNew {
    static GRider instance = new GRider();
    static Incentive trans;
    
    public DeductionNew() {
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
    public void test01NewTransaction() {
        try {
            if (trans.NewTransaction()){
                if(trans.searchDepartment("Management Information", false)){
                    assertEquals("026", (String) trans.getMaster("sDeptIDxx"));
                    assertEquals("Management Information System", (String) trans.getMaster("xDeptName"));

                    int lnRow = trans.getItemCount();

                    System.out.println("----------------------------------------");
                    for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                        System.out.print(trans.getDetail(lnCtr, "sEmployID"));
                        System.out.print("\t");
                        System.out.print(trans.getDetail(lnCtr, "nTotalAmt"));
                        System.out.print("\t");
                        System.out.print(trans.getDetail(lnCtr, "xEmployNm"));
                        System.out.println("");
                    }
                    System.out.println("----------------------------------------");
                    
                    if (trans.addDeduction("SSRF")){
                        int lnCtr;
                        lnRow = trans.getDeductionCount();
                        
                        trans.setDeductionInfo(1, "nDedctAmt", 5000.00);

                        System.out.println("----------------------------------------");
                        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                            System.out.print(lnCtr);
                            System.out.print("\t");
                            System.out.print(trans.getDeductionInfo(lnCtr, "sRemarksx"));
                            System.out.print("\t");
                            System.out.print(trans.getDeductionInfo(lnCtr, "nDedctAmt"));
                            System.out.println("");
                        }
                        System.out.println("----------------------------------------");
                        
                        lnRow = trans.getItemCount();
                        int lnDed = trans.getDeductionCount();
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

                        trans.setDeductionEmployeeAllocationInfo("nAllcAmtx", 1, "M00108011504", 750.00);
                        assertEquals(750.00, trans.getDeductionEmployeeAllocationInfo("nAllcAmtx", 1, "M00108011504"));            

                        trans.setDeductionEmployeeAllocationInfo("nAllcAmtx", 1, "M00103001137", 750.00);
                        assertEquals(750.00, trans.getDeductionEmployeeAllocationInfo("nAllcAmtx", 1, "M00103001137")); 
                        
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
                        
                        lnRow = trans.getItemCount();

                        System.out.println("----------------------------------------");
                        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                            System.out.print(trans.getDetail(lnCtr, "sEmployID"));
                            System.out.print("\t");
                            System.out.print(trans.getDetail(lnCtr, "nTotalAmt"));
                            System.out.print("\t");
                            System.out.print(trans.getDetail(lnCtr, "xEmployNm"));
                            System.out.println("");
                        }
                        System.out.println("----------------------------------------");
                        
                        lnRow = trans.getDeductionCount();
                        System.out.println("----------------------------------------");
                        for (lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                            System.out.print(lnCtr);
                            System.out.print("\t");
                            System.out.print(trans.getDeductionInfo(lnCtr, "sRemarksx"));
                            System.out.print("\t");
                            System.out.print(trans.getDeductionInfo(lnCtr, "nDedctAmt"));
                            System.out.print("\t");
                            System.out.print(trans.getDeductionInfo(lnCtr, 101));
                            System.out.print("\t");
                            System.out.print(trans.getDeductionInfo(lnCtr, 102));
                            System.out.println("");
                        }
                        System.out.println("----------------------------------------");
                    }                    
                } else{
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