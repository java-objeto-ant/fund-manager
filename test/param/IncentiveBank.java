package param;

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
import org.rmj.fund.manager.parameters.IncentiveBankInfo;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IncentiveBank {
    static GRider instance = new GRider();
    static IncentiveBankInfo trans;
    
    public IncentiveBank() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
        if (!instance.logUser("gRider", "M001111122")){
            System.err.println(instance.getMessage() + instance.getErrMsg());
            System.exit(1);
        }
        
        trans = new IncentiveBankInfo(instance, instance.getBranchCode(), false);
        trans.setWithUI(false);
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
    public void test02SearchEmployee(){
        try {
            if (trans.searchEmployee("Cuison, Michael", false)){
                assertEquals("M00111005387", (String) trans.getMaster("sEmployID"));
                assertEquals("Cuison, Michael Torres", (String) trans.getMaster("xEmployNm"));
            } else
                fail(trans.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test 
    public void test03SearchBank(){
        try {
            if (trans.searchBank("Banco De", false)){
                assertEquals("00XX024", (String) trans.getMaster("sBankIDxx"));
                assertEquals("Banco De Oro", (String) trans.getMaster("xBankName"));
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
            trans.setMaster("sBankAcct", "01234567890");
            assertEquals("01234567890", (String) trans.getMaster("sBankAcct"));
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
            if (trans.SearchRecord("Cuison, M", false)){
                if (trans.UpdateRecord()){
                    trans.setMaster("sBankAcct", "1111111111");
                    assertEquals("1111111111", (String) trans.getMaster("sBankAcct"));
                    
                    if (trans.searchBank("Bank of the Philippine", false)){
                        assertEquals("00XX004", (String) trans.getMaster("sBankIDxx"));
                        assertEquals("Bank of the Philippine Island", (String) trans.getMaster("xBankName"));
                    } else
                        fail(trans.getMessage());
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
    public void test07SaveRecord(){
        try {
            if (!trans.SaveRecord())
                fail(trans.getMessage());
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }
}