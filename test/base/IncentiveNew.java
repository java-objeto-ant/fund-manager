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
import org.rmj.fund.manager.base.Incentive;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IncentiveNew {
    static GRider instance = new GRider();
    static Incentive trans;
    
    public IncentiveNew() {
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
                for (int lnCtr = 1; lnCtr <= trans.getItemCount(); lnCtr++){
                    System.out.print(trans.getDetail(lnCtr, "sEmployID"));
                    System.out.print("\t");
                    System.out.print(trans.getDetail(lnCtr, "nTotalAmt"));
                    System.out.print("\t");
                    System.out.print(trans.getDetail(lnCtr, "xEmployNm"));
                    System.out.println("");
                }
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        
    }
    
    @Test
    public void test02DisplayFields(){
        try {
            trans.displayMasFields();
            trans.displayDetFields();
            trans.displayDetAllocFields();
            trans.displayDetAllocEmpFields();
            trans.displayDetDeductionAllocFields();
            trans.displayDetDeductionAllocEmpFields();
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test03MasterSet(){
        try {
            String lsPeriodxx = "202112";
            String lsRemarksx = "December 2021";

            trans.setMaster("sMonthxxx", lsPeriodxx);
            trans.setMaster("sRemarksx", lsRemarksx);

            assertEquals(lsPeriodxx, (String) trans.getMaster("sMonthxxx"));
            assertEquals(lsRemarksx, (String) trans.getMaster("sRemarksx"));
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test04SearchDepartment(){
        try {
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
                
                if (trans.removeDetail(lnRow)){
                    lnRow -= 1;
                    assertEquals(lnRow, trans.getItemCount());
                } else 
                    fail("Unable to remove employee");
                
                if (trans.removeDetail(lnRow)){
                    lnRow -=1;
                    assertEquals(lnRow, trans.getItemCount());
                } else 
                    fail("Unable to remove employee");
                
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
            } else{
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test05AddIncentive(){
        try {
            if(trans.searchIncentive("MC Sales", false)){
                assertEquals("001", (String) trans.getIncentiveInfo(1, "sInctveCD"));
                assertEquals("MC Sales", (String) trans.getIncentiveInfo(1, "xInctvNme"));
            } else{
                fail(trans.getMessage());
            }
            
            if(trans.searchIncentive("Service", false)){
                assertEquals("002", (String) trans.getIncentiveInfo(2, "sInctveCD"));
                assertEquals("Service", (String) trans.getIncentiveInfo(2, "xInctvNme"));
            } else{
                fail(trans.getMessage());
            }
            
            assertEquals(2, trans.getIncentiveCount());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test06RemoveIncentive(){
        try {
            if (trans.removeIncentive("002")){
                assertEquals(1, trans.getIncentiveCount());
            } else{
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test07RemoveEmpWithAddedIncentive(){
        try {
            int lnRow = trans.getItemCount();
            
            assertFalse(trans.removeDetail(lnRow));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test08ValidateDetailAllocationCount(){
        try {
            assertEquals(trans.getIncentiveEmployeeAllocationCount(), trans.getIncentiveCount() * trans.getItemCount());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test09AddIncentive(){
        try {
            if(trans.searchIncentive("Service", false)){
                assertEquals("002", (String) trans.getIncentiveInfo(2, "sInctveCD"));
                assertEquals("Service", (String) trans.getIncentiveInfo(2, "xInctvNme"));
            } else{
                fail(trans.getMessage());
            }
            
            assertEquals(2, trans.getIncentiveCount());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
        
    }
    
    @Test
    public void test10ValidateDetailAllocationCount(){
        try {
            assertEquals(trans.getIncentiveEmployeeAllocationCount(), trans.getIncentiveCount() * trans.getItemCount());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test11SetIncentiveInfo(){
        try {
            String sInctveCD = "001";
            String sRemarksx = "MC Sales";
            int nQtyGoalx = 100;
            int nQtyActlx = 110;
            double nAmtGoalx = 0.00;
            double nAmtActlx = 0.00;
            double nInctvAmt = 10000.00;
            
            trans.setIncentiveInfo(1, "sRemarksx", sRemarksx);
            trans.setIncentiveInfo(1, "nQtyGoalx", nQtyGoalx);
            trans.setIncentiveInfo(1, "nQtyActlx", nQtyActlx);
            trans.setIncentiveInfo(1, "nAmtGoalx", nAmtGoalx);
            trans.setIncentiveInfo(1, "nAmtActlx", nAmtActlx);
            trans.setIncentiveInfo(1, "nInctvAmt", nInctvAmt);
            
            assertEquals(sInctveCD, trans.getIncentiveInfo(1, "sInctveCD"));
            assertEquals(sRemarksx, trans.getIncentiveInfo(1, "sRemarksx"));
            assertEquals(nQtyGoalx, trans.getIncentiveInfo(1, "nQtyGoalx"));
            assertEquals(nQtyActlx, trans.getIncentiveInfo(1, "nQtyActlx"));
            assertEquals(nAmtGoalx, trans.getIncentiveInfo(1, "nAmtGoalx"));
            assertEquals(nAmtActlx, trans.getIncentiveInfo(1, "nAmtActlx"));
            assertEquals(nInctvAmt, trans.getIncentiveInfo(1, "nInctvAmt"));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test12SetIncentiveInfo(){
        try {
            String sInctveCD = "002";
            String sRemarksx = "Service";
            int nQtyGoalx = 0;
            int nQtyActlx = 0;
            double nAmtGoalx = 100000.00;
            double nAmtActlx = 110000.00;
            double nInctvAmt = 20000.00;
            
            trans.setIncentiveInfo(2, "sRemarksx", sRemarksx);
            trans.setIncentiveInfo(2, "nQtyGoalx", nQtyGoalx);
            trans.setIncentiveInfo(2, "nQtyActlx", nQtyActlx);
            trans.setIncentiveInfo(2, "nAmtGoalx", nAmtGoalx);
            trans.setIncentiveInfo(2, "nAmtActlx", nAmtActlx);
            trans.setIncentiveInfo(2, "nInctvAmt", nInctvAmt);
            
            assertEquals(sInctveCD, trans.getIncentiveInfo(2, "sInctveCD"));
            assertEquals(sRemarksx, trans.getIncentiveInfo(2, "sRemarksx"));
            assertEquals(nQtyGoalx, trans.getIncentiveInfo(2, "nQtyGoalx"));
            assertEquals(nQtyActlx, trans.getIncentiveInfo(2, "nQtyActlx"));
            assertEquals(nAmtGoalx, trans.getIncentiveInfo(2, "nAmtGoalx"));
            assertEquals(nAmtActlx, trans.getIncentiveInfo(2, "nAmtActlx"));
            assertEquals(nInctvAmt, trans.getIncentiveInfo(2, "nInctvAmt"));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test13SetIncentiveEmployee(){
        try {
            int lnRow = trans.getIncentiveCount();
            
            System.out.println("----------------------------------------");
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
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
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "001", "M00111005387", 3000.00);
            assertEquals(3000.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "001", "M00111005387"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "001", "M00103001137", 3500.00);
            assertEquals(3500.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "001", "M00103001137"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "001", "M00108011504", 3500.00);
            assertEquals(3500.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "001", "M00108011504"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "001", "M00111005387", 30.00);
            assertEquals(0.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "001", "M00111005387"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "001", "M00103001137", 35.00);
            assertEquals(0.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "001", "M00103001137"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "001", "M00108011504", 35.00);
            assertEquals(0.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "001", "M00108011504"));
            
            System.out.println("----------------------------------------");
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
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
            
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test14DisplayUpdatedAmount(){
        try {
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
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test14SetIncentiveEmployee(){
        try {
            int lnRow = trans.getIncentiveCount();
            
            System.out.println("----------------------------------------");
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
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
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "002", "M00111005387", 3000.00);
            assertEquals(0.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "002", "M00111005387"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "002", "M00103001137", 3500.00);
            assertEquals(0.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "002", "M00103001137"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "002", "M00108011504", 3500.00);
            assertEquals(0.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "002", "M00108011504"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00111005387", 30.00);
            assertEquals(30.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00111005387"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00103001137", 30.00);
            assertEquals(30.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00103001137"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00108011504", 30.00);
            assertEquals(30.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00108011504"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00110017110", 10.00);
            assertEquals(10.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "002", "M00110017110"));
            
            System.out.println("----------------------------------------");
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
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
            
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test16DisplayUpdatedAmount(){
        try {
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
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test17AddIncentive(){
        try {
            String sInctveCD = "003";
            String sRemarksx = "MIS Goal Cash";
            if(trans.searchIncentive("MIS Goal Cash", false)){
                assertEquals("003", (String) trans.getIncentiveInfo(3, "sInctveCD"));
                assertEquals("MIS Goal Cash", (String) trans.getIncentiveInfo(3, "xInctvNme"));
                
                
                int nQtyGoalx = 0;
                int nQtyActlx = 0;
                double nAmtGoalx = 0.00;
                double nAmtActlx = 0.00;
                double nInctvAmt = 200000.00;

                trans.setIncentiveInfo(3, "sRemarksx", sRemarksx);
                trans.setIncentiveInfo(3, "nQtyGoalx", nQtyGoalx);
                trans.setIncentiveInfo(3, "nQtyActlx", nQtyActlx);
                trans.setIncentiveInfo(3, "nAmtGoalx", nAmtGoalx);
                trans.setIncentiveInfo(3, "nAmtActlx", nAmtActlx);
                trans.setIncentiveInfo(3, "nInctvAmt", nInctvAmt);

                assertEquals(sInctveCD, trans.getIncentiveInfo(3, "sInctveCD"));
                assertEquals(sRemarksx, trans.getIncentiveInfo(3, "sRemarksx"));
                assertEquals(nQtyGoalx, trans.getIncentiveInfo(3, "nQtyGoalx"));
                assertEquals(nQtyActlx, trans.getIncentiveInfo(3, "nQtyActlx"));
                assertEquals(nAmtGoalx, trans.getIncentiveInfo(3, "nAmtGoalx"));
                assertEquals(nAmtActlx, trans.getIncentiveInfo(3, "nAmtActlx"));
                assertEquals(nInctvAmt, trans.getIncentiveInfo(3, "nInctvAmt"));
            } else{
                fail(trans.getMessage());
            }
            
            assertEquals(3, trans.getIncentiveCount());
            
            System.out.println("----------------------------------------");
            int lnRow = trans.getIncentiveCount();
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
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
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00111005387", 50000.00);
            assertEquals(50000.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00111005387"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00103001137", 50000.00);
            assertEquals(50000.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00103001137"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00108011504", 50000.00);
            assertEquals(50000.00, trans.getIncentiveEmployeeAllocationInfo("nAllcAmtx", "003", "M00108011504"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00110017110", 25.00);
            assertEquals(25.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00110017110"));
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00113001338", 25.00);
            assertEquals(25.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00113001338"));            
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001131", 25.00);
            assertEquals(25.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001131"));            
            
            trans.setIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001680", 25.00);
            assertEquals(25.00, trans.getIncentiveEmployeeAllocationInfo("nAllcPerc", "003", "M00119001680"));            
            
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
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
            
//            assertEquals(trans.getIncentiveInfo(3, "nInctvAmt"), trans.getIncentiveInfo(3, "xAllocAmt"));
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test17DisplayUpdatedAmount(){
        try {
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
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test19Deduction(){
        try {
            if (trans.addDeduction("SSRF")){
                int lnCtr;
                int lnRow = trans.getDeductionCount();
                
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
                
                if (trans.addDeduction("SSRF")){
                    lnRow = trans.getDeductionCount();
                
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
                } else 
                    fail(trans.getMessage());
                
                lnRow = trans.getDeductionCount();
                trans.setDeductionInfo(lnRow, "sRemarksx", "TEST");
                trans.setDeductionInfo(lnRow, "nDedctAmt", 500.00);
                
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
                
                System.out.println("----------------------------------------");
            } else 
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test20Deduction(){
        try {
            if (trans.addDeduction("CHICKS")){
                int lnCtr;
                int lnRow = trans.getDeductionCount();
                
                trans.setDeductionInfo(lnRow, "nDedctAmt", 1500.00);
                
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
            } else 
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test21Deduction(){
        try {
            trans.setDeductionEmployeeAllocationInfo("nAllcAmtx", 2, "M00108011504", 750.00);
            assertEquals(750.00, trans.getDeductionEmployeeAllocationInfo("nAllcAmtx", 2, "M00108011504"));            
            
            trans.setDeductionEmployeeAllocationInfo("nAllcAmtx", 2, "M00103001137", 750.00);
            assertEquals(750.00, trans.getDeductionEmployeeAllocationInfo("nAllcAmtx", 2, "M00103001137"));            
            
            trans.setDeductionEmployeeAllocationInfo("nAllcPerc", 1, "M00110017110", 100.00);
            assertEquals(100.00, trans.getDeductionEmployeeAllocationInfo("nAllcPerc", 1, "M00110017110"));            
            
            System.out.println("----------------------------------------");
            int lnRow = trans.getDeductionCount();
            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
                System.out.print(lnCtr);
                System.out.print("\t");
                System.out.print(trans.getDeductionInfo(lnCtr, "xAllocPer") + "%");
                System.out.print("\t");
                System.out.print(trans.getDeductionInfo(lnCtr, "xAllocAmt"));
                System.out.print("\t");
                System.out.print(trans.getDeductionInfo(lnCtr, "sRemarksx"));
                System.out.print("\t");
                System.out.print(trans.getDeductionInfo(lnCtr, "nDedctAmt"));
                System.out.println("");
            }
            System.out.println("----------------------------------------");
            
            lnRow = trans.getItemCount();
                
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
            
            lnRow = trans.getItemCount();
            int lnDed = trans.getDeductionCount();
            System.out.println("----------------------------------------");
            for(int lnCtr2 = 1; lnCtr2 <= lnDed; lnCtr2++){
                for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++){
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
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void test22SaveTransation(){
        try {
            if (trans.SaveTransaction()){
                
            } else {
                fail(trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}