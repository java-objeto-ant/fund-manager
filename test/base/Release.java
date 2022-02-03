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
import org.rmj.fund.manager.base.IncentiveRelease;
import org.rmj.fund.manager.parameters.IncentiveBankInfo;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Release {
    static GRider instance = new GRider();
    static IncentiveRelease trans;
    
    public Release() {
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
    public void test01NewTransaction() {
        try {
            if (trans.NewTransaction()){
                trans.displayMasFields();                
                
                for (int lnCtr = 0; lnCtr <= trans.getItemCount()-1; lnCtr++){
                    System.out.println(trans.getDetail(lnCtr).getMaster("sTransNox"));
                    System.out.println(trans.getDetail(lnCtr).getMaster("dTransact"));
                    System.out.println(trans.getDetail(lnCtr).getMaster("sMonthxxx"));
                    System.out.println(trans.getDetail(lnCtr).getMaster("sRemarksx"));
                    System.out.println(trans.getDetail(lnCtr).getMaster("xBranchNm"));
                    System.out.println(trans.getDetail(lnCtr).getMaster("xDeptName"));
                }
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }

    @Test
    public void test02Tag() {
        try {
            trans.setTag(0, true);
            assertTrue(trans.getTag(0));
            
            trans.setTag(0, false);
            assertFalse(trans.getTag(0));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test03DisplayDetail() {
        try {
            String lsBankName;
            String lsBankAcct;
            
            IncentiveBankInfo loBank;
            
            for (int lnCtr = 1; lnCtr <= trans.getDetail(0).getItemCount(); lnCtr++){
                loBank = trans.getBankInfo((String) trans.getDetail(0).getDetail(lnCtr, "sEmployID"));
                
                if (loBank != null){
                    lsBankName = (String) loBank.getMaster("xBankName");
                    lsBankAcct = (String) loBank.getMaster("sBankAcct");
                } else {
                    lsBankName = "";
                    lsBankAcct = "";
                }
                    
                System.out.println(trans.getDetail(0).getDetail(lnCtr, "xEmployNm"));
                System.out.println(trans.getDetail(0).getDetail(lnCtr, "xEmpLevNm"));
                System.out.println(trans.getDetail(0).getDetail(lnCtr, "xPositnNm"));
                System.out.println(trans.getDetail(0).getDetail(lnCtr, "xSrvcYear"));
                System.out.println("BANK: " + lsBankName);
                System.out.println("ACCT: " + lsBankAcct);
                System.out.println(trans.getDetail(0).getDetail(lnCtr, "nTotalAmt"));
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }   
    }
    
//    
//    @Test
//    public void test03SaveTransaction() {
//        try {
//            if (trans.SaveTransaction()){
//            } else {
//                fail(trans.getMessage());
//            }
//        } catch (SQLException e) {
//            fail(e.getMessage());
//        }
//        
//    }
}